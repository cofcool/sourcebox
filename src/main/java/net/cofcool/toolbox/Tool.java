package net.cofcool.toolbox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

public interface Tool {

    ToolName name();

    void run(Args args) throws Exception;

    String help();

    record Arg(String key, String val) {

    }

    class Args extends HashSet<Arg> {

        public Args(int initialCapacity) {
            super(initialCapacity);
        }

        public Args() {
            super(4);
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

        public Args arg(String key, String val) {
            add(new Arg(key, val));
            return this;
        }

        public Optional<Arg> readArg(String key) {
            for (Arg arg : this) {
                if (arg.key.equals(key)) {
                    return Optional.of(arg);
                }
            }
            return Optional.empty();
        }

        @Override
        public String[] toArray() {
            return Arrays.stream(super.toArray()).map(a -> ((Arg)a).key).toArray(String[]::new);
        }
    }
}
