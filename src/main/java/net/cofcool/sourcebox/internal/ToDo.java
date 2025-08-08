package net.cofcool.sourcebox.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.util.CsvParser;
import org.apache.commons.io.FileUtils;

@CustomLog
public class ToDo extends BaseRecord {

    @Override
    public ToolName name() {
        return ToolName.todo;
    }

    @Override
    protected Type currentType() {
        return Type.todo;
    }

    @Override
    protected boolean callOthers(Args args) throws Exception {
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
                        saveRecord(ActionState.valueOf(state), t.get(0), remark, null);
                    });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return flag.get();
    }

    @Override
    public Args config() {
        return super.config()
            .arg(new Arg("import", null, "csv file path, like: value,remark or value,remark,state ", false, null))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
    }

}
