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
            logger.log(Level.SEVERE, Objects.toString(val));
        }
    }

    @Override
    public void error(String msg, Throwable throwable) {
        logger.log(Level.SEVERE, msg, throwable);
    }

    @Override
    public void info(String val, Object... arg) {
        logger.log(logRecord(Level.INFO, val, arg));
    }

    @Override
    public void debug(Object val) {
        logger.log(logRecord(Level.FINEST, Objects.toString(val)));
    }

    private LogRecord logRecord(Level level, String msg, Object... arg) {
        var record = new LogRecord(level, msg);
        record.setLoggerName(name);
        record.setSourceClassName(null);
        record.setParameters(arg);

        return record;
    }
}
