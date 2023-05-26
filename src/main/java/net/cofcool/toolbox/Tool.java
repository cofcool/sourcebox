package net.cofcool.toolbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
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

        public Optional<String> getVal() {
            return Optional.ofNullable(val);
        }

        public boolean test(Predicate<String> predicate) {
            if (val != null) {
                return predicate.test(val);
            }

            return true;
        }

        public static Arg of(String key, String val) {
            return new Arg(key, val, null, false, null);
        }

    }

    class Args extends LinkedHashMap<String, Arg> {

        private final Map<String, Arg> aliases = new HashMap<>();
        private final Map<String, AliasInterceptor> aliasInterceptors = new HashMap<>();

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
                if (strings.length > 2) {
                    strings[1] = String.join("=", Arrays.copyOfRange(strings, 1, strings.length)).trim();
                }
                arg(strings[0], strings.length == 1 ? null :strings[1].trim());
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

        public Args alias(String alias, ToolName name, String arg) {
            return alias(alias, name, arg, null);
        }

        public Args alias(String alias, ToolName name, String arg, AliasInterceptor argInterceptor) {
            aliases.put(alias, new Arg(name.name(), arg, null, false, null));
            if (argInterceptor != null) {
                aliasInterceptors.put(alias, argInterceptor);
            }
            return this;
        }

        public Args copyAliasFrom(Args args) {
            aliases.putAll(args.aliases);
            aliasInterceptors.putAll(args.aliasInterceptors);
            var cmds = new HashMap<String, Arg>();
            for (Arg arg : values()) {
                if (aliases.containsKey(arg.key)) {
                    var alias = aliases.get(arg.key);
                    cmds.put("tool", Arg.of("tool", alias.key));
                    cmds.put(alias.val, Arg.of(alias.val, arg.val));

                    var consumer = aliasInterceptors.get(arg.key);
                    if (consumer != null) {
                        consumer.post(cmds, arg, alias);
                    }
                }
            }
            putAll(cmds);
            return this;
        }

        @Override
        public String toString() {
            return super.toString();
        }

        public String toHelpString() {
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
            var alias = aliases.entrySet().stream().map(e -> "    --" + e.getKey() + "    " + "--tool=" + e.getValue().key + " --" + e.getValue().val).collect(Collectors.joining("\n"));
            return "Synopsis\n    "
                + synopsis
                + "\nDescription\n"
                + description
                + "\nAlias\n"
                + alias;
        }
    }

    @FunctionalInterface
    interface AliasInterceptor {

        /**
         * invoke when inject alias arguments
         * @param before current argument holder
         * @param arg current argument
         * @param alias alias argument
         */
        void post(Map<String, Arg> before, Arg arg, Arg alias);
    }
}
