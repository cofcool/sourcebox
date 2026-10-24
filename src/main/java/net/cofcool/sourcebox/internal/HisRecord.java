package net.cofcool.sourcebox.internal;

import lombok.CustomLog;
import lombok.Setter;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.NoteIndex;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.util.Repl;

import java.util.EnumSet;

import static net.cofcool.sourcebox.internal.api.entity.ActionRecord.toPrintStr;

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
        args.readArg("find").ifPresent(find -> {
            var d = findActions(find.val());
            args.getContext().write(toPrintStr(d));
        });
    }

    private Actions findActions(String q) {
        var method = "/action?type=" + Type.record.name();
        if (!q.isBlank()) {
            method += "&" + q;
        }

        return getRequestLocalData(method, Actions.class);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("find", null, "find condition, use url query string", false,
                "id=d5ebeacf9a0aa3319560c7aa78e8751c"))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
    }

    public static class Actions extends ListData<ActionRecord> {

    }

    @Override
    public Class<? extends WebRouter> routerType() {
        return NoteIndex.class;
    }

    @Override
    public boolean supportCommand() {
        return true;
    }
}
