package net.cofcool.sourcebox.internal;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.internal.FileNameFormatter.Formatter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


class FileNameFormatterTest extends BaseTest {

    @TempDir
    File file;

    @Test
    void run() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + "demo.txt"), "test", StandardCharsets.UTF_8);
        instance().run(args.arg("path", path).arg("formatter", FileNameFormatter.Formatter.order.name()));
        Assertions.assertTrue(new File(path + File.separator + "demo-001.txt").exists());
    }

    @Test
    void runWithStart() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + "demo.txt"), "test", StandardCharsets.UTF_8);
        instance().run(args.arg("path", path).arg("formatter", FileNameFormatter.Formatter.order.name()).arg("start",  "2"));
        Assertions.assertTrue(new File(path + File.separator + "demo-002.txt").exists());
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    @Test
    void runWithDeepDirectories() throws Exception {
        var demo1 = new File(String.join(File.separator, this.file.getPath(), "demo1", "demo11"));
        var demo2 = new File(String.join(File.separator, this.file.getPath(), "demo1", "demo12"));
        demo1.mkdirs();
        demo2.mkdirs();
        var path = demo1.getPath();
        var path2 = demo2.getPath();
        FileUtils.writeStringToFile(new File(path + File.separator + "demo.txt"), "test", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(path2 + File.separator + "demo1.txt"), "test", StandardCharsets.UTF_8);

        instance().run(args.arg("path", file.getPath()).arg("formatter", FileNameFormatter.Formatter.order.name()));

        Assertions.assertTrue(new File(path + File.separator + "demo-001.txt").exists());
        Assertions.assertTrue(new File(path2 + File.separator + "demo1-001.txt").exists());
    }

    @Test
    void runWithUrlencoded() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + URLEncoder.encode("%测试demo.txt", StandardCharsets.UTF_8)), "test", StandardCharsets.UTF_8);
        instance().run(args.arg("path", path).arg("formatter", FileNameFormatter.Formatter.urlencoded.name()));
        Assertions.assertTrue(new File(path + File.separator + "%测试demo.txt").exists());
    }

    @Test
    void runWithDate() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + URLEncoder.encode("demo.txt", StandardCharsets.UTF_8)), "test", StandardCharsets.UTF_8);
        instance().run(args.arg("path", path).arg("formatter", FileNameFormatter.Formatter.date.name()));
        Assertions.assertTrue(new File(path + File.separator + String.join("-", "demo", FileNameFormatter.DATE_FORMATTER.format(LocalDate.now()), "001.txt")).exists());
    }

    @Test
    void runWithReplace() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + URLEncoder.encode("demo....txt", StandardCharsets.UTF_8)), "test", StandardCharsets.UTF_8);
        instance().run(args
            .arg("path", path)
            .arg("formatter", Formatter.replace.name())
            .arg("new", "")
            .arg("old", "...")
        );
        Assertions.assertTrue(new File(path + File.separator + "demo.txt").exists());
    }

    @Test
    void runWithReplaceExt() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + "demo.txt"), "test", StandardCharsets.UTF_8);
        instance().run(args
            .arg("path", path)
            .arg("formatter", Formatter.replace.name())
            .arg("new", "-xx")
            .arg("old", ".txt")
        );
        Assertions.assertTrue(new File(path + File.separator + "demo-xx").exists());
    }

    @Test
    void runWithDelete() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + "demo.txt"), "test", StandardCharsets.UTF_8);
        instance().run(args
            .arg("path", path)
            .arg("formatter", Formatter.delete.name())
            .arg("old", "mo")
        );
        Assertions.assertTrue(new File(path + File.separator + "de.txt").exists());
    }

    @Test
    void runWithIgnore() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + URLEncoder.encode("demo.txt", StandardCharsets.UTF_8)), "test", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(path + File.separator + URLEncoder.encode(".demo.txt", StandardCharsets.UTF_8)), "test", StandardCharsets.UTF_8);
        instance().run(args.arg("path", path));
        Assertions.assertTrue(new File(path + File.separator + "demo-001.txt").exists());
        Assertions.assertFalse(new File(path + File.separator + ".demo-002.txt").exists());
    }

    @Test
    void runWithDest() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + URLEncoder.encode("demo.txt", StandardCharsets.UTF_8)), "test", StandardCharsets.UTF_8);
        String dest = path + File.separator + "demo" + File.separator;
        instance().run(
            args
                .arg("path", path)
                .arg("dest", dest)
        );
        Assertions.assertTrue(new File(dest +  "demo-001.txt").exists());
    }

    @Test
    void runWithExpression() throws Exception {
        var path = file.getPath();
        System.out.println(path);
        FileUtils.writeStringToFile(new File(path + File.separator + URLEncoder.encode("demo.txt", StandardCharsets.UTF_8)), "test", StandardCharsets.UTF_8);
        instance().run(
            args
                .arg("formatter", Formatter.expression.name())
                .arg("path", path)
                .arg("expression", "{idx}{suffix}")
        );
        Assertions.assertTrue(new File(path + File.separator  +  "001.txt").exists());
    }

    @Test
    void printInnerHelp() {
        System.out.println(Arrays.toString(Formatter.values()));
    }

    @Override
    protected Tool instance() {
        return new FileNameFormatter();
    }

    @Override
    protected void init() throws Exception {
        super.init();
        args.arg("formatter", Formatter.order.name());
    }
}