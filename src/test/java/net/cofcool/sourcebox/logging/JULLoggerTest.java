package net.cofcool.sourcebox.logging;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JULLoggerTest {

    Logger logger;

    @BeforeEach
    void setup() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        System.setProperty("logging.debug", "true");
        System.setProperty("logging.type", "net.cofcool.toolbox.logging.JULLogger");
        logger = JULLogger.class.getConstructor(Class.class).newInstance(getClass());
    }

    @Test
    void info() {
        logger.info("info");
        logger.info("info {0}", "test");
    }

    @Test
    void error() {
        logger.error("error", new NullPointerException());
    }

    @Test
    void debug() {
        logger.debug("debug {0}", "debug1");
    }
}