package net.cofcool.toolbox.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.EnumSet;
import lombok.CustomLog;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

@CustomLog
public class HtmlDownloader implements Tool {

    private int depth;

    @Override
    public ToolName name() {
        return ToolName.htmlDown;
    }

    @Override
    public void run(Args args) throws Exception {
        var url = args.readArg("url").val();
        var out = args.readArg("out").val();
        depth = Integer.parseInt(args.readArg("depth").val());

        downloadUrl(out, url, depth);
    }

    private void downloadUrl(String folder, String url, int depth) throws IOException {
        if (StringUtils.isBlank(url)) {
            return;
        }

        var doc = Jsoup.connect(url).get();
        var title = doc.title();

        if (depth == this.depth) {
            var dir = Paths.get(folder, title).toFile();
            folder = dir.toString();
            FileUtils.forceMkdir(dir);
            log.info("Create dir {0}", dir);
        }

        File file = Paths.get(folder, title + ".html").toFile();
        if (file.exists()) {
            file = Paths.get(folder, title + RandomStringUtils.randomNumeric(2) + ".html").toFile();
        }
        FileUtils.writeStringToFile(file, doc.outerHtml(), StandardCharsets.UTF_8);
        log.info("Download {0} from url: {1}", file, url);

        var links = doc.select("a[href]");

        depth--;

        if (depth <= 0) {
            log.debug("Stop download when depth is 0");
            return;
        }

        for (Element link : links) {
            var href = link.attr("abs:href");
            try {
                downloadUrl(folder, href, depth);
            } catch (Exception e) {
                log.error("Download " + href, e);
            }
        }
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("url", null, "link string", false, "'{}'"))
            .arg(new Arg("path", null, "link file path", false, "./demo.txt"))
            .arg(new Arg("depth", "1", "link depth", false, null))
            .arg(new Arg("out", "./", "output folder", false, null))
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.WEB));
    }
}
