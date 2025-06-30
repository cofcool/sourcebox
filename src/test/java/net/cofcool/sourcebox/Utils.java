package net.cofcool.sourcebox;

import java.net.URL;
import net.cofcool.sourcebox.internal.ClippingsToMd;
import org.apache.commons.lang3.RandomUtils;

public class Utils {

    public static String getTestResourcePath(String path) {
        return ClippingsToMd.class.getResource(path).toString().substring(5);
    }

    public static String randomPort() {
        return RandomUtils.nextInt(38000, ToolRunner.DEFAULT_PORT) + "";
    }

    public static URL getTestResourceUrlPath(String path) {
        return ClippingsToMd.class.getResource(path);
    }
}
