package net.cofcool.toolbox.logging;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class JULLogger implements Logger {

    private final java.util.logging.Logger logger;
    private final String name;

    public JULLogger(Class<?> clazz) {
        name = clazz.getName();
        logger = java.util.logging.Logger.getLogger(name);
        if (LoggerFactory.DEBUG) {
            logger.setLevel(Level.ALL);
        }
    }

    @Override
    public void error(Object val) {
        if (val instanceof Throwable ex) {
            error("ERROR", ex);
        } else {
            logRecord(Level.SEVERE, Objects.toString(val), null);
        }
    }

    @Override
    public void error(String msg, Throwable throwable) {
        logRecord(Level.SEVERE, msg, throwable);
    }

    @Override
    public void info(String val, Object... arg) {
        logRecord(Level.INFO, val, null, arg);
    }

    @Override
    public void debug(String val, Object... args) {
        if (LoggerFactory.DEBUG) {
            logRecord(Level.INFO, val, null, args);
        }
    }

    private void logRecord(Level level, String msg, Throwable throwable, Object... arg) {
        var record = new LogRecord(level, msg);
        record.setLoggerName(name);
        record.setSourceClassName(null);
        record.setParameters(arg);
        if (throwable != null) {
            record.setThrown(throwable);
        }

        logger.log(record);
    }
}
