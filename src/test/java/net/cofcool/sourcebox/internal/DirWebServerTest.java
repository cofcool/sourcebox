package net.cofcool.sourcebox.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(VertxExtension.class)
class DirWebServerTest extends BaseTest {

    static final String UPLOAD_FILE = "clippingsKindle.txt";
    String URL = App.getGlobalConfig(ToolRunner.ADDRESS_KEY) + ":" + getPort();

    static Args ARGS;

    @TempDir
    static File file;

    HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var server = new DirWebServer();
        ARGS = new Args().copyConfigFrom(server.config()).arg("root", file.getAbsolutePath());
        server.deploy(vertx, new CLIWebToolVerticle(server), ARGS)
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }

    @Test
    void get(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var request = HttpRequest
                .newBuilder()
                .uri(URI.create(URL + "/files"))
                .GET()
                .build();
            var response = client.send(request, BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            testContext.completeNow();
        });
    }

    @Test
    void post(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var boundary = "--" + RandomGenerator.getDefault().ints(10, 0, 10).mapToObj(String::valueOf).collect(Collectors.joining());
            var contentStart = (boundary
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + UPLOAD_FILE + "\""
                + "\r\n"
                + "Content-Type: text/plain"
                + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1);
            var contentEnd = ("\r\n" + boundary + "--").getBytes(StandardCharsets.ISO_8859_1);
            var content = Files.readAllBytes(Path.of(Utils.getTestResourcePath("/" + UPLOAD_FILE)));
            byte[] input = new byte[contentStart.length + contentEnd.length + content.length];
            System.arraycopy(contentStart, 0, input, 0, contentStart.length);
            System.arraycopy(content, 0, input, contentStart.length, content.length);
            System.arraycopy(contentEnd, 0, input, contentStart.length + content.length, contentEnd.length);

            var request = HttpRequest
                .newBuilder()
                .uri(URI.create(URL + "/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2))
                .POST(BodyPublishers.ofByteArray(input))
                .build();
            var response = client.send(request, BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            System.out.println(response.body());
            assertTrue(Files.exists(Path.of(file.getAbsolutePath(), UPLOAD_FILE)));
            testContext.completeNow();
        });
    }

}