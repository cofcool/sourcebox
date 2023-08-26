package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.RunnerType;

public interface ToolContext {

    ToolContext write(Object val);

    RunnerType runnerType();

}
