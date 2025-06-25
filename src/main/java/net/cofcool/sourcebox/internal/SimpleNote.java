package net.cofcool.sourcebox.internal;

import java.util.EnumSet;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.NoteConfig;
import net.cofcool.sourcebox.internal.api.NoteIndex;

@CustomLog
public class SimpleNote implements WebTool {

    @Override
    public ToolName name() {
        return ToolName.note;
    }

    @Override
    public void run(Args args) throws Exception {

    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg(NoteConfig.PORT_KEY, NoteConfig.PORT_VAL + "", "note web server listen port", false, null))
            .runnerTypes(EnumSet.allOf(RunnerType.class))
            .alias("note", name(), NoteConfig.PATH_KEY,  null);
    }

    @Override
    public Class<? extends WebRouter> routerType() {
        return NoteIndex.class;
    }
}
