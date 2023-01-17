package net.cofcool.toolbox.internal;

import java.net.URL;
import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class SplitKindleClippingsTest {

    @Test
    void run() throws Exception {
        URL resource = SplitKindleClippings.class.getResource("/splitKindleClippingsTest.txt");
        new SplitKindleClippings().run(new Tool.Args().arg("path", resource.toString().substring(5)).arg("out", "./target/splitKindleClippingsTest.md"));
    }

}