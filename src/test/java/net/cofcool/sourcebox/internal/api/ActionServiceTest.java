package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.IntStream;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.CheckList;
import net.cofcool.sourcebox.internal.api.entity.Comment;
import net.cofcool.sourcebox.internal.api.entity.Habit;
import net.cofcool.sourcebox.internal.api.entity.Note;
import net.cofcool.sourcebox.internal.api.entity.RefType;
import net.cofcool.sourcebox.logging.LoggerFactory;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
    void recommendationIsIdempotentAndCanBeCancelled(VertxTestContext testContext) {
        actionService.recommend(defaultRecord.id())
            .compose(first -> actionService.recommend(defaultRecord.id())
                .compose(second -> {
                    Assertions.assertEquals(first.id(), second.id());
                    return actionService.findRecommendations();
                })
                .compose(recommendations -> {
                    Assertions.assertEquals(
                        1,
                        recommendations.stream()
                            .filter(recommend -> defaultRecord.id().equals(recommend.dataId()))
                            .count()
                    );
                    return actionService.cancelRecommendation(defaultRecord.id());
                })
                .compose(ignored -> actionService.cancelRecommendation(defaultRecord.id()))
                .compose(ignored -> actionService.findRecommendations())
                .onSuccess(recommendations -> testContext.verify(() -> {
                    Assertions.assertTrue(
                        recommendations.stream().noneMatch(recommend -> defaultRecord.id().equals(recommend.dataId()))
                    );
                    testContext.completeNow();
                }))
            )
            .onFailure(testContext::failNow);
    }

    @Test
    void fullTextSearchIndexesActionFieldsAndComments(Vertx vertx, VertxTestContext testContext) {
        LuceneSearchService.get(vertx)
            .search("test", "action", "record")
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
            .compose(saved -> LuceneSearchService.get(vertx).search("全文搜索", "note", null))
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
    void statisticsUsesSqlAggregates(VertxTestContext testContext) {
        actionService.findStatistics(null, null, null)
            .onComplete(testContext.succeeding(statistics -> testContext.verify(() -> {
                Assertions.assertTrue(statistics.total() >= 2);
                Assertions.assertFalse(statistics.statusCounts().isEmpty());
                Assertions.assertFalse(statistics.statusOptions().isEmpty());
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
    void saveAllBookSyncDeduplicatesByNameAndCategory(Vertx vertx, VertxTestContext testContext) {
        String category = "book-sync-" + java.util.UUID.randomUUID();
        var existing = ActionRecord.builder()
            .name("already here.epub")
            .type(Type.book.name())
            .category(category)
            .state("doing")
            .build();
        var duplicate = ActionRecord.builder()
            .name("already here.epub")
            .type(Type.book.name())
            .category(category)
            .state("cancel")
            .remark("new path")
            .build();
        var firstNew = ActionRecord.builder()
            .name("new book.cbz")
            .type(Type.book.name())
            .category(category)
            .state("cancel")
            .remark("series/new book.cbz")
            .labels("cbz")
            .build();
        var repeatedNew = ActionRecord.builder()
            .name("new book.cbz")
            .type(Type.book.name())
            .category(category)
            .remark("duplicate path")
            .build();

        actionService.saveBooks(List.of(existing))
            .compose(ignored -> actionService.saveBooks(List.of(duplicate, firstNew, repeatedNew)))
            .compose(ignored -> actionService.find(
                ActionRecord.builder().type(Type.book.name()).category(category).build(),
                1,
                20
            ))
            .onComplete(testContext.succeeding(books -> testContext.verify(() -> {
                Assertions.assertEquals(2, books.size());
                ActionRecord saved = books.stream()
                    .filter(book -> book.name().equals("new book.cbz"))
                    .findFirst()
                    .orElseThrow();
                Assertions.assertEquals("series/new book.cbz", saved.remark());
                Assertions.assertEquals("cbz", saved.labels());
                Assertions.assertEquals("todo", saved.state());
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
            .find15DayRecordStatistics(Type.record)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                System.out.println(r);
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void findRecordMonthStatistics(Vertx vertx, VertxTestContext testContext) {
        actionService
            .findRecordStatistics(Type.record, "month", "2026", ActionState.todo.name(), "video")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                System.out.println(r);
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void findRecordYearStatistics(Vertx vertx, VertxTestContext testContext) {
        actionService
            .findRecordStatistics(Type.record, "year", null, null, null)
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
            .saveCheckList(defaultRecord.id(), CheckList.builder().content("01").first(0).last(10).current(2).build(), true)
            .onComplete(testContext.succeeding(r -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void saveRepeatTodoAction(Vertx vertx, VertxTestContext testContext) {
        String schedule = Period.ofMonths(1).toString();
        ActionRecord record = new ActionRecord(
            "test repeat todo",
            "repeat todo",
            null,
            "mac",
            Type.todo.name(),
            "repeat",
            ActionState.done.name(),
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            "xx",
            RefType.appendRefStr(RefType.schedule.refStr(schedule)),
            LocalDateTime.now()
        );
        actionService
            .generateRepeatTodo(record)
            .compose(r -> actionService.find(ActionRecord.builder().refs(RefType.action.refStr(record.id())).build())
                .onComplete(testContext.succeeding(actions -> testContext.verify(() -> {
                    Assertions.assertFalse(actions.isEmpty());
                    Assertions.assertTrue(actions.stream().anyMatch(a -> a.refs().contains(RefType.schedule.refStr(schedule))));
                    Assertions.assertTrue(actions.stream().anyMatch(a -> a.start().minusMonths(1).toLocalDate().equals(record.start().toLocalDate())));
                    testContext.completeNow();
                }))));
    }

    @Test
    void saveHabitAndCheckValue(Vertx vertx, VertxTestContext testContext) {
        ActionRecord action = ActionRecord.builder()
            .name("check habit")
            .type(Type.habit.name())
            .state("doing")
            .start(LocalDateTime.now())
            .build();
        Habit habit = Habit.builder()
            .category("check")
            .freqNum(1)
            .freqDen(1)
            .question("Did you exercise?")
            .build();

        actionService.saveHabit(new Habit.HabitRecord(action, habit))
            .compose(saved -> actionService.findHabit(saved.id())
            )
            .compose(saved -> actionService.saveHabitValue(
                saved.actionId(),
                CheckList.builder().current(1).build()
            ))
            .onComplete(testContext.succeeding(savedValue -> testContext.verify(() -> {
                Assertions.assertEquals(1, savedValue.current());
                testContext.completeNow();
            })));
    }

    @Test
    void saveHabitValueRejectsNonBooleanCheckValue(Vertx vertx, VertxTestContext testContext) {
        ActionRecord action = ActionRecord.builder()
            .name("invalid check habit")
            .type(Type.habit.name())
            .state("doing")
            .start(LocalDateTime.now())
            .build();
        Habit habit = Habit.builder().category("check").build();

        actionService.saveHabit(new Habit.HabitRecord(action, habit))
            .compose(saved -> actionService.saveHabitValue(
                saved.id(),
                CheckList.builder().content("desc").current(21).build()
            ))
            .onComplete(testContext.failing(error -> testContext.verify(() -> {
                Assertions.assertTrue(error.getMessage().contains("must be 0 or 1"));
                testContext.completeNow();
            })));
    }

    @Test
    void saveHabitValuePreservesValueCategoryNumber(Vertx vertx, VertxTestContext testContext) {
        ActionRecord action = ActionRecord.builder()
            .name("value habit")
            .type(Type.habit.name())
            .state("doing")
            .start(LocalDateTime.now())
            .build();
        Habit habit = Habit.builder().category("value")
            .targetType(Habit.TargetType.AtLeast.name()).unit("minutes").build();

        actionService.saveHabit(new Habit.HabitRecord(action, habit))
            .compose(saved -> actionService.saveHabitValue(
                saved.id(),
                CheckList.builder().current(42).build()
            ))
            .onComplete(testContext.succeeding(savedValue -> testContext.verify(() -> {
                Assertions.assertEquals(42, savedValue.current());
                testContext.completeNow();
            })));
    }

    @Test
    void saveHabitValueUpdatesTheSameDayRecord(Vertx vertx, VertxTestContext testContext) {
        ActionRecord action = ActionRecord.builder()
            .name("daily habit")
            .type(Type.habit.name())
            .state("doing")
            .start(LocalDateTime.now())
            .build();
        Habit habit = Habit.builder()
            .category("value")
            .targetType(Habit.TargetType.AtLeast.name())
            .build();
        LocalDateTime operationTime = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);

        actionService.saveHabit(new Habit.HabitRecord(action, habit))
            .compose(saved -> actionService.saveHabitValue(saved.id(), CheckList.builder()
                .current(10).createTime(operationTime).build()))
            .compose(first -> actionService.saveHabitValue(first.actionId(), CheckList.builder()
                .current(20).createTime(operationTime.plusHours(4)).build()))
            .compose(updated -> actionService.findCheckList(updated.actionId())
                .map(checklists -> new java.util.AbstractMap.SimpleEntry<>(updated, checklists)))
            .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
                Assertions.assertEquals(20, result.getKey().current());
                Assertions.assertEquals(1, result.getValue().size());
                Assertions.assertEquals(20, result.getValue().getFirst().current());
                Assertions.assertEquals(result.getValue().getFirst().id(), result.getKey().id());
                Assertions.assertEquals(
                    Utils.md5(result.getKey().actionId() + ":" + operationTime.toLocalDate()),
                    result.getKey().id()
                );
                testContext.completeNow();
            })));
    }

    @Test
    void deleteHabitRemovesActionAndHabit(Vertx vertx, VertxTestContext testContext) {
        ActionRecord action = ActionRecord.builder()
            .name("deletable habit")
            .type(Type.habit.name())
            .state("doing")
            .start(LocalDateTime.now())
            .build();

        actionService.saveHabit(new Habit.HabitRecord(action, Habit.builder().category("check").build()))
            .compose(saved -> actionService.saveHabitValue(saved.id(), CheckList.builder().current(1).build()))
            .compose(savedValue -> actionService.deleteHabit(action.id()))
            .compose(deleted -> actionService.findCheckList(action.id()).compose(checklists -> {
                Assertions.assertTrue(checklists.isEmpty());
                return actionService.findHabitRecord(action.id());
            }))
            .onComplete(testContext.failing(error -> testContext.verify(() -> {
                Assertions.assertNotNull(error);
                testContext.completeNow();
            })));
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