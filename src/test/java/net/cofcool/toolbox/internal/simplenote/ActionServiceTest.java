package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.util.List;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.internal.simplenote.entity.ActionRecord;
import net.cofcool.toolbox.logging.LoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class ActionServiceTest extends BaseTest {

    static ActionService actionService;

    static ActionRecord defaultRecord;

    @BeforeAll
    static void setup(Vertx vertx, VertxTestContext testContext) {
        LoggerFactory.setDebug(true);
        actionService = new ActionService(vertx);
        actionService.saveAction(new ActionRecord(
                "test video",
                null,
                null,
                "mac",
                "video",
                "init",
                LocalDateTime.now(),
                null,
                null,
                5,
                List.of("first"),
                "test, demo",
                null,
                LocalDateTime.now()
            ))
            .onComplete(testContext.succeeding(r -> {
                defaultRecord = r;
                testContext.verify(testContext::completeNow);
            }));
    }

    @Test
    void find(Vertx vertx, VertxTestContext testContext) {
        actionService
            .find()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                System.out.println(r);
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void saveMultiProperties(Vertx vertx, VertxTestContext testContext) {
        ActionRecord newR = new ActionRecord(
            defaultRecord.id(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now().plusDays(5),
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now()
        );
        actionService
            .saveAction(newR)
            .onComplete(testContext.succeeding(r -> {
                actionService
                    .find(newR.id())
                    .onComplete(testContext.succeeding(r1 -> testContext.verify(() -> {
                        System.out.println(r1);
                        Assertions.assertEquals(newR.end(), r1.end());
                        testContext.completeNow();
                    })));
            }));
    }
}