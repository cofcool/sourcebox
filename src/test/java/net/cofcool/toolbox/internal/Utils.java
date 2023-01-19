package net.cofcool.toolbox.internal;

public class Utils {

    static String getTestResourcePath(String path) {
        return SplitKindleClippings.class.getResource(path).toString().substring(5);
    }
}
