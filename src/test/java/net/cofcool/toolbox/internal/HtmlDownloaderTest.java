package net.cofcool.toolbox.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.jsoup.Jsoup;
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
    void runWithClean() throws Exception {
        instance().run(args
            .arg("url", "https://www.bing.com")
            .arg("out", file.getAbsolutePath())
            .arg("clean", "true")
        );
        var files = file.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0);

        File[] listFiles = files[0].listFiles();
        assertNotNull(listFiles);
        assertTrue(listFiles.length > 0);
        assertEquals(0, Jsoup.parse(listFiles[0]).getElementsByTag("script").size());
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