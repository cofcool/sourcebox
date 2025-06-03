package net.cofcool.sourcebox.internal.simplenote;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.simplenote.entity.ActionRecord;
import net.cofcool.sourcebox.internal.simplenote.entity.ActionState;
import net.cofcool.sourcebox.internal.simplenote.entity.ActionType;
import net.cofcool.sourcebox.internal.simplenote.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.simplenote.entity.Comment;
import net.cofcool.sourcebox.internal.simplenote.entity.Note;
import net.cofcool.sourcebox.internal.simplenote.entity.RefType;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.Utils;

@CustomLog
public class ActionService {

    private final SqlRepository<ActionRecord> actionRecordSqlRepository;
    private final SqlRepository<Comment> commentSqlRepository;
    private final SqlRepository<ActionType> actionTypeSqlRepository;

    private final NoteService noteService;

    public ActionService(Vertx vertx, NoteService noteService) {
        this.actionRecordSqlRepository = SqlRepository.create(vertx, ActionRecord.class);
        this.commentSqlRepository = SqlRepository.create(vertx, Comment.class);
        this.actionTypeSqlRepository = SqlRepository.create(vertx, ActionType.class);
        this.noteService = noteService;
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

    public Future<ActionRecord> saveAction(ActionRecord record) {
        var newRecord = ActionRecord.copy(record);
        var ret = actionRecordSqlRepository.save(newRecord);
        ret.onSuccess(a -> {
            if (a.comments() != null) {
                var comments = a.comments().stream().map(c ->
                    new Comment(Utils.generateShortUUID(), a.id(), c, LocalDateTime.now(), LocalDateTime.now())
                ).toList();
                commentSqlRepository.save(comments).onFailure(new Log("Save comment error"));
            }
            if (a.type() != null) {
                saveType(a.type(), Type.action, "Save actionType error");
            }
            if (a.device() != null) {
                saveType(a.device(), Type.device, "Save device error");
            }
            if (a.state() != null) {
                saveType(a.state(), Type.state, "Save device error");
            }
            if (a.labels() != null) {
                for (String s : a.labels().split(",")) {
                    saveType(s, Type.label, "Save label error");
                }
            }
        });

        return ret;
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
        return commentSqlRepository.find(new Comment(actionId));
    }

    public Future<ActionRecord> example() {
        return Future.succeededFuture(new ActionRecord(
            "example",
            null,
            null,
            "Browser",
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
        return actionRecordSqlRepository.find(record);
    }

    public Future<ActionRecord.RecordRet> find(String id) {
        return actionRecordSqlRepository.find(id)
            .compose(a ->
                findComment(a.id())
                    .compose(c -> Future.succeededFuture(new ActionRecord.RecordRet(a, c))))
            .compose(a -> {
                var ref = RefType.parse(a.record().refs());
                var refObj = switch (ref.getKey()) {
                    case action -> find(ref.getValue());
                    case note -> noteService.find(new Note(ref.getValue()));
                    default -> Future.succeededFuture();
                };
                return refObj.compose(f -> Future.succeededFuture(
                    new ActionRecord.RecordRet(a.record(), a.comments(), f)
                ));
            });
    }

    public Future<Map<String, Object>> findAllType() {
        return actionTypeSqlRepository.find()
            .flatMap(a -> Future.succeededFuture(
                Map.of("types", a, "states", Arrays.toString(ActionState.values()))
            ));
    }

    public Future<String> deleteActions(Set<String> ids) {
        return delete(ids, actionRecordSqlRepository::delete);
    }

    public Future<String> deleteComments(Set<String> ids) {
        return delete(ids, commentSqlRepository::delete);
    }

    private Future<String> delete(Set<String> ids, Function<String, Future<Void>> deleteFunc) {
        var ret = ids.stream()
            .map(deleteFunc)
            .collect(Collectors.toList());
        return Future.all(ret).compose(a ->
            a.failed() ? Future.failedFuture(a.cause()) : Future.succeededFuture(ids.toString())
        );
    }

    private <T, R, A> Future<R> findAll(Function<ActionRecord.RecordRet, T> mapper, Collector<? super T, A, R> collector) {
        return Future.all(actionRecordSqlRepository.find(), commentSqlRepository.find())
            .compose(a -> {
                List<ActionRecord> r = a.result().resultAt(0);
                List<Comment> c = a.result().resultAt(1);
                var commentMap = c.stream().collect(Collectors.groupingBy(Comment::actionId));
                return Future.succeededFuture(r
                    .stream()
                    .map(record -> new ActionRecord.RecordRet(record, commentMap.getOrDefault(record.id(), List.of())))
                    .map(mapper)
                    .collect(collector)
                );
            });
    }

    public Future<String> export2Md() {
        return findAll(ActionRecord::toMarkdown, Collectors.joining("\n"));
    }

    public Future<List<ActionRecord>> find() {
        return findAll(a -> {
                ActionRecord record = a.record();
                return new ActionRecord(record.id(), record.name(), record.icon(),
                    record.index(), record.device(), record.type(), record.state(), record.start(),
                    record.end(),
                    record.duration(), record.rating(),
                    a.comments().stream().map(Comment::content).toList(), record.labels(),
                    record.refs(), record.remark(), record.createTime(), LocalDateTime.now());
            },
            Collectors.toList()
        );
    }

    private record Log(
        String msg
    ) implements Handler<Throwable> {

        @Override
        public void handle(Throwable event) {
            log.error(msg, event);
        }
    }

}
