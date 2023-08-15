package net.cofcool.toolbox;

public interface ToolContext {

    ToolContext write(Object val);

    class ConsoleToolContext implements ToolContext {

        @Override
        public ToolContext write(Object val) {
            System.out.println(val);
            return this;
        }
    }

}
