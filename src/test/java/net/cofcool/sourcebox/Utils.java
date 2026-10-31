package net.cofcool.sourcebox;

import net.cofcool.sourcebox.internal.FileTools;
import org.apache.commons.lang3.RandomUtils;

import java.net.URL;

public class Utils {

    public static String getTestResourcePath(String path) {
        return FileTools.class.getResource(path).toString().substring(5);
    }

    public static String randomPort() {
        return RandomUtils.nextInt(38000, ToolRunner.DEFAULT_PORT) + "";
    }

    public static URL getTestResourceUrlPath(String path) {
        return FileTools.class.getResource(path);
    }
}
