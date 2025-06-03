package net.cofcool.sourcebox.internal;

import java.util.EnumSet;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.simplenote.NoteConfig;
import net.cofcool.sourcebox.internal.simplenote.NoteIndex;

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
            .arg(new Arg(NoteConfig.FILE_KEY, NoteConfig.FILE_NAME, "note filename", false, null))
            .arg(new Arg(NoteConfig.PATH_KEY, NoteConfig.PATH_VAL, "note file path", false, null))
            .arg(new Arg(NoteConfig.PORT_KEY, NoteConfig.PORT_VAL + "", "note web server listen port", false, null))
            .runnerTypes(EnumSet.allOf(RunnerType.class))
            .alias("note", name(), NoteConfig.PATH_KEY,  null);
    }

    @Override
    public Args defaultConfig(String globalDir) {
        return new Args()
            .arg(NoteConfig.PATH_KEY, globalDir);
    }

    @Override
    public Class<? extends WebRouter> routerType() {
        return NoteIndex.class;
    }
}
