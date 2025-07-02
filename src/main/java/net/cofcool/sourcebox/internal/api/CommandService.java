package net.cofcool.sourcebox.internal.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.core.Future;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.QueryBuilder;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

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

        return find(builder, aliasTags);
    }

    private Future<List<CommandRecord>> find(QueryBuilder builder, String aliasTags) {
        if (aliasTags != null && !"ALL".equals(aliasTags)) {
            for (String s : aliasTags.split(" ")) {
                if (s.startsWith("@")) {
                    builder.and("alias=?", s);
                } else if (s.startsWith("#")) {
                    builder.and("? in (unnest(tags))", s);
                } else {
                    builder.and("id=?", Utils.md5(s));;
                }
            }
        }

        return repository.find(builder);
    }

    public Future<Boolean> delete(String alias) {
        return repository.delete(alias);
    }

    public Future<Boolean> store(String alias) {
        QueryBuilder builder = QueryBuilder.builder().select().from(CommandRecord.class)
            .limit(200);
        if (alias == null || "ALL".equals(alias)) {
            builder.and("alias is not null");
        }
        return find(builder, alias)
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

    public Future<?> importHis(ImportParam param) {
        return Future.join(
                new HistoryReader(param)
                    .readCommands()
                    .stream()
                    .filter(s -> !s.isBlank())
                    .map(s -> {
                        if (s.startsWith("{") && s.endsWith("}")) {
                            return JsonUtil.toPojo(s, CommandRecord.class);
                        } else {
                            return CommandRecord.builder()
                                .cmd(s).frequency(0).tags(List.of("#his", param.tag))
                                .remark("his").build();
                        }
                    })
                    .map(c ->
                        repository.find(c.id()).compose(
                            i -> Future.succeededFuture(),
                            i ->
                                save(c).onFailure(e -> log.error("Save error when import his", e))
                        )
                    )
                    .toList()
            )
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

    public Future<List<String>> exportHis() {
        return repository
            .find()
            .compose(a ->
                Future.succeededFuture(
                    a.stream().map(JsonUtil::toJson).toList()
                )
            );
    }

    public record Command(
        String id,
        String cmd,
        String alias,
        List<String> tags,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime cTime
    ) {}

    private record HistoryReader(ImportParam param) {

        public List<String> readCommands() {
            Set<String> uniqueCommands = new LinkedHashSet<>();
            for (String line : StringUtils.split(param.data, "\n")) {
                var a = switch (param.shell) {
                    case "zsh" -> {
                        int semicolonIndex = line.indexOf(';');
                        if (semicolonIndex != -1 && semicolonIndex + 1 < line.length()) {
                            yield line.substring(semicolonIndex + 1);
                        } else {
                            yield line;
                        }
                    }
                    default -> line;
                };
                uniqueCommands.add(a.trim());
            }
            log.info("Start save {0} history records", uniqueCommands.size());
            return new ArrayList<>(uniqueCommands);
        }
    }

    public record ImportParam(
        String shell,
        String data,
        String tag
    ){}
}

