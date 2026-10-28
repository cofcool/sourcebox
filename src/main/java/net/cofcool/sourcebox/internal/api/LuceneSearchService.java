package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.CustomLog;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.internal.api.entity.Comment;
import net.cofcool.sourcebox.internal.api.entity.Note;
import net.cofcool.sourcebox.util.RepositoryUtil;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@CustomLog
public class LuceneSearchService {

    private static final String CONTENT = "content";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String KEY = "key";
    private static final String TITLE = "title";
    private static final int MAX_RESULTS = 100;
    private static final Map<Vertx, LuceneSearchService> INSTANCES = new ConcurrentHashMap<>();

    private final Vertx vertx;
    private final SqlRepository<ActionRecord> actionRepository = RepositoryUtil.getRepository(ActionRecord.class);
    private final SqlRepository<Comment> commentRepository = RepositoryUtil.getRepository(Comment.class);
    private final SqlRepository<CommandRecord> commandRepository = RepositoryUtil.getRepository(CommandRecord.class);
    private final SqlRepository<Note> noteRepository = RepositoryUtil.getRepository(Note.class);
    private final Future<Void> ready;

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter writer;

    private LuceneSearchService(Vertx vertx) {
        this.vertx = vertx;
        ready = vertx.executeBlocking(() -> {
            try {
                Path indexPath = Path.of(App.globalCfgDir("search-index"));
                directory = FSDirectory.open(indexPath);
                boolean initializeIndex = !DirectoryReader.indexExists(directory);
                analyzer = new SmartChineseAnalyzer();
                writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
                return initializeIndex;
            } catch (IOException e) {
                throw new IllegalStateException("Open Lucene index failed", e);
            }
        }).compose(initializeIndex -> {
            if (!initializeIndex) {
                return Future.succeededFuture();
            }
            return Future.all(
                actionRepository.find(),
                commentRepository.find(),
                commandRepository.find(),
                noteRepository.find()
            ).compose(results -> {
                List<ActionRecord> actions = results.resultAt(0);
                List<Comment> comments = results.resultAt(1);
                List<CommandRecord> commands = results.resultAt(2);
                List<Note> notes = results.resultAt(3);
                Map<String, List<Comment>> commentsByAction = comments.stream()
                    .collect(Collectors.groupingBy(Comment::actionId));
                return this.<Void>runBlocking(() -> {
                    for (ActionRecord action : actions) {
                        writer.addDocument(actionDocument(action, commentsByAction.getOrDefault(action.id(), List.of())));
                    }
                    for (CommandRecord command : commands) {
                        writer.addDocument(commandDocument(command));
                    }
                    for (Note note : notes) {
                        writer.addDocument(noteDocument(note));
                    }
                    writer.commit();
                    return null;
                });
            });
        }).onFailure(error -> log.error("Initialize Lucene search index failed", error));
    }

    public static LuceneSearchService get(Vertx vertx) {
        return INSTANCES.computeIfAbsent(vertx, LuceneSearchService::new);
    }

    public Future<List<SearchResult>> search(String query, String type) {
        if (StringUtils.isBlank(query)) {
            return Future.succeededFuture(List.of());
        }
        return ready.compose(ignored -> this.<List<SearchResult>>runBlocking(() -> {
            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Query parsed = new QueryParser(CONTENT, analyzer).parse(query);
                Query searchQuery = StringUtils.isBlank(type)
                    ? parsed
                    : new org.apache.lucene.search.BooleanQuery.Builder()
                        .add(parsed, org.apache.lucene.search.BooleanClause.Occur.MUST)
                        .add(new org.apache.lucene.search.TermQuery(new Term(TYPE, type)),
                            org.apache.lucene.search.BooleanClause.Occur.FILTER)
                        .build();
                TopDocs matches = searcher.search(searchQuery, MAX_RESULTS);
                List<SearchResult> results = new ArrayList<>(matches.scoreDocs.length);
                for (ScoreDoc match : matches.scoreDocs) {
                    Document document = searcher.storedFields().document(match.doc);
                    results.add(new SearchResult(
                        document.get(TYPE),
                        document.get(ID),
                        document.get(TITLE),
                        match.score,
                        null
                    ));
                }
                return results;
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid search query", e);
            }
        })).compose(results -> {
            List<? extends Future<?>> lookups = results.stream().map(this::findResult).toList();
            return Future.all(lookups).map(composite -> {
                List<SearchResult> hydratedResults = new ArrayList<>(results.size());
                for (int i = 0; i < results.size(); i++) {
                    SearchResult result = results.get(i);
                    hydratedResults.add(result.withData(composite.resultAt(i)));
                }
                return hydratedResults;
            });
        });
    }

    private Future<?> findResult(SearchResult result) {
        return switch (result.type()) {
            case "action" -> actionRepository.find(result.id());
            case "command" -> commandRepository.find(result.id());
            case "note" -> noteRepository.find(result.id());
            default -> Future.failedFuture("Unknown search result type: " + result.type());
        };
    }

    public Future<Void> indexAction(ActionRecord action, List<Comment> comments) {
        return update(actionDocument(action, comments));
    }

    public Future<Void> indexCommand(CommandRecord command) {
        return update(commandDocument(command));
    }

    public Future<Void> indexNote(Note note) {
        return update(noteDocument(note));
    }

    public Future<Void> delete(String type, String id) {
        return ready.compose(ignored -> this.<Void>runBlocking(() -> {
            writer.deleteDocuments(new Term(KEY, key(type, id)));
            writer.commit();
            return null;
        }));
    }

    public static Future<Void> close(Vertx vertx) {
        LuceneSearchService service = INSTANCES.remove(vertx);
        if (service == null) {
            return Future.succeededFuture();
        }
        return service.ready.compose(ignored -> service.<Void>runBlocking(() -> {
                try {
                    service.closeResources();
                } catch (IOException e) {
                    throw new IllegalStateException("Close Lucene search index failed", e);
                }
                return null;
            })).onFailure(error -> log.error("Close Lucene search index failed", error));
    }

    private Future<Void> update(Document document) {
        return ready.compose(ignored -> this.<Void>runBlocking(() -> {
            writer.updateDocument(new Term(KEY, document.get(KEY)), document);
            writer.commit();
            return null;
        }));
    }

    private <T> Future<T> runBlocking(java.util.concurrent.Callable<T> action) {
        return vertx.executeBlocking(action);
    }

    private static Document actionDocument(ActionRecord action, List<Comment> comments) {
        String commentText = comments.stream().map(Comment::content).filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "));
        String searchable = join(action.name(), action.device(), action.type(), action.category(),
            commentText, action.labels(), action.remark(), action.state());
        return document("action", action.id(), action.name(), searchable);
    }

    private static Document commandDocument(CommandRecord command) {
        String title = StringUtils.defaultIfBlank(command.alias(), command.cmd());
        return document("command", command.id(), title,
            join(command.tags() == null ? null : String.join(" ", command.tags()),
                command.alias(), command.cmd(), command.remark()));
    }

    private static Document noteDocument(Note note) {
        return document("note", note.id(), note.content(), note.content());
    }

    private static Document document(String type, String id, String title, String content) {
        Document document = new Document();
        document.add(new StringField(TYPE, type, Field.Store.YES));
        document.add(new StringField(ID, id, Field.Store.YES));
        document.add(new StringField(KEY, key(type, id), Field.Store.YES));
        document.add(new StoredField(TITLE, StringUtils.abbreviate(StringUtils.defaultString(title), 200)));
        if (StringUtils.isNotBlank(content)) {
            document.add(new TextField(CONTENT, content, Field.Store.NO));
        }
        return document;
    }

    private static String key(String type, String id) {
        return type + ":" + id;
    }

    private static String join(String... values) {
        return java.util.Arrays.stream(values)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "));
    }

    private void closeResources() throws IOException {
        if (writer != null) {
            writer.close();
        }
        if (analyzer != null) {
            analyzer.close();
        }
        if (directory != null) {
            directory.close();
        }
    }

    public record SearchResult(String type, String id, String title, float score, Object data) {
        private SearchResult withData(Object data) {
            return new SearchResult(type, id, title, score, data);
        }
    }
}
