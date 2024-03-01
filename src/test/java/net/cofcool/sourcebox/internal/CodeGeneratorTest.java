package net.cofcool.sourcebox.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.CodeGenerator.Config;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodeGeneratorTest extends BaseTest {

    @TempDir
    File file;

    static final Config config = new Config("com.example", Set.of("Demo", "DemoUser"));

    @Test
    void run() throws Exception {
        var configPath = FileUtils.getFile(file, "cfg.json");
        FileUtils.writeStringToFile(configPath, JsonUtil.toJson(config), StandardCharsets.UTF_8);
        instance().run(args
            .arg("out", "./target/code-generate")
            .arg("config", configPath.getPath())
        );
        Assertions.assertTrue(new File("./target/code-generate/com/example/DemoController.java").exists());
    }
    @Test
    @Disabled
    void runWithTemplate() throws Exception {
        var configPath = FileUtils.getFile(file, "cfg.json");
        FileUtils.writeStringToFile(configPath, JsonUtil.toJson(config), StandardCharsets.UTF_8);
        instance().run(args
            .arg("out", "./target/code-generate")
            .arg("template", Utils.getTestResourcePath("/DemoTemplate.class"))
            .arg("config", configPath.getPath())
        );
        Assertions.assertTrue(new File("./target/code-generate/com/example/DemoService.java").exists());
    }

    @Override
    protected Tool instance() {
        return new CodeGenerator();
    }
}