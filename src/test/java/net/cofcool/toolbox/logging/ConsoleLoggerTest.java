package net.cofcool.toolbox.logging;

import org.junit.jupiter.api.Test;

class ConsoleLoggerTest {

    Logger logger = new ConsoleLogger(getClass());

    @Test
    void info() {
        logger.info("info");
    }

    @Test
    void error() {
        logger.error("error");
        logger.error(new NullPointerException());
    }

    @Test
    void debug() {
        logger.debug("debug");
    }

}