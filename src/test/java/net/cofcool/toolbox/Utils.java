package net.cofcool.toolbox;

import java.net.URL;
import net.cofcool.toolbox.internal.ClippingsToMd;
import net.cofcool.toolbox.runner.WebRunner;
import org.apache.commons.lang3.RandomUtils;

public class Utils {

    public static String getTestResourcePath(String path) {
        return ClippingsToMd.class.getResource(path).toString().substring(5);
    }

    public static String randomPort() {
        return RandomUtils.nextInt(38000, WebRunner.PORT_VAL) + "";
    }

    public static URL getTestResourceUrlPath(String path) {
        return ClippingsToMd.class.getResource(path);
    }
}
