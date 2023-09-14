package net.cofcool.toolbox.internal;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    private boolean clean;
    private Proxy proxy;

    @Override
    public ToolName name() {
        return ToolName.htmlDown;
    }

    @Override
    public void run(Args args) throws Exception {
        var urls = new ArrayList<String>();

        args.readArg("urlFile").ifPresent(a -> {
            try {
                urls.addAll(FileUtils.readLines(new File(a.val()), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Read url file error", e);
            }
        });
        args.readArg("url").ifPresent(a -> urls.add(a.val()));
        clean = args.readArg("clean").test(Boolean::parseBoolean);

        if (urls.isEmpty()) {
            throw new IllegalArgumentException("Do not find any url");
        }

        var out = args.readArg("out").val();
        args.readArg("proxy").ifPresent(a -> {
            var p = a.val().split(":");
            proxy = new Proxy(Type.HTTP, new InetSocketAddress(p[0], Integer.parseInt(p[1])));
            log.debug("Enable {0}", proxy);
        });
        depth = Integer.parseInt(args.readArg("depth").val());

        for (String url : urls) {
            downloadUrl(out, url, depth);
        }
    }

    private void downloadUrl(String folder, String url, int depth) throws IOException {
        if (StringUtils.isBlank(url)) {
            return;
        }

        var doc = Jsoup.connect(url).proxy(proxy).get();
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

        if (clean) {
            doc.getElementsByTag("script").remove();
            doc.getElementsByTag("style").remove();
            doc.getElementsByTag("meta").remove();
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
            .arg(new Arg("urlFile", null, "link file path", false, "./demo.txt"))
            .arg(new Arg("depth", "1", "link depth", false, null))
            .arg(new Arg("proxy", null, "request proxy", false, "127.0.0.1:8087"))
            .arg(new Arg("out", "./", "output folder", false, null))
            .arg(new Arg("clean", "false", "remove css or javascript", false, null))
            .runnerTypes(EnumSet.of(RunnerType.CLI));
    }
}
