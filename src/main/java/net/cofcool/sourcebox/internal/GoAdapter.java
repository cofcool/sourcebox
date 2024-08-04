package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;

public class GoAdapter {

    public static final UnsupportedOperationException EXCEPTION = new UnsupportedOperationException(
        "please use go version, no need tool argument, ps: sourcebox.sh task --level 20 ");

    public record Task() implements Tool {

        @Override
        public ToolName name() {
            return ToolName.task;
        }

        @Override
        public void run(Args args) throws Exception {
            throw EXCEPTION;
        }

        @Override
        public Args config() {
            return new Args();
        }
    }

    public record MobileBackup() implements Tool {

        @Override
        public ToolName name() {
            return ToolName.mobileBackup;
        }

        @Override
        public void run(Args args) throws Exception {
            throw EXCEPTION;
        }

        @Override
        public Args config() {
            return new Args();
        }
    }
}
