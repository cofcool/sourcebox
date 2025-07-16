package net.cofcool.sourcebox.internal;

import static net.cofcool.sourcebox.internal.api.entity.ActionRecord.toPrintStr;

import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.Setter;
import lombok.SneakyThrows;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.NoteIndex;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.Comment;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.util.Repl;
import net.cofcool.sourcebox.util.Repl.Command;
import net.cofcool.sourcebox.util.Repl.CommandRegistry;
import net.cofcool.sourcebox.util.Repl.Inputs;
import net.cofcool.sourcebox.util.Repl.PromptStore;
import net.cofcool.sourcebox.util.Utils;

@CustomLog
public class HisRecord implements WebTool {

    @Setter
    private Repl repl;

    public Repl getRepl() {
        if (repl == null) {
            repl = new Repl();
        }
        return repl;
    }

    @Override
    public ToolName name() {
        return ToolName.hisRecord;
    }

    @Override
    public void run(Args args) throws Exception {
        var flag = new AtomicBoolean();

        args.readArg("done").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.done);
        });

        args.readArg("cancel").ifPresent(a -> {
            flag.set(true);
            updateState(a.val(),ActionState.cancel);
        });

        args.readArg("del").ifPresent(a -> {
            flag.set(true);
            new DeleteCommand().execute(new Inputs().add("id", a.val()), null);
        });

        args.readArg("find").ifPresent(find -> {
            var d = findActions(find.val());
            args.getContext().write(toPrintStr(d));

            flag.set(true);
        });

        args.readArg("comment").ifPresent(a -> {
            flag.set(true);
            var i = a.val().indexOf(":");
            if (i > 0) {
                var id = a.val().substring(0, i);
                var comment = a.val().substring(i+1);
                new CommentCommand().request(new Inputs().add("id", id).add("content", comment));
            } else {
                throw new IllegalArgumentException(a.val() + " format error");
            }
        });

        if (!flag.get()) {
            var cr = new CommandRegistry();
            cr.register(new AddCommand());
            cr.register(new DeleteCommand());
            cr.register(new FindCommand());
            cr.register(new CommentCommand());
            getRepl().launch(cr);
        }
    }

    private Actions findActions(String q) {
        var method = "/action?type=" + Type.record.name();
        if (!q.isBlank()) {
            method += "&" + q;
        }

        return getRequestLocalData(method, Actions.class);
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
                .type(Type.record.name())
                .state(state.name())
                .build()
        );
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("find", null, "find condition, use url query string", false,
                "id=d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("done", null, "mark item done by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("cancel", null, "mark item cancel by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("del", null, "delete item by item id", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c"))
            .arg(new Arg("comment", null, "add comment to this record", false,
                "d5ebeacf9a0aa3319560c7aa78e8751c:add comment"));
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

    class CommentCommand implements Command {

        @Override
        public String name() {
            return "comment";
        }

        @Override
        public void prompt(PromptStore store) {
            store.record("content", null)
                .record("id", null);
        }

        @Override
        public void execute(Inputs inputs, Writer writer) {
            request(inputs);
        }

        public Comment request(Inputs inputs) {
            return postRequestLocalData("/action/comment/" + inputs.getString("id"),
                Comment.builder().content(inputs.getString("content")).build(),
                Comment.class
            );
        }
    }

    class FindCommand implements Command {

        @Override
        public String name() {
            return "find";
        }

        @Override
        public void prompt(PromptStore store) {
            store
                .record("name", null)
                .record("state", null);
        }

        @SneakyThrows
        @Override
        public void execute(Inputs inputs, Writer writer) {
            var str = inputs.all().entrySet().stream().map(e ->
                e.getKey() + "=" + URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8)
            ).collect(Collectors.joining("&"));
            writer.write(toPrintStr(findActions(str)));
        }
    }

    class DeleteCommand implements Command {

        @Override
        public String name() {
            return "del";
        }

        @Override
        public void prompt(PromptStore store) {
            store.record("id", null);
        }

        @Override
        public void execute(Inputs inputs, Writer writer) {
            var id = inputs.getString();
            if (deleteRequestLocalData("/action/"+ id)) {
                log.info("Delete {0}", id);
            }
        }
    }

    class AddCommand implements Command {

        @Override
        public String name() {
            return "add";
        }

        @Override
        public void prompt(PromptStore store) {
            store
                .record("name", null)
                .record("device", null)
                .record("category", null)
                .record("state", null)
                .record("labels", "enter label(separate by commas)")
                .record("start", null)
                .record("remark", null)
            ;
        }

        @Override
        public void execute(Inputs inputs, Writer writer) {
            if (inputs.getString("state").isEmpty()) {
                inputs.add("state", ActionState.todo.name());
            }
            if (inputs.getString("start").isEmpty()) {
                inputs.add("start", Utils.formatDatetime(LocalDateTime.now()));
            }
            inputs.add("type", Type.record.name());
            var param = inputs.toObj(ActionRecord.class);
            postRequestLocalData("/action", param, ActionRecord.class);
        }
    }

}
