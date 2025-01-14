package net.cofcool.sourcebox.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.HtmlDownloader.OutputType;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlDownloaderTest extends BaseTest {

    @TempDir
    File file;

    String url = Utils.getTestResourceUrlPath("/htmlDownloaderTest.html").toString();

    @Test
    void run() throws Exception {
        try {
            instance().run(args
                .arg("url", "https://www.bing.com")
                .arg("out", file.getAbsolutePath())
            );
            var files = file.listFiles();
            assertNotNull(files);
            assertTrue(files.length > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void runWithFile() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", "./target/HtmlDownloaderTest")
            .arg("img", "Multimedia_and_embedding")
        );
        File download = new File("./target/HtmlDownloaderTest/HtmlDownloaderTest/imgs");
        assertTrue(download.listFiles().length > 0);
    }

    @Test
    void runWithFilter() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", new File(file, "runWithFilter").getAbsolutePath())
            .arg("filter", "test")
        );
        assertEquals(0,
            Paths.get(file.getAbsolutePath(), "runWithFilter", "HtmlDownloaderTest")
                .toFile()
                .listFiles().length
        );
    }

    @Test
    void runWithimg() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", "./target/HtmlDownloaderTest")
            .arg("img", "false")
        );
    }

    @Test
    void runWithMd() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", "./target/HtmlDownloaderTest")
            .arg("outType", OutputType.markdown.name())
        );
    }

    @Test
    void runWithTxt() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", "./target/HtmlDownloaderTest")
            .arg("outType", OutputType.txt.name())
        );
    }

    @Test
    void runWithReplace() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", "./target/HtmlDownloaderTest")
            .arg("replace", "<img.+*>")
            .arg("outType", OutputType.html.name())
        );
    }

    @Test
    void testParseExp() throws Exception {
        var a = HtmlDownloader.parseExp("tag:a,attr:ref=1&ref1=2;tag:b,attr:ref=1&ref1=2;");
        System.out.println(a);
    }

    @Test
    void runWithCleanexp() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", "./target/HtmlDownloaderTest")
            .arg("cleanexp", "img")
            .arg("outType", OutputType.html.name())
        );
    }

    @Test
    void runWithEpub() throws Exception {
        instance().run(args
            .arg("url", url)
            .arg("out", "./target/HtmlDownloaderTest")
            .arg("outType", OutputType.epub.name())
        );
    }

    @Test
    void runWithClean() throws Exception {
        instance().run(args
            .arg("url", url)
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
        Files.writeString(urlFile, url);
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