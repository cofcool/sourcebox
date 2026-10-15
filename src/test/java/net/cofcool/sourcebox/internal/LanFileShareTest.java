package net.cofcool.sourcebox.internal;

import com.sun.net.httpserver.HttpServer;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LanFileShareTest {

    @TempDir
    Path tempDir;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void receivesDirectoryArchiveWithoutCollidingWithExistingServerPort() throws Exception {
        Path output = Files.createDirectory(tempDir.resolve("received"));
        Path archive = createArchive("photos/one.txt", "first", "photos/nested/two.txt", "second");
        server = LanFileShare.startReceiver(output, "secret", 0);
        server.start();

        HttpResponse<String> response = upload(archive, "secret");

        assertEquals(201, response.statusCode());
        assertEquals("first", Files.readString(output.resolve("photos/one.txt")));
        assertEquals("second", Files.readString(output.resolve("photos/nested/two.txt")));
        assertNotEquals(ToolRunner.DEFAULT_PORT, server.getAddress().getPort());
    }

    @Test
    void sendsSelectedDirectoryToPeer() throws Exception {
        Path output = Files.createDirectory(tempDir.resolve("received"));
        Path source = Files.createDirectory(tempDir.resolve("documents"));
        Files.createDirectories(source.resolve("nested"));
        Files.writeString(source.resolve("nested/report.txt"), "report");
        server = LanFileShare.startReceiver(output, "secret", 0);
        server.start();
        CapturingContext context = new CapturingContext();

        LanFileShare tool = new LanFileShare();
        Args args = new Args().copyConfigFrom(tool.config())
            .arg("mode", "send")
            .arg("path", source.toString())
            .arg("to", "http://127.0.0.1:" + server.getAddress().getPort())
            .arg("token", "secret")
            .context(context);
        tool.run(args);

        assertEquals("report", Files.readString(output.resolve("documents/nested/report.txt")));
        assertTrue(context.messages.stream().anyMatch(message -> message.contains("Transfer progress:")));
        assertTrue(context.messages.stream().anyMatch(message -> message.contains("speed ")));
    }

    @Test
    void sendsCompressedDirectoryWhenRequested() throws Exception {
        Path output = Files.createDirectory(tempDir.resolve("received"));
        Path source = Files.createDirectory(tempDir.resolve("compressed"));
        Files.writeString(source.resolve("data.txt"), "compressed payload");
        server = LanFileShare.startReceiver(output, "secret", 0);
        server.start();

        LanFileShare tool = new LanFileShare();
        Args args = new Args().copyConfigFrom(tool.config())
            .arg("mode", "send")
            .arg("path", source.toString())
            .arg("to", "http://127.0.0.1:" + server.getAddress().getPort())
            .arg("token", "secret")
            .arg("compress", "true")
            .context(new ConsoleToolContext());
        tool.run(args);

        assertEquals("compressed payload", Files.readString(output.resolve("compressed/data.txt")));
    }

    @Test
    void rawFileUploadSkipsExistingFileByDefault() throws Exception {
        Path output = Files.createDirectory(tempDir.resolve("received"));
        Path source = tempDir.resolve("same.txt");
        Path existing = output.resolve("same.txt");
        Files.writeString(source, "incoming");
        Files.writeString(existing, "keep existing");
        server = LanFileShare.startReceiver(output, "secret", 0);
        server.start();

        LanFileShare tool = new LanFileShare();
        Args args = new Args().copyConfigFrom(tool.config())
            .arg("mode", "send")
            .arg("path", source.toString())
            .arg("to", "http://127.0.0.1:" + server.getAddress().getPort())
            .arg("token", "secret")
            .context(new ConsoleToolContext());
        tool.run(args);

        assertEquals("keep existing", Files.readString(existing));
    }

    @Test
    void rejectsRawFileWhenSha256DoesNotMatch() throws Exception {
        Path output = Files.createDirectory(tempDir.resolve("received"));
        Path source = tempDir.resolve("incoming.txt");
        Files.writeString(source, "payload");
        server = LanFileShare.startReceiver(output, "secret", 0);
        server.start();
        String falseSha256 = "0".repeat(64);

        HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/upload"))
                .header("Authorization", "Bearer secret")
                .header("X-Sourcebox-Transfer", "file")
                .header("X-Sourcebox-Path", Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("incoming.txt".getBytes(StandardCharsets.UTF_8)))
                .header("X-Sourcebox-SHA256", falseSha256)
                .POST(HttpRequest.BodyPublishers.ofFile(source))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("SHA-256 mismatch for incoming.txt"));
        assertTrue(response.body().contains("expected " + falseSha256));
        assertTrue(response.body().contains("actual " + sha256(source)));
        assertFalse(Files.exists(output.resolve("incoming.txt")));
    }

    @Test
    void skipsConflictsByDefaultAndOverwritesWhenRequested() throws Exception {
        Path output = Files.createDirectory(tempDir.resolve("received"));
        Path existing = output.resolve("folder/file.txt");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "original");
        server = LanFileShare.startReceiver(output, "secret", 0);
        server.start();

        HttpResponse<String> skipped = upload(createArchive("folder/file.txt", "new"), "secret");
        assertEquals(200, skipped.statusCode());
        assertTrue(skipped.body().contains("Skipped conflict: folder/file.txt"));
        assertEquals("original", Files.readString(existing));

        server.stop(0);
        server = LanFileShare.startReceiver(output, "secret", 0, "overwrite");
        server.start();
        HttpResponse<String> overwritten = upload(createArchive("folder/file.txt", "replacement"), "secret");
        assertEquals(201, overwritten.statusCode());
        assertEquals("replacement", Files.readString(existing));
    }

    @Test
    void rejectsWrongTokenAndZipSlipArchive() throws Exception {
        Path output = Files.createDirectory(tempDir.resolve("received"));
        Path validArchive = createArchive("safe/file.txt", "content");
        server = LanFileShare.startReceiver(output, "secret", 0);
        server.start();

        HttpResponse<String> unauthorized = upload(validArchive, "wrong");
        assertEquals(401, unauthorized.statusCode());
        assertFalse(Files.exists(output.resolve("safe")));

        Path traversalArchive = createArchive("../outside.txt", "escaped");
        HttpResponse<String> rejected = upload(traversalArchive, "secret");
        assertEquals(400, rejected.statusCode());
        assertFalse(Files.exists(tempDir.resolve("outside.txt")));
        try (var files = Files.list(output)) {
            assertTrue(files.findAny().isEmpty());
        }
    }

    private HttpResponse<String> upload(Path archive, String token) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/upload"))
                .header("Content-Type", "application/zip")
                .header("Authorization", "Bearer " + token)
                .header("X-Sourcebox-Transfer", "zip")
                .header("X-Sourcebox-SHA256", sha256(archive))
                .POST(HttpRequest.BodyPublishers.ofFile(archive))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private Path createArchive(String... entries) throws Exception {
        Path archive = Files.createTempFile(tempDir, "share-", ".zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (int i = 0; i < entries.length; i += 2) {
                zip.putNextEntry(new ZipEntry(entries[i]));
                zip.write(entries[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive;
    }

    private String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static class CapturingContext implements ToolContext {

        private final List<String> messages = new ArrayList<>();

        @Override
        public ToolContext write(Object value) {
            messages.add(String.valueOf(value));
            return this;
        }

        @Override
        public ToolContext write(String name, String value) {
            messages.add(value);
            return this;
        }

        @Override
        public RunnerType runnerType() {
            return RunnerType.CLI;
        }
    }
}