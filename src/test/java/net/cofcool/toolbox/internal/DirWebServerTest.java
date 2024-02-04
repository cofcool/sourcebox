package net.cofcool.toolbox.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class DirWebServerTest {

    static final String UPLOAD_FILE = "clippingsKindle.txt";
    static final String ROOT = "./target/upload";

    static Args ARGS;

    HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        Path.of(ROOT).toFile().mkdir();
        var server = new DirWebServer();
        ARGS = new Args().copyConfigFrom(server.config()).arg("root", ROOT);
        server.deploy(vertx, null, ARGS)
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }

    @Test
    void get(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var request = HttpRequest
                .newBuilder()
                .uri(URI.create("http://127.0.0.1:" + ARGS.readArg("port").val() + "/files"))
                .GET()
                .build();
            var response = client.send(request, BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
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
                .uri(URI.create("http://127.0.0.1:" + ARGS.readArg("port").val() + "/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2))
                .POST(BodyPublishers.ofByteArray(input))
                .build();
            var response = client.send(request, BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            System.out.println(response.body());
            assertTrue(Files.exists(Path.of(ROOT, UPLOAD_FILE)));
            testContext.completeNow();
        });
    }

}