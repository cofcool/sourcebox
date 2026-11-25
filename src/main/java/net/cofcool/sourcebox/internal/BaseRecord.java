package net.cofcool.sourcebox.internal;

import static net.cofcool.sourcebox.internal.api.entity.ActionRecord.toPrintStr;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public abstract class BaseRecord implements WebTool {

    @Override
    public void run(Args args) throws Exception {
        var flag = new AtomicBoolean();

        var export = args.readArg("export");
        if (export.isPresent()) {
            flag.set(true);
            exportRecords(export.requiredVal("export path must be specified"));
        }

        var importFile = args.readArg("import");
        if (importFile.isPresent()) {
            flag.set(true);
            var imported = importRecords(importFile.requiredVal("import path must be specified"));
            args.getContext().write(JsonUtil.toJson(imported));
        }

        args.readArg("find").ifPresent(a -> {
            flag.set(true);
            args.getContext().write(toPrintStr(findActions(a)));
        });

        args.readArg("add").ifPresent(a -> {
            flag.set(true);
            args.getContext().write(callAdd(a));
        });

        args.readArg("done").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.done);
        });

        args.readArg("cancel").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.cancel);
        });

        args.readArg("todo").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.todo);
        });

        args.readArg("delete").ifPresent(a -> {
            flag.set(true);
            deleteRequestLocalData("/action/"+a.val());
        });

        if (!flag.get()) {
            flag.set(callOthers(args));
        }

        if (!flag.get()) {
            runTui();
        }
    }

    protected void runTui() throws Exception {

    }

    protected Actions findActions(Arg arg) {
        var method = "/action?type=" + currentType().name();
        if (arg.isPresent()) {
            method = method + "&" + arg.val();
        }

        return getRequestLocalData(method, Actions.class);
    }

    private void exportRecords(String path) throws Exception {
        var records = getRequestLocalData("/action/all?type=" + currentType().name(), Actions.class);
        Files.writeString(Path.of(path), JsonUtil.toJson(records), StandardCharsets.UTF_8);
    }

    private Actions importRecords(String path) throws Exception {
        var records = JsonUtil.toPojoList(Files.readAllBytes(Path.of(path)), ActionRecord.class);
        return postRequestLocalData("/action/import", records, Actions.class);
    }

    protected boolean callOthers(Args args) throws Exception {
        return false;
    }

    protected String callAdd(Arg a) {
        var id = "";
        if (a.val().isBlank()) {
            var name = Utils.readLine("name");
            var remark = Utils.readLine("remark");
            id = saveRecord(ActionState.todo, name, remark, null).id();
        } else {
            id = saveRecord(ActionState.todo, a.val(), null, null).id();
        }
        return id;
    }

    protected ActionRecord updateState(ActionRecord record) {
        return postRequestLocalData(
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

    protected ActionRecord saveRecord(ActionState state, String name, String remark, String category) {
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
        return updateState(param);
    }

    protected abstract Type currentType();

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("find", null, "find condition, use url query string", false,
                "state=todo&id=d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("export", null, "export all records of this type to a JSON file", false,
                "./records.json"))
            .arg(new Arg("import", null, "import records from a JSON file", false,
                "./records.json"))
            .arg(new Arg("add", null, "add new item", false,
                "buy something"))
            .arg(new Arg("done", null, "mark item done by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("cancel", null, "mark item cancel by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("todo", null, "mark item todo by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("delete", null, "delete item cancel by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .runnerTypes(Set.of(RunnerType.CLI));
    }

    public static class Actions extends ListData<ActionRecord> {

    }

}
