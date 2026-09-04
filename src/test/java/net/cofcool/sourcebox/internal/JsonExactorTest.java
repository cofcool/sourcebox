package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonExactorTest extends BaseTest {

    private static final String JSON_STR = """
        {"key": "val", "key1": "val1", "key3": [{ "k3": "val3"}]}
        """;

    @TempDir
    Path dir;

    @Test
    void runWithJson() throws Exception {
        instance().run(args.arg("in", JSON_STR).arg("jsonpath", "$.key3.[*].k3"));
    }

    @Test
    void runWithPath() throws Exception {
        var file = dir.resolve("jsonRunWithPath.json").toFile();
        FileUtils.write(file, JSON_STR);
        instance().run(args
                .arg("in", file.getPath())
                .arg("jsonpath", "$.key")
        );
    }

    @Override
    protected Tool instance() {
        return new JsonExactor();
    }
}