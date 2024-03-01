package net.cofcool.sourcebox.internal.commandhelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.util.BaseFileCrudRepository;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FileUtils;

@CustomLog
public class CommandManager {

    private static final List<Command> INIT_CMDS;

    static {
        List<String> tags = List.of("#mytool");
        INIT_CMDS = List.of(
            new Command("mytool --tool=cHelper", "@helper", tags),
            new Command("mytool --tool=converts --cmd=now", "@mnow", tags),
            new Command("mytool --tool=converts --cmd=md5", "@mmd5", tags),
            new Command("mytool --tool=converts --cmd=hdate", "@mhdate", tags),
            new Command("mytool --tool=converts --cmd=timesp", "@mtimesp", tags),
            new Command("mytool --tool=converts --cmd=lower", "@mlower", tags),
            new Command("mytool --tool=converts --cmd=upper", "@mupper", tags)
        );
    }

    private final CommandRepository repository;
    private final String aliasPath;

    public CommandManager(String path, String aliasPath) {
        repository = new CommandRepository(path);
        this.aliasPath = aliasPath;
    }

    public void save(String command) {
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

        repository.save(new Command(String.join(" ", Arrays.copyOfRange(split, sIdx, eIdx)), alias, tags));
    }

    public List<Command> findByTag(String tag) {
        return repository.find(new Command(null, null, List.of(tag)));
    }

    public List<Command> findByAlias(String alias) {
        return repository.find(new Command(null, alias, null));
    }

    public List<Command> findByAT(String aliasTags) {
        if (aliasTags == null || "ALL".equals(aliasTags)) {
            return repository.find();
        }

        String alias = null;
        List<String> tags = new ArrayList<>();
        for (String s : aliasTags.split(" ")) {
            if (s.startsWith("@")) {
                alias = s;
            } else if (s.startsWith("#")) {
                tags.add(s);
            } else {
                alias = "@" + s;
            }
        }
        return repository.find(new Command(null, alias, tags));
    }

    public void delete(String alias) {
        repository.delete(alias);
    }

    public void store(String alias) {
        var all = findByAT(alias)
            .stream()
            .filter(Command::hasAlias)
            .toList();
        if (all.isEmpty()) {
            log.debug("No alias to store");
            return;
        }
        try {
            File file = new File(aliasPath);
            var newAlias = new ArrayList<>(all);
            if (file.exists()) {
                var old = FileUtils.readLines(file, StandardCharsets.UTF_8)
                    .stream()
                    .map(a -> {
                        var index = a.indexOf("=");
                        return new Command(a.substring(index + 2, a.length() - 1), "@" + a.substring(6, index), Collections.emptyList());
                    })
                    .filter(a -> all.stream().noneMatch(c -> c.id().equals(a.id())))
                    .toList();
                newAlias.addAll(old);
            }
            FileUtils.write(
                file,
                newAlias.stream().map(Command::toAlias).collect(Collectors.joining("\n")),
                StandardCharsets.UTF_8
            );
            Runtime.getRuntime().exec(new String[] {"sh", "source", file.getAbsolutePath()});
            log.info("Update {0} alias to {1}", alias, file.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Update " + aliasPath + " file error", e);
        }
    }

    static class CommandRepository extends BaseFileCrudRepository<Command> {

        public CommandRepository(String path) {
            super(path);
        }

        @Override
        protected String initContent() {
            save(INIT_CMDS);
            return null;
        }

        @Override
        protected void loadData(byte[] data) {
            var commands = JsonUtil.toPojoList(data, Command.class);
            dataCache.putAll(commands.stream().collect(Collectors.toMap(Command::id, a -> a)));
        }

        @Override
        protected Command saveData(Command entity) {
            dataCache.put(entity.id(), entity);
            return entity;
        }

        @Override
        public List<Command> find(Command condition) {
            Predicate<Command> predicate = a -> true;
            if (condition.alias() != null) {
                predicate = predicate.and(a -> condition.alias().equals(a.alias()));
            }
            if (condition.tags() != null && !condition.tags().isEmpty()) {
                predicate = predicate.and(a -> condition.tags().stream().anyMatch(a::tagContains));
            }

            return find().stream().filter(predicate).toList();
        }
    }

}

