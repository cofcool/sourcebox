package net.cofcool.toolbox.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_METHOD)
class DirWebServerTest extends BaseTest {

    static final String UPLOAD_FILE = "splitKindleClippingsTest.txt";
    static final String ROOT = "./target/upload";

    static Args ARGS;

    HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();

    @Override
    protected Tool instance() {
        return new DirWebServer();
    }

    @BeforeAll
    static void startServer() throws Exception {
        Path.of(ROOT).toFile().mkdir();
        var server = new DirWebServer();
        ARGS = server.config().arg("root", ROOT);
        server.run(ARGS);
    }

    @Override
    protected void init() {
        args = ARGS;
    }

    @Test
    void get() throws Exception {
        var request = HttpRequest
            .newBuilder()
            .uri(URI.create("http://127.0.0.1:" + args.readArg("port").val() + "/upload"))
            .GET()
            .build();
        var response = client.send(request, BodyHandlers.ofString());
        assertEquals(response.statusCode(), 200);
        System.out.println(response.body());
    }

    @Test
    void post() throws Exception {
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
            .uri(URI.create("http://127.0.0.1:" + args.readArg("port").val() + "/upload"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2))
            .POST(BodyPublishers.ofByteArray(input))
            .build();
        var response = client.send(request, BodyHandlers.ofString());
        assertEquals(response.statusCode(), 200);
        System.out.println(response.body());
        assertTrue(Files.exists(Path.of(ROOT, UPLOAD_FILE)));
    }
}