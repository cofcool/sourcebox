package net.cofcool.sourcebox.runner;

import lombok.CustomLog;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import net.cofcool.sourcebox.util.JsonUtil;

import java.util.LinkedHashMap;

@CustomLog
public class GUIRunner implements ToolRunner {

    @Override
    public boolean run(Args args) throws Exception {
        var tools = App.supportTools(RunnerType.GUI);

        var ret = new LinkedHashMap<>();

        for (Tool tool : tools) {
            ret.put(tool.name(), tool.config());
        }

        var console = new ConsoleToolContext();
        console.write(JsonUtil.toJson(ret));

        return true;
    }

}
