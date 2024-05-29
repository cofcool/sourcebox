package net.cofcool.sourcebox.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JsonFormatterTest extends BaseTest {

    public static String JSON_STR = """
        {"key": "val", "key1": "val1"}
        """;
    public static String JSON_LINE = """
        key1
        {"key": "val", "key1": "val1"}
        """;

    @TempDir
    Path dir;

    @Test
    void runWithJson() throws Exception {
        instance().run(args.arg("json", JSON_STR));
    }

    @Test
    void runWithPrettyJson() throws Exception {
        instance().run(args.arg("json", JSON_STR). arg("pretty", "true"));
    }

    @Test
    void runWithPrettyJson1() throws Exception {
        instance().run(args.arg("json", JSON_STR). arg("pretty", "false"));
    }

    @Test
    void runWithPath() throws Exception {
        var file = dir.resolve("jsonRunWithPath.json").toFile();
        FileUtils.write(file, JSON_STR);
        instance().run(args.arg("path", file.getPath()));
        assertTrue(FileUtils.readLines(file, StandardCharsets.UTF_8).size() > 1);
    }

    @Test
    void runWithJsonLine() throws Exception {
        var file = dir.resolve("jsonRunWithPath.json").toFile();
        FileUtils.write(file, JSON_LINE);
        instance().run(args.arg("path", file.getPath()).arg("jsonl", "idline"));
        assertTrue(FileUtils.readLines(file, StandardCharsets.UTF_8).size() > 1);
    }

    @Override
    protected Tool instance() {
        return new JsonFormatter();
    }
}