package net.cofcool.sourcebox.internal.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.api.entity.*;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.util.QueryBuilder;
import net.cofcool.sourcebox.util.RepositoryUtil;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CustomLog
public class ActionService {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;

    private final SqlRepository<ActionRecord> actionRecordSqlRepository;
    private final SqlRepository<Comment> commentSqlRepository;
    private final SqlRepository<ActionType> actionTypeSqlRepository;
    private final SqlRepository<CheckList>  checklistSqlRepository;
    private final SqlRepository<Habit> habitSqlRepository;
    private final LuceneSearchService searchService;

    private final List<ActionInterceptor> saveInterceptors = List.of(new LinkInterceptor(), new RecordInterceptor(), new LinkLabelInterceptor());

    private final NoteService noteService;

    public ActionService(Vertx vertx, NoteService noteService) {
        this.actionRecordSqlRepository = RepositoryUtil.getRepository(ActionRecord.class);
        this.commentSqlRepository = RepositoryUtil.getRepository(Comment.class);
        this.actionTypeSqlRepository = RepositoryUtil.getRepository(ActionType.class);
        this.checklistSqlRepository = RepositoryUtil.getRepository(CheckList.class);
        this.habitSqlRepository = RepositoryUtil.getRepository(Habit.class);
        this.noteService = noteService;
        this.searchService = LuceneSearchService.get(vertx);
    }

    public Future<Void> saveAll(List<ActionRecord> records) {
        return Future.all(records.stream().map(this::saveAction).toList()).compose(a -> {
            if (a.failed()) {
                return Future.failedFuture(a.cause());
            } else {
                return Future.succeededFuture();
            }
        });
    }

    public Future<Integer> saveBooks(List<ActionRecord> books) {
        var category = books.getFirst().category();
        if (StringUtils.isBlank(category)) {
            return Future.failedFuture("book category is required");
        }
        var query = QueryBuilder.builder()
            .from(ActionRecord.class)
            .select("name")
            .and("type=?", Type.book.name())
            .and("category=?", category);
        return actionRecordSqlRepository.find(query)
            .compose(existing -> {
                var existingNames = new HashSet<>();
                existing.forEach(record -> existingNames.add(record.name()));
                var now = LocalDateTime.now();
                List<ActionRecord> inserts = new ArrayList<>();
                for (ActionRecord book : books) {
                    if (StringUtils.isBlank(book.name())) {
                        continue;
                    }
                    if (existingNames.contains(book.name())) {
                        continue;
                    } else {
                        existingNames.add(book.name());
                        inserts.add(
                            ActionRecord.builder()
                            .name(book.name())
                            .type(Type.book.name())
                            .category(category)
                            .state(ActionState.todo.name())
                            .remark(book.remark())
                            .labels(book.labels())
                            .start(book.start() == null ? now : book.start())
                            .build()
                        );
                    };
                }
                return Future.succeededFuture(inserts);
            })
            .compose(a ->
                Future.all(a.stream().map(actionRecordSqlRepository::insert).toList()).compose(f -> {
                    if (f.failed()) {
                        return Future.failedFuture(f.cause());
                    } else {
                        return Future.succeededFuture(f.size());
                    }
                })
            );
    }

    public Future<ActionRecord> saveAction(ActionRecord record) {
        for (ActionInterceptor interceptor : saveInterceptors) {
            record = interceptor.apply(record);
        }
        var ret = actionRecordSqlRepository.save(record);
        ret.onSuccess(a -> {
            var ncms =  new ArrayList<Comment>();
            ncms.add(Comment
                .builder()
                .id(Utils.generateShortUUID())
                .actionId(a.id())
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                .content(a.generateSysMsg())
                .build()
            );
            if (a.comments() != null) {
                var comments = a.comments().stream().map(c ->
                    new Comment(Utils.generateShortUUID(), a.id(), c, LocalDateTime.now(), LocalDateTime.now())
                ).toList();
                ncms.addAll(comments);
            }
            commentSqlRepository.save(ncms).onFailure(new Log("Save comment error"));

            if (a.category() != null) {
                saveType(a.category(), Type.category, "Save actionType error");
            }
            if (a.device() != null) {
                saveType(a.device(), Type.device, "Save device error");
            }
            if (a.labels() != null) {
                for (String s : a.labels().split(",")) {
                    saveType(s, Type.label, "Save label error");
                }
            }
            searchService.indexAction(a);
        }).onSuccess(this::generateRepeatTodo);
        return ret;
    }

    Future<ActionRecord> generateRepeatTodo(ActionRecord a) {
        if (Objects.equals(a.state(), ActionState.done.name())
        && Objects.equals(a.type(), Type.todo.name())
        && Objects.equals(a.category(), "repeat")) {
           var scheduleRef = RefType.parse(a.refs()).entrySet().stream()
               .filter(e -> e.getKey() == RefType.schedule).findFirst();
            if (scheduleRef.isPresent()) {
                var schedule = scheduleRef.get().getValue();
                ActionRecord next = ActionRecord.builder()
                    .name(a.name())
                    .type(a.type())
                    .category(a.category())
                    .state(ActionState.todo.name())
                    .remark(a.remark())
                    .labels(a.labels())
                    .refs(RefType.appendRefStr(RefType.action.refStr(a.id()), RefType.schedule.refStr(schedule)))
                    .start(a.start().plus(Period.parse(schedule)))
                    .build();
                return saveAction(next)
                .onFailure(new Log("Generate next repeat action error: " + a.name() + "(" + a.id() + ")" + " -> " + schedule));
            }
        }

        return Future.succeededFuture(a);
    }

    public Future<ActionRecord> saveHabit(Habit.HabitRecord habitRecord) {
        var action = habitRecord.action();
        var habit = habitRecord.habit();
        if (action == null || habit == null) {
            throw new IllegalArgumentException("action and habit are required");
        }
        if (!Objects.equals(action.type(), Type.habit.name())) {
            throw new IllegalArgumentException("action type must be habit");
        }
        if (!Objects.equals(habit.category(), "check") && !Objects.equals(habit.category(), "value")) {
            throw new IllegalArgumentException("habit category must be check or value");
        }
        if (habit.category().equals("value")) {
            Habit.TargetType.valueOf(habit.targetType());
        }

        return saveAction(action).compose(savedAction ->
            habitSqlRepository.find(Habit.builder().actionId(savedAction.id()).build())
                .compose(existingHabits -> {
                    Habit existing = existingHabits.stream().findFirst().orElse(null);
                    Habit toSave = Habit.builder()
                        .id(existing == null ? habit.id() : existing.id())
                        .actionId(savedAction.id())
                        .category(habit.category())
                        .note(habit.note())
                        .unit(habit.unit())
                        .freqNum(habit.freqNum())
                        .freqDen(habit.freqDen())
                        .question(habit.question())
                        .targetValue(habit.targetValue())
                        .targetType(habit.targetType())
                        .createTime(existing == null ? habit.createTime() : existing.createTime())
                        .build();
                    return habitSqlRepository.save(toSave).map(savedAction);
                })
        );
    }

    public Future<CheckList> saveHabitValue(String actionId, CheckList param) {
        if (StringUtils.isBlank(actionId) || param == null || param.current() == null) {
            return Future.failedFuture("actionId and habit value are required");
        }
        var value = param.current();
        var operationTime = param.createTime() == null ? LocalDateTime.now() : param.createTime();
        return findHabit(actionId)
            .compose(habit -> {
                if (Objects.equals(habit.category(), "check") && value != 0 && value != 1) {
                    return Future.failedFuture("check habit value must be 0 or 1");
                }
                if (!Objects.equals(habit.category(), "check") && !Objects.equals(habit.category(), "value")) {
                    return Future.failedFuture("unsupported habit category " + habit.category());
                }

                var checklistId = StringUtils.isNotEmpty(param.id()) ? param.id() : Utils.md5(actionId + ":" + operationTime.toLocalDate());
                var checklist = CheckList.builder()
                    .id(checklistId)
                    .content(habit.calcCheckListName(param.content()))
                    .first(0)
                    .last(0)
                    .current(value)
                    .createTime(operationTime)
                    .build();
                return saveCheckList(actionId, checklist, false);
            });
    }

    public Future<Habit> findHabit(String actionId) {
        return habitSqlRepository.find(Habit.builder().actionId(actionId).build())
            .flatMap(a -> {
            if (a.isEmpty()) {
                return Future.failedFuture("habit not found for action " + actionId);
            } else {
                return Future.succeededFuture(a.getFirst());
            }
        });
    }

    public Future<Habit.HabitRecord> findHabitRecord(String id) {
        return actionRecordSqlRepository.find(id)
            .compose(a ->
                Future.all(findHabit(a.id()), findCheckList(a.id()))
                    .compose(c -> Future.succeededFuture(new Habit.HabitRecord(a, c.resultAt(0), c.resultAt(1)))
            ));
    }

    private void saveType(String a, Type label, String error) {
        ActionType actionType = new ActionType(a, label);
        actionTypeSqlRepository
            .find(actionType.name())
            .onFailure(t ->
                actionTypeSqlRepository
                    .save(actionType)
                    .onFailure(new Log(error + " with " + a))
            );
    }

    public Future<List<Comment>> findComment(String actionId) {
        return commentSqlRepository.find(
            QueryBuilder.builder()
                .select().from(Comment.class)
                .and("action_id=?", actionId)
                .orderBy("create_time desc")
        );
    }

    public Future<List<CheckList>> findCheckList(String actionId) {
        return checklistSqlRepository.find(
            QueryBuilder.builder()
                .select().from(CheckList.class)
                .and("action_id=?", actionId)
                .orderBy("create_time desc")
        );
    }

    public Future<List<RecordStatistics>> find15DayRecordStatistics(Type t) {
        var a = actionRecordSqlRepository.execute(
            QueryBuilder.builder()
                .select("CAST(start AS DATE) AS day")
                .select("COUNT(*) AS cnt")
                .select("SUM(COALESCE(duration, 0)) total")
                .from(ActionRecord.class)
                .and("type=?", t.name())
                .groupBy("day")
                .orderBy("day desc")
                .limit(15)
        );

        return a.compose(r -> {
            List<RecordStatistics> list = new ArrayList<>();
            for (Row row : r) {
                list.add(new RecordStatistics(row.getLocalDate("DAY"), row.getLong("CNT"), row.getLong("TOTAL")));
            }
            return Future.succeededFuture(list);
        });
    }

    public Future<List<RecordDateStatistic>> findRecordStatistics(Type t, String dateType, @Nullable String year,  @Nullable String state,  @Nullable String category) {
        var dt = switch (dateType) {
            case "month" -> "EXTRACT(month from end) AS dt";
            case "year" -> "EXTRACT(year from end) AS dt";
            default -> throw new IllegalArgumentException("dateType must be month or year");
        };
        var a = actionRecordSqlRepository.execute(
            QueryBuilder.builder()
                .select(dt)
                .select("COUNT(*) AS cnt")
                .select("SUM(COALESCE(duration, 0)) AS total")
                .select("ROUND(AVG(duration), 2) AS avg_duration")
                .select("SUM(CASE WHEN state='done' THEN 1 ELSE 0 END) AS completed")
                .from(ActionRecord.class)
                .and("type=?", t.name())
                .and(year != null, "end >= ? and end < ?", () -> {
                    var yearStart = LocalDate.of(Integer.parseInt(year), 1, 1);
                    var yearEnd = yearStart.plusYears(1);
                    return new LocalDate[]{yearStart, yearEnd};
                })
                .and(StringUtils.isNotBlank(state), "state=?", () -> new String[]{state})
                .and(StringUtils.isNotBlank(category), "category=?", () -> new String[]{category})
                .groupBy("dt")
                .orderBy("dt")
        );

        return a.compose(r -> {
            List<RecordDateStatistic> list = new ArrayList<>();
            for (Row row : r) {
                list.add(new RecordDateStatistic(
                    row.getInteger("DT"),
                    row.getLong("CNT"),
                    row.getLong("TOTAL"),
                    row.getDouble("AVG_DURATION"),
                    row.getLong("COMPLETED")
                ));
            }
            if ("month".equals(dateType)) {
                list = fillMonths(list);
            }
            return Future.succeededFuture(list);
        });
    }

    private List<RecordDateStatistic> fillMonths(List<RecordDateStatistic> data) {
        Map<Integer, RecordDateStatistic> map = data.stream()
            .collect(Collectors.toMap(
                RecordDateStatistic::date,
                Function.identity()
            ));

        return IntStream.rangeClosed(1, 12)
            .mapToObj(month -> map.getOrDefault(
                month,
                new RecordDateStatistic(month, 0L, 0L, 0D, 0L)
            ))
            .toList();
    }

    public Future<ActionRecord> example() {
        return Future.succeededFuture(new ActionRecord(
            "example",
            null,
            null,
            "Browser",
            Type.record.name(),
            "video",
            "init",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            null,
            5,
            List.of("start watch"),
            "example",
            String.join(":", RefType.action.name(), "id"),
            LocalDateTime.now()
        ));
    }

    public Future<ActionRecord.Records> findAllRefs(String id) {
        return Future.all(
                actionRecordSqlRepository.find(id),
                actionRecordSqlRepository.find(new ActionRecord(RefType.action.refStr(id)))
            )
            .flatMap(r ->
                Future.succeededFuture(new ActionRecord.Records(r.resultAt(0), r.resultAt(1)))
            );
    }

    public Future<List<ActionRecord>> find(ActionRecord record) {
        return find(record, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
    }

    public Future<List<ActionRecord>> find(ActionRecord record, int page, int size) {
        if (page < 1 || size < 1) {
            return Future.failedFuture("page and size must be positive");
        }

        long offset = (long) (page - 1) * size;
        var builder = QueryBuilder.builder().select().from(ActionRecord.class)
            .orderBy("create_time desc")
            .limit(size);
        if (offset > 0) {
            builder.offset(offset);
        }
        if (StringUtils.isNotBlank(record.name())) {
            builder.and("name like ?", "%" + record.name() + "%");
        }
        if (StringUtils.isNotBlank(record.id()) && record.checkId()) {
            builder.and("id=?", record.id());
        }
        if (StringUtils.isNotBlank(record.type())) {
            builder.and("type=?", record.type());
        }
        if (StringUtils.isNotBlank(record.state())) {
            builder.and("state=?", record.state());
        }
        if (StringUtils.isNotBlank(record.category())) {
            builder.and("category=?", record.category());
        }
        if (StringUtils.isNotBlank(record.labels())) {
            builder.and("labels=?", record.labels());
        }
        if (StringUtils.isNotBlank(record.refs())) {
            builder.and("refs like ?", "%" + record.refs() + "%");
        }
        return actionRecordSqlRepository.find(builder);
    }

    public Future<ActionRecord.RecordRet> find(String id) {
        return actionRecordSqlRepository.find(id)
            .compose(a ->
                Future.all(findComment(a.id()), findCheckList(a.id()))
                    .compose(c -> Future.succeededFuture(new ActionRecord.RecordRet(a, c.resultAt(0), c.resultAt(1)))))
            .compose(a ->
                Future.all(RefType.parse(a.record().refs()).entrySet().stream().map(ref ->
                        switch (ref.getKey()) {
                            case action -> find(ref.getValue());
                            case note -> noteService.find(new Note(ref.getValue()));
                            default -> Future.succeededFuture();
                        }
                    ).toList()
                ).compose(f -> Future.succeededFuture(
                    new ActionRecord.RecordRet(a.record(), a.comments(), a.checklists(), f)
                )));
    }

    public Future<Map<String, Object>> findAllType() {
        return actionTypeSqlRepository.find()
            .flatMap(a -> Future.succeededFuture(
                Map.of("types", a, "states", Arrays.toString(ActionState.values()))
            ));
    }

    public Future<List<ActionType>> findType(ActionType type) {
        return actionTypeSqlRepository.find(type);
    }

    public Future<Boolean> deleteActions(String id) {
        return delete(id, actionRecordSqlRepository::delete)
            .compose(a -> deleteCommentsByActionId(id))
            .andThen(a -> deleteCheckListByActionId(id))
            .andThen(a -> deleteHabitsByActionId(id))
            .andThen(a -> searchService.delete("action", id).map(true));
    }

    public Future<Boolean> deleteHabit(String actionId) {
        return actionRecordSqlRepository.find(actionId)
            .compose(action -> {
                if (!Objects.equals(action.type(), Type.habit.name())) {
                    return Future.failedFuture("action is not a habit: " + actionId);
                }
                return deleteActions(actionId);
            });
    }

    private Future<Boolean> deleteHabitsByActionId(String actionId) {
        return habitSqlRepository.find(Habit.builder().actionId(actionId).build())
            .compose(habits -> Future.all(habits.stream()
                .map(habit -> habitSqlRepository.delete(habit.id()))
                .toList()).map(true));
    }

    public Future<Boolean> deleteComments(String id) {
        return commentSqlRepository.find(id)
            .compose(comment -> commentSqlRepository.delete(id)
                .compose(deleted -> refreshActionIndex(comment.actionId()).map(true)));
    }

    public Future<Boolean> deleteCommentsByActionId(String actionId) {
        return commentSqlRepository
            .find(
                QueryBuilder.builder().from(Comment.class).select().and("action_id=?", actionId))
            .compose(
                a -> Future.all(a.stream().map(c -> commentSqlRepository.delete(c.id())).toList()))
            .compose(a -> Future.succeededFuture(true));
    }

    public Future<Boolean> deleteCheckList(String id) {
        return delete(id, checklistSqlRepository::delete);
    }

    public Future<Boolean> deleteCheckListByActionId(String actionId) {
        return checklistSqlRepository
            .find(
                QueryBuilder.builder().from(CheckList.class).select().and("action_id=?", actionId))
            .compose(
                a -> Future.all(a.stream().map(c -> checklistSqlRepository.delete(c.id())).toList()))
            .compose(a -> Future.succeededFuture(true));
    }

    private Future<Boolean> delete(String id, Function<String, Future<Boolean>> deleteFunc) {
        return deleteFunc.apply(id).compose(a -> Future.succeededFuture(true));
    }

    private <T, R, A> Future<R> findAll(Function<ActionRecord.RecordRet, T> mapper, Collector<? super T, A, R> collector) {
        return Future.all(actionRecordSqlRepository.find(), commentSqlRepository.find(), checklistSqlRepository.find())
            .compose(a -> {
                List<ActionRecord> r = a.result().resultAt(0);
                List<Comment> c = a.result().resultAt(1);
                var commentMap = c.stream().collect(Collectors.groupingBy(Comment::actionId));
                List<CheckList> cl = a.result().resultAt(2);
                var checkListMap = cl.stream().collect(Collectors.groupingBy(CheckList::actionId));
                return Future.succeededFuture(r
                    .stream()
                    .map(record -> new ActionRecord.RecordRet(record, commentMap.getOrDefault(record.id(), List.of()), checkListMap.getOrDefault(record.id(), List.of())))
                    .map(mapper)
                    .collect(collector)
                );
            });
    }

    public Future<String> export2Md() {
        return findAll(ActionRecord::toMarkdown, Collectors.joining("\n"));
    }

    public Future<List<ActionRecord.RecordRet>> find() {
        return findAll(
            a -> a,
            Collectors.toList()
        );
    }

    public Future<Comment> saveComment(String actionId, Comment pojo) {
        return commentSqlRepository.save(Comment.builder()
            .id(System.currentTimeMillis() + "")
            .actionId(actionId).content(pojo.content())
            .createTime(pojo.createTime() != null ? pojo.createTime() : LocalDateTime.now()).updateTime(LocalDateTime.now())
            .build()
        ).compose(comment -> refreshActionIndex(actionId).map(comment));
    }

    private Future<Void> refreshActionIndex(String actionId) {
        return actionRecordSqlRepository.find(actionId)
            .compose(action -> commentSqlRepository.find(new Comment(actionId))
                .compose(comments -> searchService.indexAction(action)));
    }

    public Future<CheckList> saveCheckList(String actionId, CheckList pojo, boolean saveComment) {
        return checklistSqlRepository.save(CheckList.builder()
            .id(StringUtils.isBlank(pojo.id()) ? System.currentTimeMillis() + "" : pojo.id())
            .actionId(actionId)
            .content(pojo.content())
            .first(pojo.first())
            .last(pojo.last())
            .current(pojo.current())
            .createTime(pojo.createTime())
            .build()
        ).onSuccess( c -> {
                if (saveComment) {
                    var ct = LocalDateTime.now();
                    var content = "sys-action-checklist-";
                    if (c.first().equals(0) && c.current().equals(0)) {
                        content = content + "created: " + c.content();
                        ct = pojo.createTime();
                    } else if (Objects.equals(c.current(), c.last())) {
                        content = content + "finished: " + c.content();
                    } else {
                        content = content + "updated: " + c.content() + " --- " + c.current();
                    }
                    saveComment(
                        c.actionId(),
                        Comment.builder()
                            .actionId(actionId)
                            .content(content)
                            .createTime(ct)
                            .build()
                    );
                }
            }
        );
    }

    private static class LinkLabelInterceptor implements ActionInterceptor {

        @Override
        public ActionRecord apply(ActionRecord actionRecord) {
            String label = actionRecord.labels();
            if (actionRecord.remark() != null && actionRecord.remark().toLowerCase().startsWith("http")) {
                if (StringUtils.isBlank(label)) {
                    label = "link";
                } else if (!label.contains("link")) {
                    label = String.join(",",label, "link");
                }
            }
            return actionRecord.withLabels(label);
        }
    }
    private static class LinkInterceptor implements ActionInterceptor {

        @Override
        public ActionRecord apply(ActionRecord record) {
            if (record.name() != null && record.name().toLowerCase().startsWith("http") && StringUtils.isBlank(record.remark())) {
                try {
                    var title = Jsoup.newSession().url(record.name()).timeout(5_000).get().title();
                    record = record
                        .withName(title)
                        .withRemark(record.name());
                } catch (IOException e) {
                    log.error("Get " + record.name() + " web page title error", e);
                }
            }
            return record;
        }
    }

    private static class RecordInterceptor implements ActionInterceptor {

        @Override
        public ActionRecord apply(ActionRecord record) {
            if (Objects.equals(record.type(), Type.record.name())
                && Objects.equals(record.state(), ActionState.done.name())
                && record.end() == null
            ) {
                    record = record.withEnd(LocalDateTime.now());
            }
            return record;
        }
    }

    private record Log(
        String msg
    ) implements Handler<Throwable> {

        @Override
        public void handle(Throwable event) {
            log.info(msg, event);
        }
    }

    public record RecordStatistics (
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate day,
        Long cnt,
        Long total
    ){}

    public record RecordDateStatistic(
        Integer date,
        Long total,
        Long totalDuration,
        Double averageDuration,
        Long completed
    ) {}

}
