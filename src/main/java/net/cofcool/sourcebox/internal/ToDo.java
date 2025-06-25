package net.cofcool.sourcebox.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.NoteConfig;
import net.cofcool.sourcebox.internal.api.NoteIndex;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.runner.WebRunner;
import net.cofcool.sourcebox.util.CsvParser;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public class ToDo implements WebTool {

    private String port;


    @Override
    public ToolName name() {
        return ToolName.todo;
    }

    @Override
    public String getPort() {
        return port;
    }

    @Override
    public void run(Args args) throws Exception {
        args.readArg(NoteConfig.PORT_KEY).ifPresent(a -> port = a.val());
        var flag = new AtomicBoolean();

        args.readArg("import").ifPresent(a -> {
            flag.set(true);
            try {
                FileUtils
                    .readLines(new File(a.val()), StandardCharsets.UTF_8)
                    .forEach(s -> {
                        var t =  CsvParser.parseLine(s);
                        var state = "";
                        var remark = "";
                        if (t.size() == 2) {
                            remark = t.get(1);
                        }
                        if (t.size() == 3) {
                            remark = t.get(1);
                            state = t.get(2);
                        }
                        saveTodo(state, t.get(0), remark);
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
                saveTodo("todo", name, remark);
            } else {
                saveTodo("todo", a.val(), null);
            }
        });

        args.readArg("done").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),"done");
        });

        args.readArg("cancel").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),"cancel");
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

    private void updateState(String id, String state) {
        updateState(
            ActionRecord
                .builder()
                .id(id)
                .state(state)
                .build()
        );
    }

    private void saveTodo(String state, String name, String remark) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (StringUtils.isBlank(state)) {
            state = "todo";
        }
        if (StringUtils.isBlank(remark)) {
            remark = null;
        }
        if (!state.equals("todo") && !state.equals("done") && !state.equals("cancel")) {
            throw new IllegalArgumentException("item state error, must be todo or done");
        }
        var param = ActionRecord.builder()
            .name(name)
            .state(state)
            .remark(remark)
            .type(Type.todo.name())
            .start(LocalDateTime.now())
            .end(LocalDateTime.now())
            .build();
        updateState(param);
    }

    public static String toPrintStr(List<ActionRecord> objects) {
        if (objects == null) {
            return "";
        }
        return objects.stream()
            .map(obj ->
                String.join(" | ",
                    obj.id(), obj.state(), obj.name(), Objects.toString(obj.remark(), ""),
                    Utils.formatDatetime(obj.createTime())
                )
            )
            .map(s -> s +"\n" + "-".repeat(s.length()))
            .collect(Collectors.joining("\n"));
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
            .arg(new Arg("port", WebRunner.PORT_VAL + "", "web server listen port", false, null))
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
