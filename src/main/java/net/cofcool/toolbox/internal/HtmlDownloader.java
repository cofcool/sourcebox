package net.cofcool.toolbox.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.CustomLog;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
    private Set<OutputType> outputTypes = EnumSet.of(OutputType.html);
    private Proxy proxy;
    private String filter;

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
        outputTypes = Arrays
            .stream(args.readArg("outType").val().split(","))
            .map(OutputType::valueOf)
            .collect(Collectors.toSet());
        var img = args.readArg("img").val();
        filter = args.readArg("filter").getVal().orElse(null);

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

        for (OutputType type : outputTypes) {
            type.finished();
        }
    }

    private Connection getConnection() {
        if (connection == null) {
            connection = Jsoup.newSession().proxy(proxy);
        }

        return connection;
    }

    private static void toMarkdown(Document body, String folder, String title) throws IOException {
        var md = new ArrayList<String>();
        convertTagToMd(body.body(), md);
        FileUtils.writeLines(Paths.get(folder, title + ".md").toFile(), md);
    }

    private static void toPlainText(Document body, String folder, String title) throws IOException {
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

        if (clean) {
            doc.getElementsByTag("script").remove();
            doc.getElementsByTag("style").remove();
            doc.getElementsByTag("meta").remove();
        }

        downloadImages(doc.getElementsByTag("img"), folder, expression);

        if (filter == null || title.contains(filter)) {
            for (OutputType type : outputTypes) {
                type.applyOutput(doc, folder, title);
                log.info("Save {0} file to {1} from <<{2}>>: {3}", type, folder, title, url);
            }
        }

        history.add(url);

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
        boolean removeImg = "false".equals(expression);
        if (!removeImg) {
            folder = FilenameUtils.concat(folder, IMGS_FOLDER);
            FileUtils.forceMkdir(new File(folder));
        }

        var idx = 0;
        for (Element img : imgs) {
            if (removeImg) {
                img.remove();
                continue;
            }
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
            .arg(new Arg("outType", OutputType.html.name(), "output file type: " + Arrays.toString(OutputType.values()), false, null))
            .arg(new Arg("filter", null, "title filter", false, "demo"))
            .arg(new Arg("depth", "1", "link depth", false, null))
            .arg(new Arg("proxy", null, "request proxy", false, "127.0.0.1:8087"))
            .arg(new Arg("out", "./", "output folder", false, null))
            .arg(new Arg("clean", "false", "remove css or javascript", false, null))
            .runnerTypes(EnumSet.of(RunnerType.CLI));
    }

    private static String checkText(Element e, String out) {
        return StringUtils.isEmpty(e.text()) ? null :  out;
    }

    enum OutputType {
        html((d, f, t) -> {
            File file = Paths.get(f, t + ".html").toFile();
            if (file.exists()) {
                file = Paths.get(f, t + RandomStringUtils.randomNumeric(2) + ".html").toFile();
            }
            FileUtils.writeStringToFile(file, d.outerHtml(), StandardCharsets.UTF_8);
        }),
        txt(HtmlDownloader::toPlainText),
        markdown(HtmlDownloader::toMarkdown),
        epub(new ToEpub());

        private final Output output;

        OutputType(Output output) {
            this.output = output;
        }

        private void applyOutput(Document body, String folder, String title) throws IOException {
            output.write(body, folder, title);
        }

        private void finished() {
            output.finished();
        }

    }

    private interface Output {

        void write(Document body, String folder, String title) throws IOException;

        default void finished() {

        }
    }

    // calibre formatter
    // epub 2
    private static class ToEpub implements Output {

        private static final byte[] MINE_TYPE = "application/epub+zip".getBytes(StandardCharsets.UTF_8);

        private static final byte[] CONTAINER = """
            <?xml version="1.0"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
               <rootfiles>
                  <rootfile full-path="metadata.opf" media-type="application/oebps-package+xml"/>
               </rootfiles>
            </container>
            """.getBytes(StandardCharsets.UTF_8);

        private static final String METADATA = """
            <?xml version='1.0' encoding='utf-8'?>
            <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="uuid_id" version="2.0">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
                <dc:title>%s</dc:title>
                <dc:creator opf:file-as="Unknown" opf:role="aut">HtmlDownloader</dc:creator>
                <dc:contributor opf:file-as="Unknown" opf:role="bkp">HtmlDownloader</dc:contributor>
                <dc:language>zho</dc:language>
              </metadata>
              <manifest>
                <item href="start.xhtml" id="start" media-type="application/xhtml+xml"/>
                <item href="toc.ncx" id="ncx" media-type="application/x-dtbncx+xml"/>
              </manifest>
              <spine toc="ncx">
                <itemref idref="start"/>
              </spine>
              <guide/>
            </package>
            """;

        private static final String TOC = """
            <?xml version='1.0' encoding='utf-8'?>
            <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1" xml:lang="zh">
              <head>
                <meta name="dtb:depth" content="2"/>
                <meta name="dtb:generator" content="HtmlDownloader"/>
                <meta name="dtb:totalPageCount" content="0"/>
                <meta name="dtb:maxPageNumber" content="0"/>
              </head>
              <docTitle>
                <text>%s</text>
              </docTitle>
              <navMap>
                <navPoint id="num_1" playOrder="1">
                  <navLabel>
                    <text>Start</text>
                  </navLabel>
                  <content src="start.xhtml"/>
                </navPoint>
                <navPoint id="num_2" playOrder="2">
                  <navLabel>
                    <text>%s</text>
                  </navLabel>
                  <content src="start.xhtml#toc_1"/>
                </navPoint>
              </navMap>
            </ncx>
            """;

        private static final String START_HTML = """
            <?xml version='1.0' encoding='utf-8'?>
            <html xmlns="http://www.w3.org/1999/xhtml" lang="zh">
                        
            <head>
              <title id="toc_1">%s</title>
            </head>
                        
            <body>
                        
              <h1>%s</h1>
                        
            </body>
                        
            </html>
            """;

        private ZipOutputStream zos;
        private List<String> files;
        private String title;

        @Override
        public void write(Document body, String folder, String title) throws IOException {
            if (zos == null) {
                zos = new ZipOutputStream(new FileOutputStream(Paths.get(folder, title + ".epub").toFile()));
                files = new ArrayList<>();
                this.title = title;

                var entry = new ZipEntry("mimetype");
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(MINE_TYPE.length);
                entry.setCrc(crc(MINE_TYPE));
                zos.putNextEntry(entry);
                zos.write(MINE_TYPE);

                var container = new ZipEntry("META-INF/container.xml");
                zos.putNextEntry(container);
                zos.write(CONTAINER);

                var start = new ZipEntry("start.xhtml");
                zos.putNextEntry(start);
                zos.write(START_HTML.formatted(title, title).getBytes(StandardCharsets.UTF_8));

                log.debug("Init epub file");
            }

            var cloned = body.clone();

            var titleTag = cloned.getElementsByTag("title");
            if (titleTag.isEmpty()) {
                titleTag.add(new Element("title").text(title));
            }
            titleTag.attr("id", "toc_1");

            zos.putNextEntry(new ZipEntry(title + ".html"));
            zos.write(cloned.outerHtml().getBytes(StandardCharsets.UTF_8));
            files.add(title);
            log.debug("Write {0}", title);
        }

        private long crc(byte[] data) {
            var crc = new CRC32();
            crc.update(data);
            return crc.getValue();
        }

        @Override
        public void finished() {
            var mata = METADATA.formatted(title);
            try {
                var factory = DocumentBuilderFactory.newInstance().newDocumentBuilder();

                var doc = factory.parse(new ByteArrayInputStream(mata.getBytes(StandardCharsets.UTF_8)));
                var manifest = doc.getElementsByTagName("manifest").item(0);
                var spine = doc.getElementsByTagName("spine").item(0);

                var toc = factory.parse(new ByteArrayInputStream(TOC.formatted(title, title).getBytes(
                    StandardCharsets.UTF_8)));
                var navMap = toc.getElementsByTagName("navMap").item(0);

                for (int i = 0; i < files.size(); i++) {
                    var item = doc.createElement("item");
                    item.setAttribute("media-type", "application/xhtml+xml");
                    item.setAttribute("id", "id" + i);
                    item.setAttribute("href", title + ".html");
                    manifest.appendChild(item);


                    var itemref = doc.createElement("itemref");
                    itemref.setAttribute("idref", "id" + i);
                    spine.appendChild(itemref);

                    var navPoint = toc.createElement("navPoint");
                    navPoint.setAttribute("id", "num_" + (i + 3));
                    navPoint.setAttribute("playOrder", (i + 3) + "");
                    var navLabel = toc.createElement("navLabel");
                    navLabel.appendChild(toc.createTextNode(title));
                    navPoint.appendChild(navLabel);
                    var content = toc.createElement("content");
                    content.setAttribute("src", title + ".html" + "#toc_1");
                    navPoint.appendChild(content);
                    navMap.appendChild(navPoint);
                }

                var transformer = TransformerFactory.newInstance().newTransformer();

                var outputStream = new ByteArrayOutputStream();
                transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
                zos.putNextEntry(new ZipEntry("metadata.opf"));
                zos.write(outputStream.toByteArray());

                var tocStream = new ByteArrayOutputStream();
                transformer.transform(new DOMSource(toc), new StreamResult(tocStream));
                zos.putNextEntry(new ZipEntry("toc.ncx"));
                zos.write(tocStream.toByteArray());

                IOUtils.closeQuietly(zos);
            } catch (Exception e) {
                log.error("Save epub file error",  e);
            }
            zos = null;
            files =  null;
            title = null;
        }
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
