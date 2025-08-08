package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.Comment;
import net.cofcool.sourcebox.internal.api.entity.Note;
import net.cofcool.sourcebox.internal.api.entity.RefType;
import net.cofcool.sourcebox.util.QueryBuilder;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

@CustomLog
public class ActionService {

    private final SqlRepository<ActionRecord> actionRecordSqlRepository;
    private final SqlRepository<Comment> commentSqlRepository;
    private final SqlRepository<ActionType> actionTypeSqlRepository;

    private final List<ActionInterceptor> saveInterceptors = List.of(new LinkInterceptor(), new RecordInterceptor());

    private final NoteService noteService;

    public ActionService(Vertx vertx, NoteService noteService) {
        this.actionRecordSqlRepository = SqlRepository.create(ActionRecord.class);
        this.commentSqlRepository = SqlRepository.create(Comment.class);
        this.actionTypeSqlRepository = SqlRepository.create(ActionType.class);
        this.noteService = noteService;
    }

    public Future<Void> saveAll(List<ActionRecord> records) {
        return Future.all(records.stream().map(this::saveAction).toList()).compose(a -> {
            if (a.failed()) {
                return Future.failedFuture(a.cause());
            } else {
                return Future.succeededFuture();
            }
        });
    }

    public Future<ActionRecord> saveAction(ActionRecord record) {
        for (ActionInterceptor interceptor : saveInterceptors) {
            record = interceptor.apply(record);
        }
        var ret = actionRecordSqlRepository.save(record);
        ret.onSuccess(a -> {
            if (a.comments() != null) {
                var comments = a.comments().stream().map(c ->
                    new Comment(Utils.generateShortUUID(), a.id(), c, LocalDateTime.now(), LocalDateTime.now())
                ).toList();
                commentSqlRepository.save(comments).onFailure(new Log("Save comment error"));
            }
            if (a.category() != null) {
                saveType(a.category(), Type.category, "Save actionType error");
            }
            if (a.device() != null) {
                saveType(a.device(), Type.device, "Save device error");
            }
            if (a.labels() != null) {
                for (String s : a.labels().split(",")) {
                    saveType(s, Type.label, "Save label error");
                }
            }
        });

        return ret;
    }

    private void saveType(String a, Type label, String error) {
        ActionType actionType = new ActionType(a, label);
        actionTypeSqlRepository
            .find(actionType.name())
            .onFailure(t ->
                actionTypeSqlRepository
                    .save(actionType)
                    .onFailure(new Log(error + " with " + a))
            );
    }

    public Future<List<Comment>> findComment(String actionId) {
        return commentSqlRepository.find(new Comment(actionId));
    }

    public Future<ActionRecord> example() {
        return Future.succeededFuture(new ActionRecord(
            "example",
            null,
            null,
            "Browser",
            Type.record.name(),
            "video",
            "init",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            null,
            5,
            List.of("start watch"),
            "example",
            String.join(":", RefType.action.name(), "id"),
            LocalDateTime.now()
        ));
    }

    public Future<ActionRecord.Records> findAllRefs(String id) {
        return Future.all(
                actionRecordSqlRepository.find(id),
                actionRecordSqlRepository.find(new ActionRecord(RefType.action.refStr(id)))
            )
            .flatMap(r ->
                Future.succeededFuture(new ActionRecord.Records(r.resultAt(0), r.resultAt(1)))
            );
    }

    public Future<List<ActionRecord>> find(ActionRecord record) {
        var builder = QueryBuilder.builder().select().from(ActionRecord.class)
            .orderBy("create_time desc")
            .limit(50);
        if (StringUtils.isNotBlank(record.name())) {
            builder.and("name like '%" + record.name() + "%'");
        }
        if (StringUtils.isNotBlank(record.id()) && record.checkId()) {
            builder.and("id=?", record.id());
        }
        if (StringUtils.isNotBlank(record.type())) {
            builder.and("type=?", record.type());
        }
        if (StringUtils.isNotBlank(record.state())) {
            builder.and("state=?", record.state());
        }
        if (StringUtils.isNotBlank(record.category())) {
            builder.and("category=?", record.category());
        }
        return actionRecordSqlRepository.find(builder);
    }

    public Future<ActionRecord.RecordRet> find(String id) {
        return actionRecordSqlRepository.find(id)
            .compose(a ->
                findComment(a.id())
                    .compose(c -> Future.succeededFuture(new ActionRecord.RecordRet(a, c))))
            .compose(a -> {
                var ref = RefType.parse(a.record().refs());
                var refObj = switch (ref.getKey()) {
                    case action -> find(ref.getValue());
                    case note -> noteService.find(new Note(ref.getValue()));
                    default -> Future.succeededFuture();
                };
                return refObj.compose(f -> Future.succeededFuture(
                    new ActionRecord.RecordRet(a.record(), a.comments(), f)
                ));
            });
    }

    public Future<Map<String, Object>> findAllType() {
        return actionTypeSqlRepository.find()
            .flatMap(a -> Future.succeededFuture(
                Map.of("types", a, "states", Arrays.toString(ActionState.values()))
            ));
    }

    public Future<Boolean> deleteActions(String id) {
        return delete(id, actionRecordSqlRepository::delete).compose(a -> deleteCommentsByActionId(id));
    }

    public Future<Boolean> deleteComments(String id) {
        return delete(id, commentSqlRepository::delete);
    }

    public Future<Boolean> deleteCommentsByActionId(String actionId) {
        return commentSqlRepository
            .find(
                QueryBuilder.builder().from(Comment.class).select().and("action_id=?", actionId))
            .compose(
                a -> Future.all(a.stream().map(c -> commentSqlRepository.delete(c.id())).toList()))
            .compose(a -> Future.succeededFuture(true));
    }

    private Future<Boolean> delete(String id, Function<String, Future<Boolean>> deleteFunc) {
        return deleteFunc.apply(id).compose(a -> Future.succeededFuture(true));
    }

    private <T, R, A> Future<R> findAll(Function<ActionRecord.RecordRet, T> mapper, Collector<? super T, A, R> collector) {
        return Future.all(actionRecordSqlRepository.find(), commentSqlRepository.find())
            .compose(a -> {
                List<ActionRecord> r = a.result().resultAt(0);
                List<Comment> c = a.result().resultAt(1);
                var commentMap = c.stream().collect(Collectors.groupingBy(Comment::actionId));
                return Future.succeededFuture(r
                    .stream()
                    .map(record -> new ActionRecord.RecordRet(record, commentMap.getOrDefault(record.id(), List.of())))
                    .map(mapper)
                    .collect(collector)
                );
            });
    }

    public Future<String> export2Md() {
        return findAll(ActionRecord::toMarkdown, Collectors.joining("\n"));
    }

    public Future<List<ActionRecord>> find() {
        return findAll(a -> {
                ActionRecord record = a.record();
                return new ActionRecord(record.id(), record.name(), record.icon(),
                    record.index(), record.device(), record.type(), record.category(), record.state(), record.start(),
                    record.end(),
                    record.duration(), record.rating(),
                    a.comments().stream().map(Comment::content).toList(), record.labels(),
                    record.refs(), record.remark(), record.createTime(), LocalDateTime.now());
            },
            Collectors.toList()
        );
    }

    public Future<Comment> saveComment(String actionId, Comment pojo) {
        return commentSqlRepository.save(Comment.builder()
            .id(System.currentTimeMillis() + "")
            .actionId(actionId).content(pojo.content())
            .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
            .build()
        );
    }

    private static class LinkInterceptor implements ActionInterceptor {

        @Override
        public ActionRecord apply(ActionRecord record) {
            if (record.name() != null && record.name().toLowerCase().startsWith("http") && StringUtils.isBlank(record.remark())) {
                try {
                    var title = Jsoup.newSession().url(record.name()).get().title();
                    record = ActionRecord.builder()
                        .name(title)
                        .remark(record.name())
                        .type(record.type())
                        .state(record.state())
                        .start(record.start())
                        .end(record.end())
                        .build();
                } catch (IOException e) {
                    log.error("Get " + record.name() + " web page title error", e);
                }
            }
            return record;
        }
    }

    private static class RecordInterceptor implements ActionInterceptor {

        @Override
        public ActionRecord apply(ActionRecord record) {
            if (Objects.equals(record.type(), Type.record.name()) && Objects.equals(record.state(), ActionState.done.name())) {
                    record = ActionRecord.builder()
                        .id(record.id())
                        .type(record.type())
                        .state(record.state())
                        .end(LocalDateTime.now())
                        .build();
            }
            return record;
        }
    }

    private record Log(
        String msg
    ) implements Handler<Throwable> {

        @Override
        public void handle(Throwable event) {
            log.info(msg, event);
        }
    }

}
