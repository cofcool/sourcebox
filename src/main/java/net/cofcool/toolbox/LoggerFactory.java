package net.cofcool.toolbox;

import java.util.HashMap;
import java.util.Map;

public final class LoggerFactory {

    private static final Map<Class<?>, Logger> loggers = new HashMap<>();

    private static boolean debug;

    static {
        String property = System.getProperty("logging.debug");
        if (property != null) {
            debug = Boolean.parseBoolean(property);
        }
    }

    private LoggerFactory() {
    }

    static void setDebug(boolean debug) {
        LoggerFactory.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static Logger getLogger(Class<?> clazz) {
        return loggers.computeIfAbsent(clazz, c -> new LoggerImpl(c, debug));
    }

    private record LoggerImpl(Class<?> clazz, boolean debug) implements Logger {

        @Override
        public void error(Object val) {
            if (val instanceof Throwable) {
                if (debug) {
                    ((Throwable) val).printStackTrace();
                    return;
                } else {
                    val = ((Throwable) val).getMessage();
                }
            }
            System.err.println(val);
        }

        @Override
        public void info(Object val) {
            System.out.println(val);
        }

        @Override
        public void debug(Object val) {
            if (debug) {
                System.out.println(val);
            }
        }
    }
}
