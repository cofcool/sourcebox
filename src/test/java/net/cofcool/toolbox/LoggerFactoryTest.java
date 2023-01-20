package net.cofcool.toolbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoggerFactoryTest {

    Logger logger;

    @BeforeEach
    void setup() {
        LoggerFactory.setDebug(true);
        logger = LoggerFactory.getLogger(getClass());
    }

    @Test
    void info() {
        logger.info("test");
    }

    @Test
    void error() {
        logger.error("test");
        logger.error(new NullPointerException());
    }
}