package net.cofcool.toolbox.internal;

import java.net.URL;

public class Utils {

    static String getTestResourcePath(String path) {
        return ClippingsToMd.class.getResource(path).toString().substring(5);
    }

    static URL getTestResourceUrlPath(String path) {
        return ClippingsToMd.class.getResource(path);
    }
}
