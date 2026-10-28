package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.internal.api.entity.*;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.logging.LoggerFactory;
import net.cofcool.sourcebox.util.SqlRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@ExtendWith(VertxExtension.class)
class ActionServiceTest extends BaseTest {

    static ActionService actionService;

    static ActionRecord defaultRecord;

    @BeforeAll
    static void setup(Vertx vertx, VertxTestContext testContext) {
        LoggerFactory.setDebug(true);
        SqlRepository.init(vertx);
        actionService = new ActionService(vertx, new NoteService(vertx));
        actionService.saveAction(new ActionRecord(
                "test video",
                null,
                null,
                "mac",
                Type.record.name(),
                "video",
                "doing",
                LocalDateTime.now(),
                null,
                null,
                5,
                List.of("first"),
                "test,demo",
                null,
                LocalDateTime.now()
            ))
            .compose(r -> {
                defaultRecord = r;
                return actionService.saveAction(new ActionRecord(
                    "test video1",
                    null,
                    null,
                    "mac",
                    Type.record.name(),
                    "video",
                    "doing",
                    LocalDateTime.now(),
                    null,
                    null,
                    5,
                    null,
                    null,
                    RefType.action.refStr(r.id()),
                    LocalDateTime.now()
                ));
            })
            .onComplete(testContext.succeeding(r -> {
                testContext.verify(testContext::completeNow);
            }));

    }

    @Test
    @Order(Integer.MAX_VALUE)
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
    void findById(Vertx vertx, VertxTestContext testContext) {
        actionService
            .find(defaultRecord.id())
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertNotNull(r);
                System.out.println(r);
                testContext.completeNow();
            })));
    }

    @Test
    void fullTextSearchIndexesActionFieldsAndComments(Vertx vertx, VertxTestContext testContext) {
        LuceneSearchService.get(vertx)
            .search("first", "action")
            .onComplete(testContext.succeeding(results -> testContext.verify(() -> {
                Assertions.assertTrue(results.stream().anyMatch(result ->
                    result.id().equals(defaultRecord.id()) && result.data() instanceof ActionRecord
                ));
                testContext.completeNow();
            })));
    }

    @Test
    void fullTextSearchIndexesNoteContent(Vertx vertx, VertxTestContext testContext) {
        Note note = Note.init("这是用于 Lucene 全文搜索的测试内容");
        NoteService noteService = new NoteService(vertx);
        noteService.save(note)
            .compose(saved -> LuceneSearchService.get(vertx).search("全文搜索", "note"))
            .onComplete(testContext.succeeding(results -> testContext.verify(() -> {
                Assertions.assertTrue(results.stream().anyMatch(result ->
                    result.id().equals(note.id()) && result.data() instanceof Note
                ));
                testContext.completeNow();
            })));
    }

    @Test
    void findByRecord(Vertx vertx, VertxTestContext testContext) {
        actionService
            .find(defaultRecord)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertNotNull(r);
                System.out.println(r);
                testContext.completeNow();
            })));
    }

    @Test
    void findByRecordPage(Vertx vertx, VertxTestContext testContext) {
        LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        List<ActionRecord> records = IntStream.range(0, 3)
            .mapToObj(i -> new ActionRecord(
                "pagination-" + i,
                null,
                null,
                "mac",
                Type.record.name(),
                "pagination-test",
                "doing",
                baseTime.plusDays(i),
                null,
                null,
                5,
                null,
                null,
                null,
                baseTime.plusDays(i)
            ))
            .toList();
        ActionRecord condition = ActionRecord.builder().category("pagination-test").build();

        actionService.saveAll(records)
            .compose(ignored -> Future.all(
                actionService.find(condition, 1, 2),
                actionService.find(condition, 2, 2)
            ))
            .onComplete(testContext.succeeding(pages -> testContext.verify(() -> {
                List<ActionRecord> firstPage = pages.resultAt(0);
                List<ActionRecord> secondPage = pages.resultAt(1);
                Assertions.assertEquals(List.of("pagination-2", "pagination-1"),
                    firstPage.stream().map(ActionRecord::name).toList());
                Assertions.assertEquals(List.of("pagination-0"),
                    secondPage.stream().map(ActionRecord::name).toList());
                testContext.completeNow();
            })));
    }

    @Test
    void findAllRefs(Vertx vertx, VertxTestContext testContext) {
        actionService
            .findAllRefs(defaultRecord.id())
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertNotNull(r);
                Assertions.assertFalse(r.refs().isEmpty());
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
            Type.record.name(),
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
            .compose(r -> actionService.deleteActions(r.id()))
            .compose(r -> actionService.deleteCommentsByActionId(newR.id()))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertNotNull(r);
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
    void findRecordStatistics(Vertx vertx, VertxTestContext testContext) {
        actionService
            .findRecordStatistics(Type.record)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                System.out.println(r);
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void example(Vertx vertx, VertxTestContext testContext) {
        actionService
            .example()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertNotNull(r);
                testContext.completeNow();
            })));
    }

    @Test
    void saveComment(Vertx vertx, VertxTestContext testContext) {
        actionService
            .saveComment(defaultRecord.id(), Comment.builder().content("aaaaaa").build())
            .onComplete(testContext.succeeding(r -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void saveCheckList(Vertx vertx, VertxTestContext testContext) {
        actionService
            .saveCheckList(defaultRecord.id(), CheckList.builder().content("01").first(0).last(10).current(2).build())
            .onComplete(testContext.succeeding(r -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void toMarkdown(Vertx vertx, VertxTestContext testContext) {
        actionService
            .export2Md()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertNotNull(r);
                System.out.println(r);
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
            null,
            LocalDateTime.now().plusDays(5),
            null,
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
                Assertions.assertEquals(newR.end().toLocalDate(), r1.record().end().toLocalDate());
                testContext.completeNow();
            })));
    }
}