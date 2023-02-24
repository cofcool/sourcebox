package net.cofcool.toolbox.internal;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.DEFAULT_BUFFER_SIZE;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpHandlers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import com.sun.net.httpserver.SimpleFileServer.OutputLevel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Logger;
import net.cofcool.toolbox.LoggerFactory;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.IOUtils;

public class DirWebServer implements Tool {

    @Override
    public ToolName name() {
        return ToolName.dirWebServer;
    }

    @Override
    public void run(Args args) throws Exception {
        var port = Integer.parseInt(args.readArg("port").val());
        var rootPath = Path.of(args.readArg("root").val()).toRealPath();
        var server = HttpServer
            .create(
                new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port),
                0,
                "/",
                new DelegateHttpHandler(
                    HttpHandlers.handleOrElse(
                        r -> !r.getRequestURI().getPath().contains("upload"),
                        FileServerHandler.create(rootPath, URLConnection.getFileNameMap()::getContentTypeFor),
                        new FileUploadHandler(rootPath)
                    )
                ),
                SimpleFileServer.createOutputFilter(System.out, OutputLevel.INFO)
            );
        server.start();
        getLogger().info("See http://0.0.0.0:" + port);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("port", "8080", "web server listen port", false, null))
            .arg(new Arg("root", System.getProperty("user.dir"), "web server root directory", false, null));
    }

    private record DelegateHttpHandler(HttpHandler delegate) implements HttpHandler {

        static final Logger log =  LoggerFactory.getLogger(DelegateHttpHandler.class);

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    delegate.handle(exchange);
                } catch (Exception e) {
                    log.error(e);
                    var bytes = e.getMessage().getBytes(UTF_8);
                    exchange.getResponseBody().write(bytes);
                    exchange.sendResponseHeaders(500, bytes.length);
                }
            }
        }

    private static class FileUploadHandler implements HttpHandler {

        private static final String UPLOAD_HTML = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8"/>
            </head>
            <body>
            <form ENCTYPE="multipart/form-data" method="post">
                <input name="file" type="file" multiple/>
                <input type="submit" value="upload"/>
            </form>
            <ul>
            %s
            </ul>
            </body>
            </html>
            """;

        private static final String UPLOAD_FILES = "uploadFiles";

        private final Path root;
        private final Logger logger = LoggerFactory.getLogger(FileUploadHandler.class);

        private FileUploadHandler(Path root) {
            root = root.normalize();
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            assert List.of("GET", "POST").contains(exchange.getRequestMethod());
            try (exchange) {
                if (exchange.getRequestMethod().equals("POST")) {
                    handlePOST(exchange);
                } else {
                    handleGET(exchange);
                }
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void handleGET(HttpExchange exchange) throws IOException {
            var uploadFiles = exchange.getAttribute(UPLOAD_FILES);
            if (uploadFiles == null) {
                uploadFiles = Collections.emptyList();
            }
            var respHdrs = exchange.getResponseHeaders();
            respHdrs.set("Content-Type", "text/html");
            respHdrs.set("Last-Modified", LocalDateTime.now().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.RFC_1123_DATE_TIME));
            var contentBytes = String
                .format(
                    UPLOAD_HTML,
                    ((Collection) uploadFiles).stream().map(a -> "<li>" + a + "</li>").collect(Collectors.joining("\n"))
                )
                .getBytes(UTF_8);
            exchange.sendResponseHeaders(200, contentBytes.length);
            try (InputStream is = new ByteArrayInputStream(contentBytes);
                OutputStream os = exchange.getResponseBody()) {
                is.transferTo(os);
            }
            exchange.setAttribute(UPLOAD_FILES, Collections.emptyList());
        }

        private void handlePOST(HttpExchange exchange) throws IOException {
            var contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null) {
                logger.info("no file to upload");
                return;
            }
            var contentTypes = contentType.split("=");
            if (contentTypes.length < 2) {
                logger.info("Content NOT begin with boundary");
                return;
            }
            var boundary = contentTypes[1];

            var files = new ArrayList<>();
            try(var body = exchange.getRequestBody()) {
                var stream = new MultipartStream(body, boundary.getBytes(ISO_8859_1));
                boolean nextPart = stream.skipPreamble();
                OutputStream output = null;
                while(nextPart) {
                    String header = stream.readHeaders();
                    for (String line : header.split("\r\n")) {
                        if (line.startsWith("Content-Disposition")) {
                            for (String s1 : line.split(";")) {
                                var trim = s1.trim();
                                if (trim.startsWith("filename")) {
                                    var file = trim.split("=")[1].replace("\"", "");
                                    files.add(file);
                                    output = new FileOutputStream(FileSystems.getDefault().getPath(root.toString(), file).toFile());
                                }
                            }
                        }
                    }
                    stream.readBodyData(output);
                    IOUtils.closeQuietly(output);
                    output = null;
                    nextPart = stream.readBoundary();
                }
            } catch (Exception e) {
                logger.error("Write file error", e);
                throw e;
            }

            exchange.setAttribute(UPLOAD_FILES, files);
            exchange.getResponseHeaders().set("Location", "/upload");
            exchange.sendResponseHeaders(302, -1);
        }
    }

    // copy from commons-fileupload
    private static class MultipartStream {

        public static final byte CR = 0x0D;
        public static final byte LF = 0x0A;
        public static final byte DASH = 0x2D;
        public static final int HEADER_PART_SIZE_MAX = 10240;
        protected static final int DEFAULT_BUFSIZE = 4096;
        protected static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};
        protected static final byte[] FIELD_SEPARATOR = {CR, LF};
        protected static final byte[] STREAM_TERMINATOR = {DASH, DASH};
        protected static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};

        private final InputStream input;

        private int boundaryLength;

        private int keepRegion;

        private byte[] boundary;

        private final int bufSize;

        private final byte[] buffer;

        private int head;

        private int tail;


        public MultipartStream(InputStream input, byte[] boundary) {
            if (boundary == null) {
                throw new IllegalArgumentException("boundary may not be null");
            }

            this.input = input;
            this.bufSize = DEFAULT_BUFSIZE;
            this.buffer = new byte[bufSize];

            this.boundaryLength = boundary.length + BOUNDARY_PREFIX.length;
            if (bufSize < this.boundaryLength + 1) {
                throw new IllegalArgumentException(
                    "The buffer size specified for the MultipartStream is too small");
            }
            this.boundary = new byte[this.boundaryLength];
            this.keepRegion = this.boundary.length;

            System.arraycopy(BOUNDARY_PREFIX, 0, this.boundary, 0, BOUNDARY_PREFIX.length);
            System.arraycopy(boundary, 0, this.boundary, BOUNDARY_PREFIX.length, boundary.length);

            head = 0;
            tail = 0;
        }

        public byte readByte() throws IOException {
            if (head == tail) {
                head = 0;
                // Refill.
                tail = input.read(buffer, head, bufSize);
                if (tail == -1) {
                    throw new IOException("No more data is available");
                }
            }
            return buffer[head++];
        }

        public boolean readBoundary() throws MalformedStreamException {
            byte[] marker = new byte[2];
            boolean nextChunk = false;

            head += boundaryLength;
            try {
                marker[0] = readByte();
                if (marker[0] == LF) {
                    return true;
                }

                marker[1] = readByte();
                if (arrayequals(marker, STREAM_TERMINATOR, 2)) {
                    nextChunk = false;
                } else if (arrayequals(marker, FIELD_SEPARATOR, 2)) {
                    nextChunk = true;
                } else {
                    throw new MalformedStreamException("Unexpected characters follow a boundary");
                }
            } catch (IOException e) {
                throw new MalformedStreamException("Stream ended unexpectedly");
            }
            return nextChunk;
        }

        public String readHeaders() throws MalformedStreamException {
            int i = 0;
            byte b;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int size = 0;
            while (i < HEADER_SEPARATOR.length) {
                try {
                    b = readByte();
                } catch (IOException e) {
                    throw new MalformedStreamException("Stream ended unexpectedly");
                }
                if (++size > HEADER_PART_SIZE_MAX) {
                    throw new MalformedStreamException(format(
                        "Header section has more than %s bytes (maybe it is not properly terminated)",
                        HEADER_PART_SIZE_MAX));
                }
                if (b == HEADER_SEPARATOR[i]) {
                    i++;
                } else {
                    i = 0;
                }
                baos.write(b);
            }

            return baos.toString();
        }

        public int readBodyData(OutputStream output)
            throws IOException {
            final InputStream istream = newInputStream();
            return (int) copy(istream, output, new byte[DEFAULT_BUFFER_SIZE]);
        }

        ItemInputStream newInputStream() {
            return new ItemInputStream();
        }

        public int discardBodyData() throws IOException {
            return readBodyData(null);
        }

        public boolean skipPreamble() throws IOException {
            System.arraycopy(boundary, 2, boundary, 0, boundary.length - 2);
            boundaryLength = boundary.length - 2;
            try {
                discardBodyData();
                return readBoundary();
            } finally {
                System.arraycopy(boundary, 0, boundary, 2, boundary.length - 2);
                boundaryLength = boundary.length;
                boundary[0] = CR;
                boundary[1] = LF;
            }
        }

        static long copy(InputStream inputStream, OutputStream outputStream, byte[] buffer)
            throws IOException {
            OutputStream out = outputStream;
            InputStream in = inputStream;
            try {
                long total = 0;
                for (; ; ) {
                    int res = in.read(buffer);
                    if (res == -1) {
                        break;
                    }
                    if (res > 0) {
                        total += res;
                        if (out != null) {
                            out.write(buffer, 0, res);
                        }
                    }
                }
                if (out != null) {
                    out.flush();
                }
                in.close();
                in = null;
                return total;
            } finally {
                IOUtils.closeQuietly(in);
            }
        }


        public static boolean arrayequals(byte[] a, byte[] b, int count) {
            for (int i = 0; i < count; i++) {
                if (a[i] != b[i]) {
                    return false;
                }
            }
            return true;
        }

        protected int findByte(byte value, int pos) {
            for (int i = pos; i < tail; i++) {
                if (buffer[i] == value) {
                    return i;
                }
            }
            return -1;
        }

        protected int findSeparator() {
            int first;
            int match = 0;
            int maxpos = tail - boundaryLength;
            for (first = head; first <= maxpos && match != boundaryLength; first++) {
                first = findByte(boundary[0], first);
                if (first == -1 || first > maxpos) {
                    return -1;
                }
                for (match = 1; match < boundaryLength; match++) {
                    if (buffer[first + match] != boundary[match]) {
                        break;
                    }
                }
            }
            if (match == boundaryLength) {
                return first - 1;
            }
            return -1;
        }

        public static class MalformedStreamException extends IOException {

            private static final long serialVersionUID = 6466926458059796677L;

            public MalformedStreamException(String message) {
                super(message);
            }

        }

        public class ItemInputStream extends InputStream implements Closeable {

            private static final int BYTE_POSITIVE_OFFSET = 256;
            private long total;
            private int pad;
            private int pos;
            private boolean closed;

            ItemInputStream() {
                findSeparator();
            }

            private void findSeparator() {
                pos = MultipartStream.this.findSeparator();
                if (pos == -1) {
                    if (tail - head > keepRegion) {
                        pad = keepRegion;
                    } else {
                        pad = tail - head;
                    }
                }
            }

            @Override
            public int available() throws IOException {
                if (pos == -1) {
                    return tail - head - pad;
                }
                return pos - head;
            }

            @Override
            public int read() throws IOException {
                if (closed) {
                    throw new IOException();
                }
                if (available() == 0 && makeAvailable() == 0) {
                    return -1;
                }
                ++total;
                int b = buffer[head++];
                if (b >= 0) {
                    return b;
                }
                return b + BYTE_POSITIVE_OFFSET;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (closed) {
                    throw new IOException();
                }
                if (len == 0) {
                    return 0;
                }
                int res = available();
                if (res == 0) {
                    res = makeAvailable();
                    if (res == 0) {
                        return -1;
                    }
                }
                res = Math.min(res, len);
                System.arraycopy(buffer, head, b, off, res);
                head += res;
                total += res;
                return res;
            }

            @Override
            public void close() throws IOException {
                if (closed) {
                    return;
                }

                for (; ; ) {
                    int av = available();
                    if (av == 0) {
                        av = makeAvailable();
                        if (av == 0) {
                            break;
                        }
                    }
                    skip(av);
                }
                closed = true;
            }

            @Override
            public long skip(long bytes) throws IOException {
                if (closed) {
                    throw new IOException();
                }
                int av = available();
                if (av == 0) {
                    av = makeAvailable();
                    if (av == 0) {
                        return 0;
                    }
                }
                long res = Math.min(av, bytes);
                head += res;
                return res;
            }

            private int makeAvailable() throws IOException {
                if (pos != -1) {
                    return 0;
                }

                total += tail - head - pad;
                System.arraycopy(buffer, tail - pad, buffer, 0, pad);

                head = 0;
                tail = pad;

                for (; ; ) {
                    int bytesRead = input.read(buffer, tail, bufSize - tail);
                    if (bytesRead == -1) {
                        final String msg = "Stream ended unexpectedly";
                        throw new MalformedStreamException(msg);
                    }
                    tail += bytesRead;

                    findSeparator();
                    int av = available();

                    if (av > 0 || pos != -1) {
                        return av;
                    }
                }
            }

            public boolean isClosed() {
                return closed;
            }
        }
    }

    private static class FileServerHandler implements HttpHandler {

        private static final List<String> SUPPORTED_METHODS = List.of("HEAD", "GET", "POST");
        private static final List<String> UNSUPPORTED_METHODS =
            List.of("CONNECT", "DELETE", "OPTIONS", "PATCH", "PUT", "TRACE");

        private final Path root;
        private final UnaryOperator<String> mimeTable;
        private final Logger logger  = LoggerFactory.getLogger(FileServerHandler.class);

        private FileServerHandler(Path root, UnaryOperator<String> mimeTable) {
            root = root.normalize();

            if (!Files.exists(root))
                throw new IllegalArgumentException("Path does not exist: " + root);
            if (!root.isAbsolute())
                throw new IllegalArgumentException("Path is not absolute: " + root);
            if (!Files.isDirectory(root))
                throw new IllegalArgumentException("Path is not a directory: " + root);
            if (!Files.isReadable(root))
                throw new IllegalArgumentException("Path is not readable: " + root);
            this.root = root;
            this.mimeTable = mimeTable;
        }

        private static final HttpHandler NOT_IMPLEMENTED_HANDLER =
            HttpHandlers.of(501, Headers.of(), "");

        private static final HttpHandler METHOD_NOT_ALLOWED_HANDLER =
            HttpHandlers.of(405, Headers.of("Allow", "HEAD, GET, POST"), "");

        public static HttpHandler create(Path root, UnaryOperator<String> mimeTable) {
            var fallbackHandler = HttpHandlers.handleOrElse(
                r -> UNSUPPORTED_METHODS.contains(r.getRequestMethod()),
                METHOD_NOT_ALLOWED_HANDLER,
                NOT_IMPLEMENTED_HANDLER);
            return HttpHandlers.handleOrElse(
                r -> SUPPORTED_METHODS.contains(r.getRequestMethod()),
                new FileServerHandler(root, mimeTable), fallbackHandler);
        }

        private void handleHEAD(HttpExchange exchange, Path path) throws IOException {
            handleSupportedMethod(exchange, path, false);
        }

        private void handleGET(HttpExchange exchange, Path path) throws IOException {
            handleSupportedMethod(exchange, path, true);
        }

        private void handleSupportedMethod(HttpExchange exchange, Path path, boolean writeBody)
            throws IOException {
            if (Files.isDirectory(path)) {
                if (missingSlash(exchange)) {
                    handleMovedPermanently(exchange);
                    return;
                }
                if (indexFile(path) != null) {
                    serveFile(exchange, indexFile(path), writeBody);
                } else {
                    listFiles(exchange, path, writeBody);
                }
            } else {
                serveFile(exchange, path, writeBody);
            }
        }

        private void handleMovedPermanently(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Location", getRedirectURI(exchange.getRequestURI()));
            exchange.sendResponseHeaders(301, -1);
        }

        private void handleNotFound(HttpExchange exchange) throws IOException {
            String fileNotFound = "File not found";
            var bytes = (openHTML
                + "<h1>" + fileNotFound + "</h1>\n"
                + "<p>" + sanitize.apply(exchange.getRequestURI().getPath()) + "</p>\n"
                + closeHTML).getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");

            if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
                exchange.sendResponseHeaders(404, -1);
            } else {
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        private String getRedirectURI(URI uri) {
            String query = uri.getRawQuery();
            String redirectPath = uri.getRawPath() + "/";
            return query == null ? redirectPath : redirectPath + "?" + query;
        }

        private static boolean missingSlash(HttpExchange exchange) {
            return !exchange.getRequestURI().getPath().endsWith("/");
        }

        private static String contextPath(HttpExchange exchange) {
            String context = exchange.getHttpContext().getPath();
            if (!context.startsWith("/")) {
                throw new IllegalArgumentException("Context path invalid: " + context);
            }
            return context;
        }

        private static String requestPath(HttpExchange exchange) {
            String request = exchange.getRequestURI().getPath();
            if (!request.startsWith("/")) {
                throw new IllegalArgumentException("Request path invalid: " + request);
            }
            return request;
        }

        // Checks that the request does not escape context.
        private static void checkRequestWithinContext(String requestPath,
            String contextPath) {
            if (requestPath.equals(contextPath)) {
                return;  // context path requested, e.g. context /foo, request /foo
            }
            String contextPathWithTrailingSlash = contextPath.endsWith("/")
                ? contextPath : contextPath + "/";
            if (!requestPath.startsWith(contextPathWithTrailingSlash)) {
                throw new IllegalArgumentException("Request not in context: " + contextPath);
            }
        }

        // Checks that path is, or is within, the root.
        private static Path checkPathWithinRoot(Path path, Path root) {
            if (!path.startsWith(root)) {
                throw new IllegalArgumentException("Request not in root");
            }
            return path;
        }

        // Returns the request URI path relative to the context.
        private static String relativeRequestPath(HttpExchange exchange) {
            String context = contextPath(exchange);
            String request = requestPath(exchange);
            checkRequestWithinContext(request, context);
            return request.substring(context.length());
        }

        private Path mapToPath(HttpExchange exchange, Path root) {
            try {
                assert root.isAbsolute() && Files.isDirectory(root);  // checked during creation
                String uriPath = relativeRequestPath(exchange);
                String[] pathSegment = uriPath.split("/");

                // resolve each path segment against the root
                Path path = root;
                for (var segment : pathSegment) {
                    path = path.resolve(segment);
                    if (!Files.isReadable(path) || isHiddenOrSymLink(path)) {
                        return null;  // stop resolution
                    }
                }
                path = path.normalize();
                return checkPathWithinRoot(path, root);
            } catch (Exception e) {
                logger.error("FileServerHandler: request URI path resolution failed", e);
                return null;  // could not resolve request URI path
            }
        }

        private static Path indexFile(Path path) {
            Path html = path.resolve("index.html");
            Path htm = path.resolve("index.htm");
            return Files.exists(html) ? html : Files.exists(htm) ? htm : null;
        }

        private void serveFile(HttpExchange exchange, Path path, boolean writeBody)
            throws IOException
        {
            var respHdrs = exchange.getResponseHeaders();
            respHdrs.set("Content-Type", mediaType(path.toString()));
            if (writeBody) {
                exchange.sendResponseHeaders(200, Files.size(path));
                try (InputStream fis = Files.newInputStream(path);
                    OutputStream os = exchange.getResponseBody()) {
                    fis.transferTo(os);
                }
            } else {
                respHdrs.set("Content-Length", Long.toString(Files.size(path)));
                exchange.sendResponseHeaders(200, -1);
            }
        }

        private void listFiles(HttpExchange exchange, Path path, boolean writeBody)
            throws IOException
        {
            var respHdrs = exchange.getResponseHeaders();
            respHdrs.set("Content-Type", "text/html; charset=UTF-8");
            var bodyBytes = dirListing(exchange, path).getBytes(UTF_8);
            if (writeBody) {
                exchange.sendResponseHeaders(200, bodyBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bodyBytes);
                }
            } else {
                respHdrs.set("Content-Length", Integer.toString(bodyBytes.length));
                exchange.sendResponseHeaders(200, -1);
            }
        }

        private static final String openHTML = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8"/>
            </head>
            <body>
            """;

        private static final String closeHTML = """
            </body>
            </html>
            """;

        private static final String hrefListItemTemplate = """
            <li><a href="%s">%s</a></li>
            """;

        private static String hrefListItemFor(URI uri) {
            return hrefListItemTemplate.formatted(uri.toASCIIString(), sanitize.apply(uri.getPath()));
        }

        private static String dirListing(HttpExchange exchange, Path path) throws IOException {
            String dirListing = "Directory listing for";
            var sb = new StringBuilder(openHTML
                + "<h1>" + dirListing + " "
                + sanitize.apply(exchange.getRequestURI().getPath())
                + "</h1>\n"
                + "<a href=\"/upload\">Upload</a>"
                + "<ul>\n"
            );
            try (var paths = Files.list(path)) {
                paths.filter(p -> Files.isReadable(p) && !isHiddenOrSymLink(p))
                    .map(p -> path.toUri().relativize(p.toUri()))
                    .forEach(uri -> sb.append(hrefListItemFor(uri)));
            }
            sb.append("</ul>\n");
            sb.append(closeHTML);

            return sb.toString();
        }

        private static boolean isHiddenOrSymLink(Path path) {
            try {
                return Files.isHidden(path) || Files.isSymbolicLink(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // Default for unknown content types, as per RFC 2046
        private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

        private String mediaType(String file) {
            String type = mimeTable.apply(file);
            return type != null ? type : DEFAULT_CONTENT_TYPE;
        }

        // A non-exhaustive map of reserved-HTML and special characters to their
        // equivalent entity.
        private static final Map<Integer,String> RESERVED_CHARS = Map.of(
            (int) '&'  , "&amp;"   ,
            (int) '<'  , "&lt;"    ,
            (int) '>'  , "&gt;"    ,
            (int) '"'  , "&quot;"  ,
            (int) '\'' , "&#x27;"  ,
            (int) '/'  , "&#x2F;"  );

        // A function that takes a string and returns a sanitized version of that
        // string with the reserved-HTML and special characters replaced with their
        // equivalent entity.
        private static final UnaryOperator<String> sanitize =
            file -> file.chars().collect(StringBuilder::new,
                (sb, c) -> sb.append(RESERVED_CHARS.getOrDefault(c, Character.toString(c))),
                StringBuilder::append).toString();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            assert List.of("GET", "HEAD").contains(exchange.getRequestMethod());
            try (exchange) {
                Path path = mapToPath(exchange, root);
                if (path != null) {
                    exchange.setAttribute("request-path",
                        path.toString());  // store for OutputFilter
                    if (!Files.exists(path) || !Files.isReadable(path) || isHiddenOrSymLink(path)) {
                        handleNotFound(exchange);
                    } else if (exchange.getRequestMethod().equals("HEAD")) {
                        handleHEAD(exchange, path);
                    } else {
                        handleGET(exchange, path);
                    }
                } else {
                    exchange.setAttribute("request-path", "could not resolve request URI path");
                    handleNotFound(exchange);
                }
            }
        }
    }
}
