package net.cofcool.toolbox.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlDownloaderTest extends BaseTest {

    @TempDir
    File file;

    @Test
    void run() throws Exception {
        instance().run(args
            .arg("url", "https://www.bing.com")
            .arg("out", file.getAbsolutePath())
        );
        var files = file.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0);
    }

    @Test
    void runWithUrlFile() throws Exception {
        var urlFile = Paths.get(file.getAbsolutePath(), "urlFile.txt");
        Files.writeString(urlFile, "https://www.bing.com");
        instance().run(args
            .arg("urlFile",urlFile.toString())
            .arg("out", file.getAbsolutePath())
        );
        var files = file.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0);
    }

    @Override
    protected Tool instance() {
        return new HtmlDownloader();
    }
}