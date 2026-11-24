package net.cofcool.sourcebox.internal.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.internal.api.entity.PhotoAlbum;
import net.cofcool.sourcebox.internal.api.entity.SmartAlbum;
import net.cofcool.sourcebox.util.SqlRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void statisticsUsesSqlAggregates() throws Exception {
        var statistics = await(photoService.findStatistics(null, null, null));
        assertTrue(statistics.total() >= 0);
        assertTrue(statistics.albumCount() >= 0);
        assertNotNull(statistics.statusCounts());
        assertNotNull(statistics.categoryCounts());
    }

    @Test
    void albumWithoutDirectoryUsesGlobalPhotosDirectory() throws Exception {
        var album = await(photoService.saveAlbum(PhotoAlbum.builder()
            .name("默认目录相册")
            .type("MANUAL")
            .recursive(true)
            .build()));

        assertEquals(App.globalCfgDir("photos"), album.sourceRoots());
        assertTrue(Files.isDirectory(java.nio.file.Path.of(album.sourceRoots())));
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
        var completed = awaitScan(scan.getString("jobId"));
        assertEquals("COMPLETED", completed.getString("status"));
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

        var scan = await(photoService.scan(new JsonObject()
            .put("albumId", album.id())
            .put("sourceRoots", new JsonArray()
                .add(tempDir.resolve("ignored-request-directory").toString()))
            .put("recursive", true)));
        awaitScan(scan.getString("jobId"));

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

        var scan = await(photoService.scan(new JsonObject()
            .put("albumId", album.id())
            .put("sourceRoots", new JsonArray().add(root.toString()))
            .put("recursive", true)));
        awaitScan(scan.getString("jobId"));

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

        var scan = await(photoService.scan(new JsonObject()
            .put("albumId", album.id())
            .put("sourceRoots", new JsonArray().add(root.toString()))
            .put("recursive", true)));
        awaitScan(scan.getString("jobId"));

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

    @Test
    void backupCompletesAsynchronously() throws Exception {
        var root = Files.createDirectories(tempDir.resolve("backup-target"));
        var source = Files.writeString(
            tempDir.resolve("backup-source.jpg"),
            "async backup",
            StandardCharsets.UTF_8
        );

        var album = await(photoService.saveAlbum(PhotoAlbum.builder()
            .name("异步备份相册")
            .type("MANUAL")
            .sourceRoots(root.toString())
            .recursive(true)
            .build()));

        var job = photoService.backup(
            album.id(),
            source,
            "backup-source.jpg",
            "nested/backup-source.jpg",
            null
        );
        assertTrue(job.getString("status").equals("QUEUED")
            || job.getString("status").equals("RUNNING"));

        var completed = awaitBackup(job.getString("jobId"));
        assertEquals("COMPLETED", completed.getString("status"));
        assertNotNull(completed.getString("photoId"));
        assertTrue(Files.exists(root.resolve("nested/backup-source.jpg")));
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage()
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);
    }

    private JsonObject awaitScan(String jobId) throws Exception {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            var job = photoService.scanJob(jobId);
            if (job != null && "COMPLETED".equals(job.getString("status"))) {
                return job.copy();
            }
            if (job != null && "FAILED".equals(job.getString("status"))) {
                throw new IllegalStateException(job.getString("error"));
            }
            Thread.sleep(20);
        }
        throw new IllegalStateException("Photo scan timed out: " + jobId);
    }

    private JsonObject awaitBackup(String jobId) throws Exception {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            var job = photoService.backupJob(jobId);
            if (job != null && "COMPLETED".equals(job.getString("status"))) {
                return job.copy();
            }
            if (job != null && "FAILED".equals(job.getString("status"))) {
                throw new IllegalStateException(job.getString("error"));
            }
            Thread.sleep(20);
        }
        throw new IllegalStateException("Photo backup timed out: " + jobId);
    }
}
