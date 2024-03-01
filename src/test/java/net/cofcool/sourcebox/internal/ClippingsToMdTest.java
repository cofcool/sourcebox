package net.cofcool.sourcebox.internal;

import java.nio.file.Path;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.ClippingsToMd.ClipType;
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