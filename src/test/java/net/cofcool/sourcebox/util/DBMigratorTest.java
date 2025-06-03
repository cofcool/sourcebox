package net.cofcool.sourcebox.util;

import io.vertx.core.Vertx;
import net.cofcool.sourcebox.BaseTest;
import org.junit.jupiter.api.Test;

class DBMigratorTest extends BaseTest {


    @Test
    void migrator() throws Exception {
        var v = Vertx.vertx();
        SqlRepository.init(v);
        v.close();
    }
}