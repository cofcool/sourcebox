package net.cofcool.toolbox.logging;

public record ConsoleLogger(Class<?> clazz) implements Logger {

    @Override
    public void error(Object val) {
        if (val instanceof Throwable) {
            if (LoggerFactory.DEBUG) {
                ((Throwable) val).printStackTrace();
                return;
            } else {
                val = ((Throwable) val).getMessage();
            }
        }
        System.err.println("ERROR: " + val);
    }

    @Override
    public void error(String msg,Throwable throwable) {
        error(msg);
        error(throwable);
    }

    @Override
    public void info(String val, Object... arg) {
        System.out.println(val);
    }

    @Override
    public void debug(Object val) {
        if (LoggerFactory.DEBUG) {
            System.out.println(val);
        }
    }
}
