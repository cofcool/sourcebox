package net.cofcool.sourcebox.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoggerFactoryTest {

    Logger logger;

    @BeforeEach
    void setup() {
        System.setProperty("logging.debug", "true");
        logger = LoggerFactory.getLogger(getClass());
    }

    @Test
    void logger() {
        Assertions.assertNotNull(logger);
    }

}