package net.cofcool.sourcebox.internal.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.core.Future;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.internal.api.entity.Config;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.QueryBuilder;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.io.FileUtils;

@CustomLog
public class CommandService {

    private static final List<CommandRecord> INIT_CMDS;

    static {
        List<String> tags = List.of("#sourcebox");
        INIT_CMDS = List.of(
            CommandRecord.builder().cmd("sourcebox --tool=cHelper").alias("@helper").tags(tags).build(),
            CommandRecord.builder().cmd("sourcebox --tool=converts --cmd=now").alias("@mnow").tags(tags).build(),
            CommandRecord.builder().cmd("sourcebox --tool=converts --cmd=md5").alias("@mmd5").tags(tags).build(),
            CommandRecord.builder().cmd("sourcebox --tool=converts --cmd=hdate").alias("@mhdate").tags(tags).build(),
            CommandRecord.builder().cmd("sourcebox --tool=converts --cmd=timesp").alias("@mtimesp").tags(tags).build(),
            CommandRecord.builder().cmd("sourcebox --tool=converts --cmd=lower").alias("@mlower").tags(tags).build(),
            CommandRecord.builder().cmd("sourcebox --tool=converts --cmd=upper").alias("@mupper").tags(tags).build()
        );
    }

    private final SqlRepository<CommandRecord> repository;
    private final String aliasPath;

    public CommandService(String path, String aliasPath) {
        repository = SqlRepository.create(CommandRecord.class);
        loadOrInitData(path);
        this.aliasPath = aliasPath;
    }

    public Future<CommandRecord> save(CommandRecord command) {
        return repository.save(command);
    }

    public Future<CommandRecord> enter(String id) {
        return repository.find(id)
            .compose(c -> repository.save(c.incrementFrequency()));
    }

    public Future<CommandRecord> save(String command) {
        String alias = null;
        var tags = new ArrayList<String>();

        var split = command.split(" ");
        int sIdx = 0;
        int eIdx = -1;
        for (int i = 0; i < split.length; i++) {
            var s = split[i];
            if (s.startsWith("@")) {
                alias = s;
                sIdx = 1;
            }
            if (s.startsWith("#")) {
                if (eIdx < 0) {
                    eIdx = i;
                }
                tags.add(s);
            }
        }
        if (eIdx < 0) {
            eIdx = split.length;
        }

        return repository.save(
            CommandRecord.builder()
                .cmd(String.join(" ", Arrays.copyOfRange(split, sIdx, eIdx)))
                .frequency(0).alias(alias).tags(tags).build()
        );
    }

    public Future<List<CommandRecord>> findByCmd(String cmd) {
        return repository.find(
            QueryBuilder
                .builder()
                .from(CommandRecord.class)
                .select()
                .limit(6)
                .orderBy("frequency desc")
                .and("cmd like '%" + cmd + "%'")
        );
    }

    public Future<List<CommandRecord>> find(CommandRecord record) {
        return repository.find(record);
    }

    public Future<List<CommandRecord>> find(String aliasTags) {
        var builder = QueryBuilder.builder().select().from(CommandRecord.class)
            .orderBy("create_time desc").limit(20);

        if (aliasTags != null && !"ALL".equals(aliasTags)) {
            for (String s : aliasTags.split(" ")) {
                if (s.startsWith("@")) {
                    builder.and("alias=?", s);
                } else if (s.startsWith("#")) {
                    builder.and("? in (unnest(tags))", s);
                } else {
                    builder.and("alias=?", "@" + s);;
                }
            }
        }

        return repository.find(builder);
    }

    public Future<Boolean> delete(String alias) {
        return repository.delete(alias);
    }

    public Future<Boolean> store(String alias) {
        return find(alias)
            .compose(i -> Future.succeededFuture(
                i.stream()
                    .filter(CommandRecord::hasAlias)
                    .toList()
            ))
            .compose(i -> {
                try {
                    File file = new File(aliasPath);
                    var newAlias = new ArrayList<>(i);
                    if (file.exists()) {
                        var old = FileUtils.readLines(file, StandardCharsets.UTF_8)
                            .stream()
                            .map(a -> {
                                var index = a.indexOf("=");
                                return CommandRecord
                                    .builder()
                                    .cmd(a.substring(index + 2, a.length() - 1))
                                    .alias("@" + a.substring(6, index))
                                    .tags(Collections.emptyList()).build();
                            })
                            .filter(a -> i.stream().noneMatch(c -> c.id().equals(a.id())))
                            .toList();
                        newAlias.addAll(old);
                    }
                    FileUtils.write(
                        file,
                        newAlias.stream().map(CommandRecord::makeAlias)
                            .collect(Collectors.joining("\n")),
                        StandardCharsets.UTF_8
                    );
                    log.info("Update {0} alias to {1}", alias, file.getAbsolutePath());
                    return Future.succeededFuture(true);
                } catch (IOException e) {
                    throw new RuntimeException("Update " + aliasPath + " file error", e);
                }
            })
            .onFailure(e -> log.error("Store alias error", e));
    }

    public Future<?> importHis() {
        return HistoryProcessor
            .process()
            .map(h -> h.stream()
                .map(s -> CommandRecord.builder().cmd(s).frequency(0).tags(List.of("#his")).remark("his").build()).toList())
            .compose(h -> {
                log.info("Start save {0} history records", h.size());
                return Future.join(h
                    .stream()
                    .map(c ->
                        repository.find(c.id()).compose(
                            i -> Future.succeededFuture(),
                            i ->
                            save(c).onFailure(e -> log.error("Save error when import his", e))
                        )
                    )
                    .toList()
                );
            })
            .onFailure(e -> log.error("Import history error", e))
            .onSuccess(r -> log.info("Import history ok"));
    }

    private void loadOrInitData(String path) {
        try {
            var file = new File(path);
            if (file.exists()) {
                log.info("Import old commands");
                var data = JsonUtil.toPojoList(FileUtils.readFileToByteArray(file), Command.class)
                    .stream()
                    .map(i -> new CommandRecord(
                            i.id(), i.cmd(), i.alias(), i.tags(),
                            "", 0,
                            LocalDateTime.now(), LocalDateTime.now()
                        )
                    )
                    .toList();
                repository.save(data)
                    .onSuccess(i -> {
                        var np = path + ".bak";
                        var s = file.renameTo(new File(np));
                        if (!s) {
                            log.error("rename old command data " + np + " error");
                        }
                    })
                    .onFailure(e -> log.error("save old command data error", e));
            }
        } catch (IOException e) {
            log.error("load old command data error", e);
        }
        repository
            .count(QueryBuilder.builder().count().from(CommandRecord.class))
            .compose(i -> {
                if (i == 0) {
                    log.info("Init default commands");
                    return repository.save(INIT_CMDS);
                }
                return Future.succeededFuture();
            });
    }

    public record Command(
        String id,
        String cmd,
        String alias,
        List<String> tags,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime cTime
    ) {}

    interface HistoryFileReader {
        List<String> readCommands() throws IOException;
        long getModifiedTime();
        String getName();
    }

    static class HistoryProcessor {
        private static final String LASTMT = "lastmt";

        private static final List<HistoryFileReader> readers = List.of(
            new ZshHistoryFileReader(),
            new BashHistoryFileReader()
        );

        public static Future<Set<String>> process() {
            return Future.join(
                    readers.stream()
                        .map(reader ->
                            getLastModifiedTime(reader.getName())
                                .compose(i -> {
                                    var modifiedTime = reader.getModifiedTime();
                                    if (i < modifiedTime) {
                                        try {
                                            var commands = new ArrayList<>(reader.readCommands());
                                            return storeLastModifiedTime(
                                                reader.getName(),
                                                modifiedTime
                                            )
                                                .compose(c -> Future.succeededFuture(
                                                    new LastRecord(modifiedTime, commands))
                                                );
                                        } catch (IOException e) {
                                            throw new RuntimeException("read history error", e);
                                        }
                                    } else {
                                        return Future.succeededFuture(new LastRecord(modifiedTime, List.of()));
                                    }
                                }))
                        .toList()
                )
                .compose(i -> {
                    List<LastRecord> val = i.list();
                    return Future.succeededFuture(
                        new LinkedHashSet<>(
                            val.stream()
                                .map(v -> v.cmds)
                                .reduce(new ArrayList<>(), (v1, v2) -> {
                                    v1.addAll(v2);
                                    return v1;
                                })
                        )
                    );
                });
        }

        private static Future<Long> getLastModifiedTime(String sh) {
            return ConfigService
                .getService()
                .get(LASTMT + "." + sh, "0")
                .compose(i -> Future.succeededFuture(Long.valueOf(i)));
        }

        private static Future<Config> storeLastModifiedTime(String sh, long lastModifiedTime) {
            return ConfigService.getService().put(LASTMT+"." + sh, lastModifiedTime + "");
        }
    }

    record LastRecord(
        long lstm,
        List<String> cmds
    ){}

    static class ZshHistoryFileReader implements HistoryFileReader {
        private static final String FILE_PATH = System.getProperty("user.home") + "/.zsh_history";

        @Override
        public List<String> readCommands() throws IOException {
            Set<String> uniqueCommands = new LinkedHashSet<>();
            Path path = Paths.get(FILE_PATH);
            if (Files.exists(path)) {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.IGNORE)
                    .onUnmappableCharacter(CodingErrorAction.IGNORE);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path), decoder))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int semicolonIndex = line.indexOf(';');
                        if (semicolonIndex != -1 && semicolonIndex + 1 < line.length()) {
                            uniqueCommands.add(line.substring(semicolonIndex + 1));
                        }
                    }
                }
            }
            return new ArrayList<>(uniqueCommands);
        }

        @Override
        public long getModifiedTime() {
            try {
                Path path = Paths.get(FILE_PATH);
                return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : 0;
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public String getName() {
            return "zsh";
        }
    }

    static class BashHistoryFileReader implements HistoryFileReader {
        private static final String FILE_PATH = System.getProperty("user.home") + "/.bash_history";

        @Override
        public List<String> readCommands() throws IOException {
            Set<String> uniqueCommands = new HashSet<>();
            Path path = Paths.get(FILE_PATH);
            if (Files.exists(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                            uniqueCommands.add(line);
                    }
                }
            }
            return new ArrayList<>(uniqueCommands);
        }

        @Override
        public long getModifiedTime() {
            try {
                Path path = Paths.get(FILE_PATH);
                return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : 0;
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public String getName() {
            return "bash";
        }
    }
}

