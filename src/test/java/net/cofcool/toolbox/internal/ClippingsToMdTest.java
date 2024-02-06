package net.cofcool.toolbox.internal;

import java.nio.file.Path;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Utils;
import net.cofcool.toolbox.internal.ClippingsToMd.ClipType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClippingsToMdTest extends BaseTest {

    @TempDir
    Path path;

    @Test
    void run() throws Exception {
        Path path1 = path.resolve("clippingsToMdWithKindle.md");
        instance().run(args.arg("path", Utils.getTestResourcePath("/clippingsKindle.txt")).arg("out", path1.toString()));
        Assertions.assertTrue(path1.toFile().exists());
    }

    @Test
    void runWithMrexpt() throws Exception {
        Path path1 = path.resolve("clippingsToMdWithMrexpt.md");
        instance().run(
            args.arg("path", Utils.getTestResourcePath("/clippingsMrexpt.mrexpt"))
                .arg("out", path1.toString())
                .arg("type", ClipType.mrexpt.name())
        );
        Assertions.assertTrue(path1.toFile().exists());
    }

    @Override
    protected Tool instance() {
        return new ClippingsToMd();
    }
}