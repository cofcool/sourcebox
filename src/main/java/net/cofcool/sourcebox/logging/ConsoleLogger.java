package net.cofcool.sourcebox.logging;

import java.text.MessageFormat;

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
        System.out.println(MessageFormat.format(val, arg));
    }

    @Override
    public void debug(String val, Object... args) {
        if (LoggerFactory.DEBUG) {
            System.out.println(MessageFormat.format(val, args));
        }
    }
}
