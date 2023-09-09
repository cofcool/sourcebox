package net.cofcool.toolbox.logging;


public interface Logger {

    void error(Object val);
    void error(String msg, Throwable throwable);

    void info(String val, Object... arg);

    void debug(Object val);

}
