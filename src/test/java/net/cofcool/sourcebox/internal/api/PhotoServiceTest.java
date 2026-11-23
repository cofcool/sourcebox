package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.internal.api.entity.PhotoAlbum;
import net.cofcool.sourcebox.internal.api.entity.SmartAlbum;
import net.cofcool.sourcebox.util.SqlRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class PhotoServiceTest extends BaseTest {

    private static Vertx vertx;
    private static PhotoService photoService;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup(Vertx testVertx) {
        vertx = testVertx;
        SqlRepository.init(vertx);
        photoService = new PhotoService(vertx);
    }

    @AfterAll
    static void teardown() {
        SqlRepository.closePool();
    }

    @Test
    void savesAlbumWithMultipleSourceDirectories() throws Exception {
        var first = Files.createDirectories(tempDir.resolve("album-root/旅行"));
        var second = Files.createDirectories(tempDir.resolve("backup-root"));

        var scan = await(photoService.scan(new JsonObject()
            .put("albumName", "旅行")
            .put("sourceRoots", new JsonArray()
                .add(first.toString())
                .add(second.toString()))
            .put("recursive", true)));

        assertEquals("COMPLETED", scan.getString("status"));
    }

    @Test
    void scansMultipleDirectoriesAndDeduplicatesBySha256() throws Exception {
        var first = Files.createDirectories(tempDir.resolve("scan-one"));
        var second = Files.createDirectories(tempDir.resolve("scan-two"));
        Files.writeString(first.resolve("same.jpg"), "duplicate image", StandardCharsets.UTF_8);
        Files.copy(first.resolve("same.jpg"), second.resolve("same-copy.jpg"));

        var album = await(photoService.saveAlbum(PhotoAlbum.builder()
            .name("多目录相册")
            .type("MANUAL")
            .sourceRoots(first + "\n" + second)
            .recursive(true)
            .build()));

        await(photoService.scan(new JsonObject()
            .put("albumId", album.id())
            .put("sourceRoots", new JsonArray()
                .add(first.toString())
                .add(second.toString()))
            .put("recursive", true)));

        var photos = await(photoService.findAlbumPhotos(album.id()));
        assertEquals(1, photos.size());
        assertEquals("same.jpg", photos.getFirst().fileName());
    }

    @Test
    void moveToTrashThenRestoreRestoresNormalStatus() throws Exception {
        var root = Files.createDirectories(tempDir.resolve("trash-test"));
        Files.writeString(root.resolve("photo.jpg"), "photo", StandardCharsets.UTF_8);

        var album = await(photoService.saveAlbum(PhotoAlbum.builder()
            .name("回收站相册")
            .type("MANUAL")
            .sourceRoots(root.toString())
            .recursive(true)
            .build()));

        await(photoService.scan(new JsonObject()
            .put("albumId", album.id())
            .put("sourceRoots", new JsonArray().add(root.toString()))
            .put("recursive", true)));

        var photos = await(photoService.findAlbumPhotos(album.id()));
        assertFalse(photos.isEmpty());

        var photoId = photos.getFirst().id();
        await(photoService.moveToTrash(photoId));
        assertFalse(await(photoService.findTrash()).isEmpty());

        await(photoService.restorePhoto(photoId));
        var restored = await(photoService.findPhoto(photoId));
        assertEquals("NORMAL", restored.status());
        assertNull(restored.trashedAt());
    }

    @Test
    void smartAlbumMatchesMimeRule() throws Exception {
        var root = Files.createDirectories(tempDir.resolve("smart-test"));
        Files.writeString(root.resolve("smart.jpg"), "12345", StandardCharsets.UTF_8);

        var album = await(photoService.saveAlbum(PhotoAlbum.builder()
            .name("智能来源")
            .type("MANUAL")
            .sourceRoots(root.toString())
            .recursive(true)
            .build()));

        await(photoService.scan(new JsonObject()
            .put("albumId", album.id())
            .put("sourceRoots", new JsonArray().add(root.toString()))
            .put("recursive", true)));

        var smart = await(photoService.saveSmartAlbum(SmartAlbum.builder()
            .name("JPEG照片")
            .matchType("ALL")
            .rulesJson(new JsonArray()
                .add(new JsonObject()
                    .put("field", "mimeType")
                    .put("operator", "contains")
                    .put("value", "image"))
                .encode())
            .enabled(true)
            .build()));

        var photos = await(photoService.findSmartAlbumPhotos(smart.id()));
        assertFalse(photos.isEmpty());
        assertTrue(photos.stream().anyMatch(photo ->
            photo.fileName().equals("smart.jpg")
                && photo.mimeType() != null
                && photo.mimeType().startsWith("image/")
        ));
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage()
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);
    }
}
