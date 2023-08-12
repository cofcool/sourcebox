package net.cofcool.toolbox.internal;

import java.util.HashMap;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.internal.simplenote.NoteConfig;
import net.cofcool.toolbox.internal.simplenote.NoteVerticle;
import net.cofcool.toolbox.util.JsonUtil;

public class SimpleNote implements Tool {

    @Override
    public ToolName name() {
        return ToolName.note;
    }

    @Override
    public void run(Args args) throws Exception {
        var json = new HashMap<String, Object>();
        args.getArgVal(NoteConfig.PORT_KEY).ifPresent(k -> json.put(NoteConfig.PORT_KEY, Integer.valueOf(k)));
        args.getArgVal(NoteConfig.PATH_KEY).ifPresent(k -> json.put(NoteConfig.PATH_KEY, k));
        args.getArgVal(NoteConfig.FILE_KEY).ifPresent(k -> json.put(NoteConfig.FILE_KEY, k));
        NoteVerticle.start(JsonUtil.toJson(json));
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg(NoteConfig.FILE_KEY, NoteConfig.FILE_NAME, "note filename", false, null))
            .arg(new Arg(NoteConfig.PATH_KEY, NoteConfig.PATH_VAL, "note file path", false, null))
            .arg(new Arg(NoteConfig.PORT_KEY, NoteConfig.PORT_VAL + "", "note web server listen port", false, null))
            .alias("note", name(), NoteConfig.PATH_KEY,  null);
    }
}
