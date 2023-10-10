package net.cofcool.toolbox.internal;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

@CustomLog
public class HtmlDownloader implements Tool {

    private static final Map<String, Function<Element, String>> tagMap = new HashMap<>();

    private static final String IMGS_FOLDER = "imgs";
    private final Set<String> history = new HashSet<>();

    private int depth;
    private boolean clean;
    private boolean toMd;
    private boolean toTxt;
    private Proxy proxy;

    private Connection connection;

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
        toMd = args.readArg("md").test(Boolean::parseBoolean);
        toTxt = args.readArg("txt").test(Boolean::parseBoolean);
        var img = args.readArg("img").val();

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
            downloadUrl(out, url, depth, img);
        }

        history.clear();
    }

    private Connection getConnection() {
        if (connection == null) {
            connection = Jsoup.newSession().proxy(proxy);
        }

        return connection;
    }

    private void toMarkdown(Element element, String folder, String title) throws IOException {
        var md = new ArrayList<String>();
        convertTagToMd(element, md);
        FileUtils.writeLines(Paths.get(folder, title + ".md").toFile(), md);
    }

    private void toPlainText(Document body, String folder, String title) throws IOException {
        var cleaner = new Cleaner(Safelist.none());
        FileUtils.writeStringToFile(
            Paths.get(folder, title + ".txt").toFile(),
            cleaner.clean(body).wholeText(),
            StandardCharsets.UTF_8
        );
    }

    private static void convertTagToMd(Element element, ArrayList<String> md) {
        for (Element e : element.children()) {
            if (tagMap.containsKey(e.tagName())) {
                String out = tagMap.get(e.tagName()).apply(e);
                if (!StringUtils.isEmpty(out)) {
                    md.add(out);
                    md.add("\n");
                    continue;
                }
            }
            if (e.childrenSize() > 0) {
                convertTagToMd(e, md);
            }
        }

    }

    private void downloadUrl(String folder, String url, int depth, String expression) throws IOException {
        if (StringUtils.isBlank(url)) {
            return;
        }

        if (history.contains(url)) {
            log.debug("Ignore downloaded {0}", url);
            return;
        }

        Document doc;
        if (url.startsWith("file")) {
            doc = Jsoup.parse(new File(url.substring(5)));
        } else {
            doc = getConnection().url(url).get();
        }

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

        if (!("false".equals(expression))) {
            downloadImages(doc.getElementsByTag("img"), folder, expression);
        }

        FileUtils.writeStringToFile(file, doc.outerHtml(), StandardCharsets.UTF_8);
        log.info("Download {0} from url: {1}", file.getAbsolutePath(), url);
        history.add(url);

        if (toMd) {
            toMarkdown(doc.body(), folder, title);
        }
        if (toTxt) {
            toPlainText(doc, folder, title);
        }

        depth--;

        if (depth <= 0) {
            log.debug("Stop download when depth is 0");
            return;
        }

        var links = doc.select("a[href]");
        for (Element link : links) {
            var href = link.attr("abs:href");
            try {
                downloadUrl(folder, href, depth, "false");
            } catch (Exception e) {
                log.error("Download " + href, e);
            }
        }
    }

    private void downloadImages(Elements imgs, String folder, String expression) throws IOException {
        folder = FilenameUtils.concat(folder, IMGS_FOLDER);
        FileUtils.forceMkdir(new File(folder));

        var idx = 0;
        for (Element img : imgs) {
            var name = img.attr("alt");
            var src = img.attr("abs:src");
            try {
                byte[] imgData;
                if (src.startsWith("http") && src.contains(expression)) {
                    imgData = getConnection().url(src).method(Method.GET).ignoreContentType(true).execute().bodyAsBytes();
                    name = FilenameUtils.getName(src);
                } else if (src.startsWith("data")) {
                    var splitIdx = src.indexOf(",");
                    imgData = Base64.getDecoder().decode(src.substring(splitIdx + 1));
                } else {
                    log.info("Image url {0} is invalid", src);
                    continue;
                }

                idx++;
                name = StringUtils.isBlank(name) ? String.format("%03d", idx) : name;
                File file = Paths.get(folder, name).toFile();
                FileUtils.writeByteArrayToFile(file, imgData);
                img.attr("src", String.join("/", ".", IMGS_FOLDER, name));
                log.info("Download image from {0}", file);
            } catch (Exception e) {
                log.error("Download " + src + " image error", e);
            }
        }
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("url", null, "link string", false, "'{}'"))
            .arg(new Arg("urlFile", null, "link file path", false, "./demo.txt"))
            .arg(new Arg("img", "false", "download images, false or path expression", false, null))
            .arg(new Arg("md", "false", "convert to markdown", false, null))
            .arg(new Arg("txt", "false", "convert to plain text", false, null))
            .arg(new Arg("depth", "1", "link depth", false, null))
            .arg(new Arg("proxy", null, "request proxy", false, "127.0.0.1:8087"))
            .arg(new Arg("out", "./", "output folder", false, null))
            .arg(new Arg("clean", "false", "remove css or javascript", false, null))
            .runnerTypes(EnumSet.of(RunnerType.CLI));
    }

    private static String checkText(Element e, String out) {
        return StringUtils.isEmpty(e.text()) ? null :  out;
    }

    static {
        tagMap.put("h1", a -> "#  " + a.text());
        tagMap.put("h2", a -> "## " + a.text());
        tagMap.put("h3", a -> "### " + a.text());
        tagMap.put("h4", a -> "##### " + a.text());
        tagMap.put("h5", a -> "##### " + a.text());
        tagMap.put("b", a -> "**" + a.text() + "**");
        tagMap.put("p", Element::text);
        tagMap.put("span", Element::text);
        tagMap.put("text", Element::text);
        tagMap.put("code", a -> "```\n" + a.wholeText() + "\n```");
        tagMap.put("a", a -> checkText(a, String.format("[%s](%s)", a.text(), a.attr("href"))));
        tagMap.put("img", a -> String.format("![%s](%s)", a.attr("alt"), a.attr("src")));
        tagMap.put("ul", a -> a.children().stream().map(l -> "* " + l.text()).collect(Collectors.joining("\n")));
        tagMap.put("ol", a -> {
            var str = new StringBuilder();
            for (int i = 1; i <= a.children().size(); i++) {
                str.append(i).append(". ").append(a.child(i - 1).text()).append("\n");
            }
            return str.toString();
        });
    }
}
