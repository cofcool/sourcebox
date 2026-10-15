package net.cofcool.sourcebox.internal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Tool.Experiment
public class LanFileShare implements Tool {

    private static final String UPLOAD_PATH = "/upload";
    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TRANSFER_HEADER = "X-Sourcebox-Transfer";
    private static final String PATH_HEADER = "X-Sourcebox-Path";
    private static final String SHA256_HEADER = "X-Sourcebox-SHA256";

    @Override
    public ToolName name() {
        return ToolName.lanShare;
    }

    @Override
    public void run(Args args) throws Exception {
        switch (args.readArg("mode").val()) {
            case "send" -> send(args);
            case "receive" -> receive(args);
            default -> throw new IllegalArgumentException("mode must be send or receive");
        }
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("mode", "receive", "send or receive", false, "receive"))
            .arg(new Arg("path", null, "file or directory to send", false, "./photos"))
            .arg(new Arg("to", null, "receiver base URL, for example http://192.168.1.20:43127", false, null))
            .arg(new Arg("token", null, "receiver's one-time token", false, null))
            .arg(new Arg("dir", null, "directory where received files are stored", false, "./received"))
            .arg(new Arg("port", "0", "receiver port; 0 selects an available port", false, "0"))
            .arg(new Arg("compress", "false", "compress the upload as a ZIP archive", false, "true"))
            .arg(new Arg("conflict", "skip", "receiver conflict policy: skip or overwrite", false, "overwrite"));
    }

    private void receive(Args args) throws Exception {
        Path destination = Path.of(args.readArg("dir").requiredVal("dir is required in receive mode"))
            .toAbsolutePath().normalize();
        Files.createDirectories(destination);
        destination = destination.toRealPath();
        String conflictPolicy = args.readArg("conflict").val();
        validateConflictPolicy(conflictPolicy);
        int requestedPort = Integer.parseInt(args.readArg("port").val());
        if (requestedPort < 0 || requestedPort > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }

        String token = args.getArgVal("token").filter(s -> !s.isBlank()).orElseGet(LanFileShare::newToken);
        HttpServer server = startReceiver(destination, token, requestedPort, conflictPolicy);
        ExecutorService executor = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "lan-share-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();

        int actualPort = server.getAddress().getPort();
        args.getContext().write("Receiving into: " + destination);
        for (String address : localAddresses()) {
            args.getContext().write("Share URL: http://" + address + ":" + actualPort);
        }
        args.getContext().write("Token: " + token);
        args.getContext().write("Stop with Ctrl+C");

        CountDownLatch stopped = new CountDownLatch(1);
        Thread shutdownHook = new Thread(() -> {
            server.stop(0);
            executor.shutdownNow();
            stopped.countDown();
        }, "lan-share-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            stopped.await();
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    static HttpServer startReceiver(Path destination, String token, int port) throws IOException {
        return startReceiver(destination, token, port, "skip");
    }

    static HttpServer startReceiver(Path destination, String token, int port, String conflictPolicy) throws IOException {
        validateConflictPolicy(conflictPolicy);
        Path root = destination.toAbsolutePath().normalize();
        Files.createDirectories(root);
        root = root.toRealPath();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Path receiveRoot = root;
        server.createContext(UPLOAD_PATH, exchange -> handleUpload(exchange, receiveRoot, token, conflictPolicy));
        return server;
    }

    private static void handleUpload(HttpExchange exchange, Path destination, String token, String conflictPolicy) throws IOException {
        try (exchange) {
            if (!"POST".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "Only POST is supported");
                return;
            }
            String authorization = exchange.getRequestHeaders().getFirst(TOKEN_HEADER);
            byte[] expected = (TOKEN_PREFIX + token).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] actual = authorization == null ? new byte[0] : authorization.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) {
                respond(exchange, 401, "Invalid share token");
                return;
            }

            try {
                String transferType = exchange.getRequestHeaders().getFirst(TRANSFER_HEADER);
                String expectedSha256 = exchange.getRequestHeaders().getFirst(SHA256_HEADER);
                UploadResult result = switch (transferType == null ? "" : transferType) {
                    case "zip" -> receiveArchive(exchange.getRequestBody(), destination, conflictPolicy, expectedSha256);
                    case "file" -> receiveFile(exchange, destination, conflictPolicy, expectedSha256);
                    case "directory" -> receiveDirectory(exchange, destination, conflictPolicy);
                    default -> throw new IllegalArgumentException("Missing or unsupported transfer format");
                };
                respond(exchange, result.status(), result.message());
                } catch (IllegalArgumentException exception) {
                    respond(exchange, 400, exception.getMessage());
                } catch (IOException exception) {
                    respond(exchange, 500, "Could not store upload: " + exception.getMessage());
            }
        }
    }

    private static UploadResult receiveArchive(InputStream requestBody, Path destination, String conflictPolicy,
        String expectedSha256) throws IOException {
        Path staging = Files.createTempDirectory(destination, ".lan-share-");
        try {
            Path archive = Files.createTempFile(staging, ".lan-share-upload-", ".zip");
            MessageDigest archiveDigest = newSha256();
            try (InputStream input = new DigestInputStream(requestBody, archiveDigest);
                 OutputStream output = Files.newOutputStream(archive)) {
                input.transferTo(output);
            }
            String actualSha256 = HexFormat.of().formatHex(archiveDigest.digest());
            if (!matchesSha256(expectedSha256, actualSha256)) {
                return new UploadResult(422, sha256Mismatch("compressed upload", expectedSha256, actualSha256));
            }

            List<Path> stagedItems = new ArrayList<>();
            String topLevelName = null;
            try (var zip = new ZipInputStream(Files.newInputStream(archive))) {
                ZipEntry entry;
                byte[] buffer = new byte[16 * 1024];
                while ((entry = zip.getNextEntry()) != null) {
                    String safeName = entry.getName().replace('\\', '/');
                    if (safeName.isBlank() || safeName.startsWith("/") || safeName.matches("^[A-Za-z]:.*")) {
                        throw new IllegalArgumentException("Invalid archive path");
                    }
                    Path relative = Path.of(safeName).normalize();
                    if (relative.isAbsolute() || relative.startsWith("..") || relative.toString().isBlank()) {
                        throw new IllegalArgumentException("Invalid archive path: " + safeName);
                    }
                    String firstName = relative.getName(0).toString();
                    if (topLevelName == null) {
                        topLevelName = firstName;
                    } else if (!topLevelName.equals(firstName)) {
                        throw new IllegalArgumentException("Archive must contain one top-level item");
                    }

                    Path output = staging.resolve(relative).normalize();
                    if (!output.startsWith(staging)) {
                        throw new IllegalArgumentException("Invalid archive path: " + safeName);
                    }
                    if (entry.isDirectory() || safeName.endsWith("/")) {
                        Files.createDirectories(output);
                    } else {
                        Files.createDirectories(output.getParent());
                        try (var file = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                            int count;
                            while ((count = zip.read(buffer)) != -1) {
                                file.write(buffer, 0, count);
                            }
                        }
                    }
                    stagedItems.add(output);
                    zip.closeEntry();
                }
            } catch (ZipException exception) {
                throw new IllegalArgumentException("Invalid ZIP archive", exception);
            }

            if (stagedItems.isEmpty()) {
                throw new IllegalArgumentException("Archive is empty");
            }
            stagedItems.sort(Comparator.comparingInt(Path::getNameCount));
            List<String> conflicts = new ArrayList<>();
            List<Path> skippedDirectories = new ArrayList<>();
            int transferred = 0;
            for (Path stagedItem : stagedItems) {
                Path relative = staging.relativize(stagedItem);
                if (skippedDirectories.stream().anyMatch(relative::startsWith)) {
                    continue;
                }
                boolean committed = commitItem(stagedItem, destination.resolve(relative), destination, conflictPolicy, conflicts);
                if (committed) {
                    transferred++;
                } else if (Files.isDirectory(stagedItem)) {
                    skippedDirectories.add(relative);
                }
            }
            if (transferred == 0 && !conflicts.isEmpty()) {
                return new UploadResult(200, String.join("\n", conflicts));
            }
            return new UploadResult(201, String.join("\n", conflicts));
        } finally {
            deleteTree(staging);
        }
    }

    private static UploadResult receiveFile(HttpExchange exchange, Path destination, String conflictPolicy,
        String expectedSha256) throws IOException {
        Path relative = decodeRelativePath(exchange.getRequestHeaders().getFirst(PATH_HEADER));
        Path target = safeResolve(destination, relative);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".lan-share-", ".tmp");
        try {
            MessageDigest fileDigest = newSha256();
            try (InputStream input = new DigestInputStream(exchange.getRequestBody(), fileDigest);
                 OutputStream output = Files.newOutputStream(temporary)) {
                input.transferTo(output);
            }
            String actualSha256 = HexFormat.of().formatHex(fileDigest.digest());
            if (!matchesSha256(expectedSha256, actualSha256)) {
                return new UploadResult(422, sha256Mismatch(relative.toString(), expectedSha256, actualSha256));
            }
            if (Files.exists(target, java.nio.file.LinkOption.NOFOLLOW_LINKS) && "skip".equals(conflictPolicy)) {
                return new UploadResult(200, "Skipped conflict: " + relative);
            }
            moveItem(temporary, target, "overwrite".equals(conflictPolicy));
            return new UploadResult(201, "Received: " + relative);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static UploadResult receiveDirectory(HttpExchange exchange, Path destination, String conflictPolicy) throws IOException {
        Path relative = decodeRelativePath(exchange.getRequestHeaders().getFirst(PATH_HEADER));
        Path target = safeResolve(destination, relative);
        if (Files.exists(target, java.nio.file.LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(target)) {
            if ("skip".equals(conflictPolicy)) {
                return new UploadResult(200, "Skipped conflict: " + relative);
            }
            Files.delete(target);
        }
        Files.createDirectories(target);
        return new UploadResult(201, "Received directory: " + relative);
    }

    private static boolean commitItem(Path staged, Path target, Path destination, String conflictPolicy, List<String> conflicts) throws IOException {
        boolean directory = Files.isDirectory(staged);
        if (directory) {
            if (Files.exists(target, java.nio.file.LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(target)) {
                if ("skip".equals(conflictPolicy)) {
                    conflicts.add("Skipped conflict: " + destination.relativize(target));
                    return false;
                }
                Files.delete(target);
            }
            Files.createDirectories(target);
            return true;
        }
        Files.createDirectories(target.getParent());
        if (Files.exists(target, java.nio.file.LinkOption.NOFOLLOW_LINKS) && "skip".equals(conflictPolicy)) {
            conflicts.add("Skipped conflict: " + destination.relativize(target));
            return false;
        }
        moveItem(staged, target, "overwrite".equals(conflictPolicy));
        return true;
    }

    private static void moveItem(Path source, Path target, boolean overwrite) throws IOException {
        try {
            if (overwrite) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            if (overwrite) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        }
    }

    private static Path decodeRelativePath(String encodedPath) {
        if (encodedPath == null || encodedPath.isBlank()) {
            throw new IllegalArgumentException("Missing transfer path");
        }
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(encodedPath), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid transfer path", exception);
        }
        Path relative = Path.of(decoded).normalize();
        if (relative.isAbsolute() || relative.startsWith("..") || relative.toString().isBlank()) {
            throw new IllegalArgumentException("Invalid transfer path: " + decoded);
        }
        return relative;
    }

    private static Path safeResolve(Path destination, Path relative) throws IOException {
        Path target = destination.resolve(relative).normalize();
        if (!target.startsWith(destination)) {
            throw new IllegalArgumentException("Invalid transfer path: " + relative);
        }
        Path ancestor = target.getParent();
        while (ancestor != null && ancestor.startsWith(destination)) {
            if (Files.isSymbolicLink(ancestor)) {
                throw new IllegalArgumentException("Transfer path traverses a symbolic link");
            }
            ancestor = ancestor.getParent();
        }
        return target;
    }

    private static void validateConflictPolicy(String conflictPolicy) {
        if (!"skip".equals(conflictPolicy) && !"overwrite".equals(conflictPolicy)) {
            throw new IllegalArgumentException("conflict must be skip or overwrite");
        }
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static boolean matchesSha256(String expectedSha256, String actualSha256) {
        if (expectedSha256 == null || !expectedSha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Missing or invalid SHA-256 header");
        }
        return MessageDigest.isEqual(
            expectedSha256.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
            actualSha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );
    }

    private static String sha256Mismatch(String path, String expected, String actual) {
        return "SHA-256 mismatch for " + path + ": expected " + expected + ", actual " + actual;
    }

    private void send(Args args) throws Exception {
        Path source = Path.of(args.readArg("path").requiredVal("path is required in send mode"))
            .toAbsolutePath().normalize();
        if (Files.isSymbolicLink(source) || (!Files.isRegularFile(source) && !Files.isDirectory(source))) {
            throw new IllegalArgumentException("path must be a regular file or directory: " + source);
        }
        URI target = uploadUri(args.readArg("to").requiredVal("to is required in send mode"));
        String token = args.readArg("token").requiredVal("token is required in send mode");
        boolean compress = Boolean.parseBoolean(args.readArg("compress").val());
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        List<String> conflicts = new ArrayList<>();
        int transferred = 0;
        if (compress) {
            Path archive = Files.createTempFile("sourcebox-lan-share-", ".zip");
            try {
                createArchive(source, archive);
                TransferProgress progress = new TransferProgress(args, Files.size(archive));
                progress.report(true);
                HttpResponse<String> response = sendRequest(client, target, token, "zip", null, sha256(archive),
                    withProgress(HttpRequest.BodyPublishers.ofFile(archive), progress));
                progress.report(true);
                if (response.statusCode() >= 400) {
                    throw new IllegalStateException("Receiver returned HTTP " + response.statusCode() + ": " + response.body());
                }
                collectResult(response.body(), conflicts);
                transferred++;
            } finally {
                Files.deleteIfExists(archive);
            }
        } else {
            Path uploadRoot = Files.isDirectory(source) ? source.getFileName() : source.getFileName();
            if (uploadRoot == null) {
                uploadRoot = Path.of("root");
            }
            List<Path> entries;
            if (Files.isDirectory(source)) {
                try (var paths = Files.walk(source)) {
                    entries = paths.sorted().toList();
                }
            } else {
                entries = List.of(source);
            }
            long totalBytes = 0;
            for (Path entry : entries) {
                if (Files.isSymbolicLink(entry)) {
                    throw new IllegalArgumentException("Symbolic links cannot be sent: " + entry);
                }
                if (Files.isRegularFile(entry)) {
                    totalBytes += Files.size(entry);
                }
            }
            TransferProgress progress = new TransferProgress(args, totalBytes);
            progress.report(true);
            for (Path entry : entries) {
                Path relative = Files.isDirectory(source)
                    ? uploadRoot.resolve(source.relativize(entry))
                    : uploadRoot;
                if (Files.isDirectory(entry)) {
                    HttpResponse<String> response = sendRequest(client, target, token, "directory", encodePath(relative.toString()),
                        null, HttpRequest.BodyPublishers.noBody());
                    transferred += collectResult(response, conflicts);
                } else if (Files.isRegularFile(entry)) {
                    HttpResponse<String> response = sendRequest(client, target, token, "file", encodePath(relative.toString()),
                        sha256(entry), withProgress(HttpRequest.BodyPublishers.ofFile(entry), progress));
                    transferred += collectResult(response, conflicts);
                }
            }
            progress.report(true);
        }
        conflicts.forEach(args.getContext()::write);
        args.getContext().write("Transfer complete; accepted entries: " + transferred + ", conflicts: " + conflicts.size());
    }

    private static HttpResponse<String> sendRequest(HttpClient client, URI target, String token, String transferType,
        String relativePath, String sha256, HttpRequest.BodyPublisher body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(target)
            .timeout(Duration.ofMinutes(30))
            .header("Content-Type", "zip".equals(transferType) ? "application/zip" : "application/octet-stream")
            .header(TOKEN_HEADER, TOKEN_PREFIX + token)
            .header(TRANSFER_HEADER, transferType);
        if (relativePath != null) {
            builder.header(PATH_HEADER, relativePath);
        }
        if (sha256 != null) {
            builder.header(SHA256_HEADER, sha256);
        }
        return client.send(builder.POST(body).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.BodyPublisher withProgress(HttpRequest.BodyPublisher body, TransferProgress progress) {
        return new HttpRequest.BodyPublisher() {
            @Override
            public long contentLength() {
                return body.contentLength();
            }

            @Override
            public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                body.subscribe(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }

                    @Override
                    public void onNext(ByteBuffer item) {
                        progress.advance(item.remaining());
                        subscriber.onNext(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }
        };
    }

    private static final class TransferProgress {

        private static final long REPORT_INTERVAL_NANOS = Duration.ofMillis(250).toNanos();

        private final Args args;
        private final long totalBytes;
        private final long startedAt = System.nanoTime();
        private final AtomicLong sentBytes = new AtomicLong();
        private long lastReportedAt;

        private TransferProgress(Args args, long totalBytes) {
            this.args = args;
            this.totalBytes = totalBytes;
        }

        private void advance(long count) {
            sentBytes.addAndGet(count);
            report(false);
        }

        private synchronized void report(boolean force) {
            long now = System.nanoTime();
            if (!force && now - lastReportedAt < REPORT_INTERVAL_NANOS) {
                return;
            }
            lastReportedAt = now;
            long sent = sentBytes.get();
            double percent = totalBytes == 0 ? 100 : Math.min(100.0, sent * 100.0 / totalBytes);
            double seconds = Math.max((now - startedAt) / 1_000_000_000.0, 0.001);
            double bytesPerSecond = sent / seconds;
            args.getContext().write(String.format(java.util.Locale.ROOT,
                "Transfer progress: %.1f%% (%s/%s), speed %s/s",
                percent, formatBytes(sent), formatBytes(totalBytes), formatBytes((long) bytesPerSecond)));
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KiB", "MiB", "GiB", "TiB"};
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format(java.util.Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest = newSha256();
        try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static int collectResult(HttpResponse<String> response, List<String> conflicts) {
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Receiver returned HTTP " + response.statusCode() + ": " + response.body());
        }
        collectResult(response.body(), conflicts);
        return response.statusCode() == 201 ? 1 : 0;
    }

    private static void collectResult(String responseBody, List<String> conflicts) {
        if (responseBody.startsWith("Skipped conflict:")) {
            conflicts.add(responseBody);
        } else if (responseBody.contains("\nSkipped conflict:")) {
            for (String line : responseBody.split("\\R")) {
                if (line.startsWith("Skipped conflict:")) {
                    conflicts.add(line);
                }
            }
        }
    }

    private static String encodePath(String relativePath) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(relativePath.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private record UploadResult(int status, String message) {
    }

    private static URI uploadUri(String baseUrl) throws URISyntaxException {
        URI base = URI.create(baseUrl);
        if (!"http".equalsIgnoreCase(base.getScheme()) || base.getHost() == null || base.getPort() < 1) {
            throw new IllegalArgumentException("to must be an HTTP URL with a port, for example http://192.168.1.20:43127");
        }
        return new URI("http", null, base.getHost(), base.getPort(), UPLOAD_PATH, null, null);
    }

    private static void createArchive(Path source, Path archive) throws IOException {
        String rootName = source.getFileName() == null ? "root" : source.getFileName().toString();
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            if (Files.isDirectory(source)) {
                try (var paths = Files.walk(source)) {
                    for (Path path : paths.sorted().toList()) {
                        if (Files.isSymbolicLink(path)) {
                            throw new IllegalArgumentException("Symbolic links cannot be sent: " + path);
                        }
                        String relative = rootName;
                        if (!path.equals(source)) {
                            relative += "/" + source.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
                        }
                        addEntry(zip, path, relative);
                    }
                }
            } else {
                addEntry(zip, source, rootName);
            }
        }
    }

    private static void addEntry(ZipOutputStream zip, Path path, String name) throws IOException {
        String entryName = Files.isDirectory(path) ? name + "/" : name;
        zip.putNextEntry(new ZipEntry(entryName));
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                input.transferTo(zip);
            }
        }
        zip.closeEntry();
    }

    private static List<String> localAddresses() throws IOException {
        List<String> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();
            if (!network.isUp() || network.isLoopback()) {
                continue;
            }
            Enumeration<InetAddress> candidates = network.getInetAddresses();
            while (candidates.hasMoreElements()) {
                InetAddress address = candidates.nextElement();
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    addresses.add(address.getHostAddress());
                }
            }
        }
        if (addresses.isEmpty()) {
            addresses.add(InetAddress.getLoopbackAddress().getHostAddress());
        }
        return addresses;
    }

    private static String newToken() {
        byte[] token = new byte[24];
        new SecureRandom().nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private static void respond(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }
}