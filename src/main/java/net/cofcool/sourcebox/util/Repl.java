package net.cofcool.sourcebox.util;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public final class Repl {

    public interface Command {
        String name();
        void prompt(PromptStore store);
        void execute(Inputs inputs, Writer writer);
    }

    public static class Inputs {

        private final Map<String, Object> i = new HashMap<>();

        public Inputs add(String key, Object val) {
            i.put(key, val);
            return this;
        }

        public String getString() {
            return i.values().iterator().next().toString();
        }

        public int getInt(String key) {
            return Integer.parseInt(getString(key));
        }

        public String getString(String key) {
            Object v = i.getOrDefault(key, "");

            if (v instanceof String s) {
                return s;
            }

            throw new IllegalStateException("the type is not a string: " + v);
        }

        public Map<String, Object> all() {
            return Map.copyOf(i);
        }

        @SuppressWarnings("unchecked")
        public <T> T toObj(Class<?> clazz) {
            return (T) JsonUtil.getObjectMapper().convertValue(i, clazz);
        }
    }

    public static class PromptStore {

        private PromptStore() {}

        public static PromptStore of() {
            return new PromptStore();
        }

        private final Map<String, String> records = new LinkedHashMap<>();

        public PromptStore record(String key, String prompt) {
            records.put(key, StringUtils.isNotBlank(prompt) ? prompt : key);
            return this;
        }

        public Set<String> keys() {
            return records.keySet();
        }

        public String prompt(String key) {
            return records.get(key);
        }

        public Set<Entry<String, String>> values() {
            return records.entrySet();
        }

        public List<String> prompts() {
            return List.of(records.values().toArray(String[]::new));
        }
    }

    public static class CommandRegistry {
        private final Map<String, Command> commands = new HashMap<>();

        public void register(Command command) {
            commands.put(command.name(), command);
        }

        public Command get(String name) {
            return commands.get(name);
        }

        public Command getFirst() {
            return commands.values().iterator().next();
        }

        public boolean has(String name) {
            return commands.containsKey(name);
        }

        public int size() {
            return commands.size();
        }

        public String help() {
            return "commands: " + String.join(", ", commands.keySet());
        }
    }

    private final LineReader reader;
    private final PrintWriter writer;
    private final boolean loop;

    public Repl(LineReader reader, PrintWriter writer, boolean loop) {
        this.reader = reader;
        this.writer = writer;
        this.loop = loop;
    }

    @SneakyThrows
    public Repl() {
        Terminal terminal = TerminalBuilder.builder()
            .system(true)
            .build();

        reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .parser(new DefaultParser())
            .build();
        writer = terminal.writer();
        loop = true;
    }

    public void launch(CommandRegistry registry) {
        boolean flag = true;
        while (flag) {
            Command cmd;
            String line = null;
            if (registry.size() == 1) {
                cmd = registry.getFirst();
            } else {
                line = reader.readLine(">>> ");
                if (line == null || line.trim().isEmpty()) continue;
                line = line.trim();

                if ("exit".equalsIgnoreCase(line)) return;

                cmd = registry.get(line);
            }


            if (cmd != null) {
                PromptStore store = PromptStore.of();
                cmd.prompt(store);
                Inputs inputs = new Inputs();
                for (Entry<String, String> e: store.values()) {
                    String val = reader.readLine(e.getValue() + ": ");
                    if (StringUtils.isNotBlank(val)) {
                        inputs.add(e.getKey(), val);
                    }
                }
                try {
                    cmd.execute(inputs, writer);
                } catch (Exception e) {
                    if (loop) {
                        writer.write("execute command " + line + " error: " + e.getMessage() + "\n");
                    } else {
                        throw new IllegalStateException(e);
                    }
                }
            } else {
                writer.write("unknown command: " + line + "\n");
                writer.write("help: " + registry.help() + "\n");
            }
            flag = loop;
        }
    }

}
