package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.internal.simplenote.entity.ActionRecord;
import net.cofcool.toolbox.internal.simplenote.entity.Comment;
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
    void delete(Vertx vertx, VertxTestContext testContext) {
        ActionRecord newR = new ActionRecord(
            "test",
            "asdasda",
            null,
            "mac",
            "xxx",
            "init",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(5),
            null,
            null,
            List.of("xx1", "xx2"),
            null,
            null,
            LocalDateTime.now()
        );
        actionService
            .saveAction(newR)
            .compose(r -> actionService.deleteActions(Collections.singleton(r.id())))
            .compose(r -> actionService
                .findComment(newR.id())
                .compose(r1 -> actionService.deleteComments(r1.stream().map(Comment::id).collect(Collectors.toSet())))
            )
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertNull(r);
                testContext.completeNow();
            })));
    }

    @Test
    void findAllType(Vertx vertx, VertxTestContext testContext) {
        actionService
            .findAllType()
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
            .compose(r -> actionService.find(r.id()))
            .onComplete(testContext.succeeding(r1 -> testContext.verify(() -> {
                System.out.println(r1);
                Assertions.assertEquals(newR.end(), r1.end());
                testContext.completeNow();
            })));
    }
}