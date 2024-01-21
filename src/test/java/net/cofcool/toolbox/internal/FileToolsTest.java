package net.cofcool.toolbox.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileToolsTest extends BaseTest {

    static final String INPUT_PATH = Utils.getTestResourcePath("/fileTools.txt");
    static final String SAMPLE_PATH = Utils.getTestResourcePath("/fileToolsSample.txt");

    @TempDir
    Path tmpDir;

    @Override
    protected Tool instance() {
        return new FileTools();
    }

    @Test
    void runWithSplit() throws Exception {
        instance().run(args.arg("util", "split")
            .arg("path", INPUT_PATH)
            .arg("splitIdx", "2")
        );
    }

    @Test
    void runWithCount() throws Exception {
        instance().run(args
            .arg("util", "count")
            .arg("path", INPUT_PATH)
            .arg("samplePath", SAMPLE_PATH)
            .arg("threadSize", "2")
        );
    }

    @Test
    void runWithCount1() throws Exception {
        String output = tmpDir.resolve("runWithCount1.txt").toString();
        instance().run(args
            .arg("util", "count")
            .arg("path", INPUT_PATH)
            .arg("out", output)
            .arg("samplePath", SAMPLE_PATH)
            .arg("threadSize", "2")
        );
        Assertions.assertTrue(FileUtils.readFileToString(new File(output), StandardCharsets.UTF_8).startsWith("道"));
    }

}