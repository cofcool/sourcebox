package net.cofcool.sourcebox.internal;

import static net.cofcool.sourcebox.internal.api.entity.ActionRecord.toPrintStr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.NoteIndex;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.util.CsvParser;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public class ToDo implements WebTool {

    @Override
    public ToolName name() {
        return ToolName.todo;
    }

    @Override
    public void run(Args args) throws Exception {
        var flag = new AtomicBoolean();

        args.readArg("import").ifPresent(a -> {
            flag.set(true);
            try {
                FileUtils
                    .readLines(new File(a.val()), StandardCharsets.UTF_8)
                    .forEach(s -> {
                        var t =  CsvParser.parseLine(s);
                        var state = "todo";
                        var remark = "";
                        if (t.size() == 2) {
                            remark = t.get(1);
                        }
                        if (t.size() == 3) {
                            remark = t.get(1);
                            state = t.get(2);
                        }
                        saveTodo(ActionState.valueOf(state), t.get(0), remark);
                    });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        args.readArg("add").ifPresent(a -> {
            flag.set(true);
            if (a.val().isBlank()) {
                var name = Utils.readLine("name");
                var remark = Utils.readLine("remark");
                saveTodo(ActionState.todo, name, remark);
            } else {
                saveTodo(ActionState.todo, a.val(), null);
            }
        });

        args.readArg("done").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.done);
        });

        args.readArg("cancel").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.cancel);
        });

        var find = args.readArg("find");
        if (!flag.get() || find.isPresent()) {
            var method = "/action?type=todo";
            if (find.isPresent()) {
                method = method + "&" + find.val();
            }

            var d = getRequestLocalData(method, Actions.class);
            args.getContext().write(toPrintStr(d));
        }
    }

    private void updateState(ActionRecord record) {
        postRequestLocalData(
            "/action",
            record,
            ActionRecord.class);
    }

    private void updateState(String id, ActionState state) {
        updateState(
            ActionRecord
                .builder()
                .id(id)
                .state(state.name())
                .build()
        );
    }

    private void saveTodo(ActionState state, String name, String remark) {
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
            .type(Type.todo.name())
            .start(LocalDateTime.now())
            .end(LocalDateTime.now())
            .build();
        updateState(param);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("find", null, "find condition, use url query string", false,
                "state=todo&id=d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("add", null, "add new to do item", false,
                "buy something"))
            .arg(new Arg("done", null, "mark item done by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("cancel", null, "mark item cancel by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("import", null, "csv file path, like: value,remark or value,remark,state ", false, null))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
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
