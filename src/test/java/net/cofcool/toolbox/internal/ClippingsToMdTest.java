package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.internal.ClippingsToMd.ClipType;
import org.junit.jupiter.api.Test;

class ClippingsToMdTest extends BaseTest {

    @Test
    void run() throws Exception {
        instance().run(args.arg("path", Utils.getTestResourcePath("/clippingsKindle.txt")).arg("out", "./target/clippingsToMdWithKindle.md"));
    }

    @Test
    void runWithMrexpt() throws Exception {
        instance().run(
            args.arg("path", Utils.getTestResourcePath("/clippingsMrexpt.mrexpt"))
                .arg("out", "./target/clippingsToMdWithMrexpt.md")
                .arg("type", ClipType.mrexpt.name())
        );
    }

    @Override
    protected Tool instance() {
        return new ClippingsToMd();
    }
}