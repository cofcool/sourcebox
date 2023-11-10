package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.entity.ActionRecord;
import net.cofcool.toolbox.internal.simplenote.entity.ActionType;
import net.cofcool.toolbox.internal.simplenote.entity.Comment;
import net.cofcool.toolbox.util.SqlRepository;
import net.cofcool.toolbox.util.Utils;

@CustomLog
public class ActionService {

    private final SqlRepository<ActionRecord> actionRecordSqlRepository;
    private final SqlRepository<Comment> commentSqlRepository;
    private final SqlRepository<ActionType> actionTypeSqlRepository;

    public ActionService(Vertx vertx) {
        this.actionRecordSqlRepository = SqlRepository.create(vertx, ActionRecord.class);
        this.commentSqlRepository = SqlRepository.create(vertx, Comment.class);
        this.actionTypeSqlRepository = SqlRepository.create(vertx, ActionType.class);
    }

    public Future<ActionRecord> saveAction(ActionRecord record) {
        var newRecord = ActionRecord.copy(record);
        var ret = actionRecordSqlRepository.save(newRecord);
        ret.onSuccess(a -> {
            if (a.comments() != null) {
                var comments = a.comments().stream().map(c ->
                    new Comment(Utils.generateShorUUID(), a.id(), c, LocalDateTime.now(), LocalDateTime.now())
                ).toList();
                commentSqlRepository.save(comments).onFailure(new Log("Save comment error"));
            }
            if (a.type() != null) {
                ActionType actionType = new ActionType(a.type());
                actionTypeSqlRepository
                    .find(actionType.name())
                    .onFailure(t ->
                        actionTypeSqlRepository
                            .save(actionType)
                            .onFailure(new Log("Save comment error"))
                    );
            }
        });

        return ret;
    }

    public Future<List<ActionRecord>> find() {
        return actionRecordSqlRepository.find();
    }

    public Future<List<Comment>> findComment(String actionId) {
        return commentSqlRepository.find(new Comment(actionId));
    }

    public Future<ActionRecord> find(String id) {
        return actionRecordSqlRepository.find(id);
    }

    public Future<List<String>> findAllType() {
        return actionTypeSqlRepository.find().compose(a -> Future.succeededFuture(a.stream().map(ActionType::name).toList()));
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
            .filter(Future::failed)
            .map(a -> a.cause().getMessage())
            .collect(Collectors.joining("\n"));
        return ret.isEmpty() ? Future.succeededFuture() : Future.failedFuture(ret);
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
