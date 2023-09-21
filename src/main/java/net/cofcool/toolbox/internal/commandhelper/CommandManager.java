package net.cofcool.toolbox.internal.commandhelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.toolbox.util.BaseFileCrudRepository;
import net.cofcool.toolbox.util.JsonUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

@CustomLog
public class CommandManager {

    public static final String MY_TOOL_ALIAS = FilenameUtils.concat(System.getProperty("user.home"),  ".mytool_alias");
    private final CommandRepository repository;

    public CommandManager(String path) {
        repository = new CommandRepository(path);
    }

    public void save(String command) {
        String alias = null;
        var tags = new ArrayList<String>();

        var split = command.split(" ");
        int sIdx = 0;
        int eIdx = - 1;
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
            eIdx = split.length - 1;
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
            .map(a ->
                "alias "
                    + a.alias().substring(1)
                    + "='"
                    + a.cmd()
                    + "'"
            )
            .collect(Collectors.joining("\n"));
        try {
            File file = new File(MY_TOOL_ALIAS);
            FileUtils.write(file, all, StandardCharsets.UTF_8);
            Runtime.getRuntime().exec(new String[] {"sh", "source", file.getAbsolutePath()});
            log.info("Update {0} alias to {1}", alias, file.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Update " + MY_TOOL_ALIAS + " file error", e);
        }
    }

    static class CommandRepository extends BaseFileCrudRepository<Command> {

        public CommandRepository(String path) {
            super(path);
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
            if (condition.alias() != null) {
                return find().stream().filter(a -> condition.alias().equals(a.alias())).toList();
            }
            if (condition.tags() != null && !condition.tags().isEmpty()) {
                return find().stream().filter(a -> condition.tags().stream().anyMatch(a::tagContains)).toList();
            }
            return Collections.emptyList();
        }
    }

}

