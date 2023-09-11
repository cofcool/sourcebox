package net.cofcool.toolbox.logging;

import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;

public final class LoggerFactory {

    private static final Map<Class<?>, Logger> loggers = new HashMap<>();

    static boolean DEBUG = false;
    static Class<? extends Logger> LOGGER_TYPE;

    public static final String LOGGING_TYPE_KEY = "logging.type";
    public static final String LOGGING_DEBUG_TYPE = "logging.debug";

    static {
        var property = System.getProperty(LOGGING_DEBUG_TYPE);
        if (property != null) {
            DEBUG = Boolean.parseBoolean(property);
        }
        var loggerType = System.getProperty(LOGGING_TYPE_KEY, ConsoleLogger.class.getName());
        try {
            //noinspection unchecked
            LOGGER_TYPE = (Class<Logger>) Class.forName(loggerType);
        } catch (ClassNotFoundException e) {
            LOGGER_TYPE = ConsoleLogger.class;
        }
    }

    private LoggerFactory() {
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static Logger getLogger(Class<?> clazz) {
        return loggers.computeIfAbsent(clazz, LoggerFactory::loggerInstance);
    }

    @SneakyThrows
    private static Logger loggerInstance(Class<?> clazz) {
        return LOGGER_TYPE.getConstructor(Class.class).newInstance(clazz);
    }
}
