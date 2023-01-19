package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class SplitKindleClippingsTest {

    @Test
    void run() throws Exception {
        new SplitKindleClippings().run(new Tool.Args().arg("path", Utils.getTestResourcePath("/splitKindleClippingsTest.txt")).arg("out", "./target/splitKindleClippingsTest.md"));
    }

}