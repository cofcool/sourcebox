package net.cofcool.toolbox.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JULLoggerTest {

    Logger logger;

    @BeforeEach
    void setup() {
        System.setProperty("logging.debug", "true");
        logger = new JULLogger(getClass());
    }

    @Test
    void info() {
        logger.info("info");
    }

    @Test
    void error() {
        logger.error("error", new NullPointerException());
    }

    @Test
    void debug() {
        logger.debug("debug");
    }
}