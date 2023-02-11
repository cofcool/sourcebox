package net.cofcool.toolbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface Tool {

    ToolName name();

    void run(Args args) throws Exception;

    Args config();

    default Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    record Arg(String key, String val, String desc, boolean required, String demo) {

        public boolean isPresent() {
            return val != null;
        }

        public void ifPresent(Consumer<Arg> consumer) {
            if (val != null) {
                consumer.accept(this);
            }
        }

        public static Arg of(String key, String val) {
            return new Arg(key, val, null, false, null);
        }

    }

    class Args extends LinkedHashMap<String, Arg> {

        public Args(int initialCapacity) {
            super(initialCapacity);
        }

        public Args() {
            this(4);
        }

        public Args(String[] args) {
            this();
            for (String s : String.join(" ", args).split("--")) {
                if (s.isEmpty()) {
                    continue;
                }
                String[] strings = s.split("=");
                arg(strings[0], strings[1].trim());
            }
        }

        Args setupConfig(Args config) throws IllegalArgumentException {
            var error = new ArrayList<Arg>();
            for (Arg arg : config.values()) {
                if (get(arg.key()) == null) {
                    if (arg.required()) {
                        error.add(arg);
                    } else {
                        arg(arg);
                    }
                }
            }
            if (!error.isEmpty()) {
                throw new IllegalArgumentException(error.stream().map(a -> String.format("%s must be specified, like: %s=%s", a.key(), a.key(), a.demo())).collect(Collectors.joining("; ")));
            }

            return this;
        }

        public Args args(Collection<Arg> args) {
            for (Arg arg : args) {
                arg(arg);
            }
            return this;
        }

        public Args arg(Arg arg) {
            put(arg.key(), arg);
            return this;
        }

        public Args arg(String key, String val) {
            return arg(Arg.of(key, val));
        }

        public Arg readArg(String key) {
            var arg = get(key);
            if (arg == null) {
                throw new IllegalStateException("Do not support argument " + key + ", please see the help");
            }

            return arg;
        }

        public String toSimpleString() {
            return super.toString();
        }

        @Override
        public String toString() {
            var synopsis = values()
                .stream()
                .map(a ->
                    String.join(
                        "",
                        a.required() ? "" : "[",
                        "--", a.key(),
                        "=",
                        a.isPresent() ? a.val() : a.demo(),
                        a.required() ? "" : "]"
                    )
                )
                .collect(Collectors.joining(" "));
            var description = values()
                .stream()
                .map(a -> "    --" + a.key() + "    " + a.desc() + (a.isPresent() ? ". Default: " + a.val() : ". Example: " + a.demo()))
                .collect(Collectors.joining("\n"));
            return "Synopsis\n    "
                + synopsis
                + "\nDescription\n"
                + description;
        }
    }
}
