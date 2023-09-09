package net.cofcool.toolbox;

import io.vertx.core.shareddata.Shareable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.cofcool.toolbox.logging.Logger;
import net.cofcool.toolbox.logging.LoggerFactory;

public interface Tool {

    ToolName name();

    void run(Args args) throws Exception;

    Args config();

    default Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    record Arg(String key, String val, String desc, boolean required, String demo) implements
        Shareable {

        public boolean isPresent() {
            return val != null;
        }

        public void ifPresent(Consumer<Arg> consumer) {
            if (val != null) {
                consumer.accept(this);
            }
        }

        public String getRequiredVal(String message) {
            if (val == null) {
                throw new IllegalArgumentException(message);
            }

            return val;
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

    class Args extends LinkedHashMap<String, Arg> implements Shareable {

        private final Map<String, Arg> aliases = new HashMap<>();

        private Set<RunnerType> runnerTypes = EnumSet.of(RunnerType.CLI);

        private final Map<String, AliasInterceptor> aliasInterceptors = new HashMap<>();

        private ToolContext context;

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

        public Args context(ToolContext context) {
            Objects.requireNonNull(context, "context can not be null");
            this.context = context;
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

        public ToolContext getContext() {
            return context;
        }

        public Optional<String> getArgVal(String key) {
            var arg = get(key);
            if (arg == null) {
                return Optional.empty();
            }
            return Optional.of(arg.val);
        }

        public Args alias(String alias, ToolName name, String arg, String desc) {
            return alias(alias, name, arg, desc, null);
        }

        public Args alias(String alias, ToolName name, String arg, String desc, AliasInterceptor argInterceptor) {
            aliases.put(alias, new Arg(name.name(), arg, desc, false, null));
            if (argInterceptor != null) {
                aliasInterceptors.put(alias, argInterceptor);
            }
            return this;
        }

        public Args runnerTypes(Set<RunnerType> runnerTypes) {
            this.runnerTypes = runnerTypes;
            return this;
        }

        public boolean supportsType(RunnerType type) {
            return runnerTypes.contains(type);
        }

        public boolean isCurrentType(RunnerType type) {
            return context.runnerType() == type;
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

        public Args removePrefix(String prefix) {
            var newRags =  new Args();
            forEach((k, v) -> {
                if (k.startsWith(prefix)) {
                    newRags.arg(new Arg(k.substring(prefix.length() + 1), v.val, v.desc, v.required, v.demo));
                } else {
                    newRags.arg(v);
                }
            });

            return newRags;
        }

        /**
         * only copy that does not exist
         */
        public Args copyConfigFrom(Args config) throws IllegalArgumentException {
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
            var alias = aliases.entrySet().stream().map(e -> "    --" + e.getKey() + "    " + "--tool=" + e.getValue().key + " --" + (e.getValue().desc == null ? e.getValue().val : e.getValue().desc)).collect(Collectors.joining("\n"));
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

    enum RunnerType {
        WEB, CLI, GUI
    }
}
