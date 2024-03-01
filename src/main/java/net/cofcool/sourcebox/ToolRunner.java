package net.cofcool.sourcebox;

import net.cofcool.sourcebox.Tool.Args;

public interface ToolRunner {

    boolean run(Args args) throws Exception;

    default String help() {
        return null;
    }

}
