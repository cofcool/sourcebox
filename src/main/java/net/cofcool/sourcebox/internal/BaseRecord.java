package net.cofcool.sourcebox.internal;

import static net.cofcool.sourcebox.internal.api.entity.ActionRecord.toPrintStr;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.NoteIndex;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public abstract class BaseRecord implements WebTool {

    @Override
    public void run(Args args) throws Exception {
        var flag = new AtomicBoolean();

        args.readArg("add").ifPresent(a -> {
            flag.set(true);
            callAdd(a);
        });

        args.readArg("done").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.done);
        });

        args.readArg("cancel").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.cancel);
        });

        if (callOthers(args)) {
            flag.set(true);
        }

        var find = args.readArg("find");
        if (!flag.get() || find.isPresent()) {
            var method = "/action?type=" + currentType().name();
            if (find.isPresent()) {
                method = method + "&" + find.val();
            }

            var d = getRequestLocalData(method, Actions.class);
            args.getContext().write(toPrintStr(d));
        }

    }

    protected boolean callOthers(Args args) throws Exception {
        return false;
    }

    protected void callAdd(Arg a) {
        if (a.val().isBlank()) {
            var name = Utils.readLine("name");
            var remark = Utils.readLine("remark");
            saveRecord(ActionState.todo, name, remark, null);
        } else {
            saveRecord(ActionState.todo, a.val(), null, null);
        }
    }

    protected void updateState(ActionRecord record) {
        postRequestLocalData(
            "/action",
            record,
            ActionRecord.class);
    }

    protected void updateState(String id, ActionState state) {
        updateState(
            ActionRecord
                .builder()
                .id(id)
                .state(state.name())
                .build()
        );
    }

    protected void saveRecord(ActionState state, String name, String remark, String category) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (state == null) {
            state = ActionState.todo;
        }
        if (StringUtils.isBlank(remark)) {
            remark = null;
        }
        var param = ActionRecord.builder()
            .name(name)
            .state(state.name())
            .remark(remark)
            .category(category)
            .type(currentType().name())
            .start(LocalDateTime.now())
            .end(LocalDateTime.now())
            .build();
        updateState(param);
    }

    protected abstract Type currentType();

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("find", null, "find condition, use url query string", false,
                "state=todo&id=d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("add", null, "add new item", false,
                "buy something"))
            .arg(new Arg("done", null, "mark item done by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("cancel", null, "mark item cancel by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"));
    }

    @Override
    public Class<? extends WebRouter> routerType() {
        return NoteIndex.class;
    }

    @Override
    public boolean supportCommand() {
        return true;
    }

    public static class Actions extends ListData<ActionRecord> {

    }

}
