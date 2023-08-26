package net.cofcool.toolbox.internal;

public class Utils {

    static String getTestResourcePath(String path) {
        return ClippingsToMd.class.getResource(path).toString().substring(5);
    }
}
