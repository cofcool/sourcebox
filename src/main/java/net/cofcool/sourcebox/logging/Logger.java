package net.cofcool.sourcebox.logging;


public interface Logger {

    void error(Object val);
    void error(String msg, Throwable throwable);

    void info(String val, Object... arg);

    void debug(String val, Object... args);

}
