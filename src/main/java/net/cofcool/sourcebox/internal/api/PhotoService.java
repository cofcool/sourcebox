package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.cofcool.sourcebox.internal.api.entity.Photo;
import net.cofcool.sourcebox.internal.api.entity.PhotoAlbum;
import net.cofcool.sourcebox.internal.api.entity.SmartAlbum;
import net.cofcool.sourcebox.util.RepositoryUtil;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PhotoService {
    private final Vertx vertx;
    private final SqlRepository<PhotoAlbum> albumRepository;
    private final SqlRepository<Photo> photoRepository;
    private final SqlRepository<SmartAlbum> smartRepository;
    private final ConcurrentMap<String, JsonObject> jobs = new ConcurrentHashMap<>();

    public PhotoService(Vertx vertx) {
        this.vertx = vertx;
        albumRepository = RepositoryUtil.getRepository(PhotoAlbum.class);
        photoRepository = RepositoryUtil.getRepository(Photo.class);
        smartRepository = RepositoryUtil.getRepository(SmartAlbum.class);
    }

    public Future<PhotoAlbum> saveAlbum(PhotoAlbum album) {
        var now = LocalDateTime.now();
        var roots = normalizeRoots(album.sourceRoots());
        var saved = new PhotoAlbum(
            StringUtils.defaultIfBlank(album.id(), UUID.randomUUID().toString()),
            StringUtils.defaultIfBlank(album.name(), "未命名相册"),
            StringUtils.defaultIfBlank(album.type(), "MANUAL"),
            roots,
            album.recursive() == null || album.recursive(),
            album.coverPhotoId(),
            "NORMAL",
            now,
            now
        );
        return albumRepository.save(saved);
    }

    public Future<List<PhotoAlbum>> findAlbums() {
        return albumRepository.find().map(all -> all.stream()
            .filter(a -> !"DELETED".equals(a.status()))
            .toList());
    }

    public Future<PhotoAlbum> findAlbum(String id) {
        return albumRepository.find(id);
    }

    public Future<Boolean> deleteAlbum(String id) {
        return findAlbum(id).compose(album ->
            albumRepository.save(new PhotoAlbum(
                album.id(), album.name(), album.type(), album.sourceRoots(), album.recursive(),
                album.coverPhotoId(), "DELETED", album.createTime(), LocalDateTime.now()
            ))
        ).map(album -> true);
    }

    public Future<JsonObject> scan(JsonObject request) {
        var jobId = UUID.randomUUID().toString();
        var result = new JsonObject()
            .put("jobId", jobId).put("status", "RUNNING")
            .put("scanned", 0).put("added", 0).put("skipped", 0).put("failed", 0);
        jobs.put(jobId, result);

        var roots = request.getJsonArray("sourceRoots");
        if (roots == null) {
            roots = new JsonArray();
            var one = request.getString("rootPath");
            if (StringUtils.isNotBlank(one)) roots.add(one);
        }
        var rootList = roots.stream().map(Object::toString).filter(StringUtils::isNotBlank).distinct().toList();
        var recursive = request.getBoolean("recursive", true);
        var albumName = StringUtils.defaultIfBlank(
            request.getString("albumName"),
            rootList.isEmpty() ? "未命名相册" : Path.of(rootList.getFirst()).getFileName().toString()
        );
        var albumId = request.getString("albumId");

        Future<PhotoAlbum> albumFuture;
        if (StringUtils.isNotBlank(albumId)) {
            albumFuture = findAlbum(albumId).compose(existing -> {
                var mergedRoots = mergeRoots(existing.sourceRoots(), rootList);
                return saveAlbum(new PhotoAlbum(
                    existing.id(), existing.name(), existing.type(), mergedRoots, recursive,
                    existing.coverPhotoId(), existing.status(), existing.createTime(), existing.updateTime()
                ));
            });
        } else {
            albumFuture = saveAlbum(PhotoAlbum.builder()
                .name(albumName).type("MANUAL").sourceRoots(String.join("\n", rootList))
                .recursive(recursive).status("NORMAL").build());
        }

        return albumFuture.compose(album ->
            vertx.executeBlocking(() -> collectCandidates(album, rootList, recursive))
        ).compose(candidates -> {
            var album = candidates.album;
            var total = candidates.files.size();
            result.put("scanned", total);
            return scanCandidates(album, candidates.files, result).map(result.put("status", "COMPLETED"));
        }).onFailure(error -> result.put("status", "FAILED").put("error", error.getMessage()));
    }

    public JsonObject scanJob(String jobId) {
        return jobs.get(jobId);
    }

    private Future<JsonObject> scanCandidates(PhotoAlbum album, List<Path> files, JsonObject result) {
        Future<Void> chain = Future.succeededFuture();
        for (var file : files) {
            chain = chain.compose(ignored ->
                scanFile(album, file).onSuccess(added -> result.put(
                    added ? "added" : "skipped",
                    result.getInteger(added ? "added" : "skipped") + 1
                )).recover(error -> {
                    result.put("failed", result.getInteger("failed") + 1);
                    return Future.succeededFuture(false);
                }).mapEmpty()
            );
        }
        return chain.map(result);
    }

    private record ScanCandidates(PhotoAlbum album, List<Path> files) {}

    private ScanCandidates collectCandidates(PhotoAlbum album, List<String> roots, boolean recursive) {
        var files = new ArrayList<Path>();
        var extensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".heic", ".heif");
        for (var root : roots) {
            var rootPath = Path.of(root);
            if (!Files.exists(rootPath)) continue;
            try (var paths = recursive ? Files.walk(rootPath) : Files.walk(rootPath, 1)) {
                paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        var name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        var dot = name.lastIndexOf('.');
                        return dot >= 0 && extensions.contains(name.substring(dot));
                    })
                    .forEach(files::add);
            } catch (Exception ignored) {
            }
        }
        return new ScanCandidates(album, files);
    }

    private Future<Boolean> scanFile(PhotoAlbum album, Path file) {
        return vertx.executeBlocking(() -> {
            var hash = sha256(file);
            var dimensions = imageDimensions(file.toFile());
            var relative = relativePath(album, file);
            var mime = Files.probeContentType(file);
            var modified = Files.getLastModifiedTime(file).toInstant();
            var now = LocalDateTime.now();
            var takenAt = LocalDateTime.ofInstant(modified, ZoneId.systemDefault());
            return new Photo(
                UUID.randomUUID().toString(), album.id(), file.getFileName().toString(),
                relative, file.toAbsolutePath().toString(), mime, hash, Files.size(file),
                dimensions[0], dimensions[1], takenAt, "SCANNED",
                new JsonObject()
                    .put("size", Files.size(file))
                    .put("width", dimensions[0])
                    .put("height", dimensions[1])
                    .put("modifiedAt", takenAt.toString())
                    .encode(),
                "NORMAL", null, now, now
            );
        }).compose(photo ->
            photoRepository.find(Photo.builder()
                .albumId(photo.albumId()).sha256(photo.sha256()).status("NORMAL").build())
                .compose(existing -> {
                    if (!existing.isEmpty()) return Future.succeededFuture(false);
                    return photoRepository.save(photo).map(saved -> true);
                })
        );
    }

    private String relativePath(PhotoAlbum album, Path file) {
        for (var root : album.sourceRoots().split("\\n")) {
            var rootPath = Path.of(root.trim()).toAbsolutePath();
            var absolute = file.toAbsolutePath();
            if (absolute.startsWith(rootPath)) {
                var relative = rootPath.relativize(absolute);
                return relative.toString().replace(File.separatorChar, '/');
            }
        }
        return file.getFileName().toString();
    }

    public Future<List<Photo>> findAlbumPhotos(String albumId) {
        return photoRepository.find().map(all -> all.stream()
            .filter(p -> albumId.equals(p.albumId()) && "NORMAL".equals(p.status()))
            .sorted((a, b) -> b.createTime().compareTo(a.createTime()))
            .toList());
    }

    public Future<Photo> findPhoto(String id) {
        return photoRepository.find(id);
    }

    public Future<Void> moveToTrash(String id) {
        return findPhoto(id).compose(photo ->
            photoRepository.save(new Photo(
                photo.id(), photo.albumId(), photo.fileName(), photo.relativePath(), photo.sourcePath(),
                photo.mimeType(), photo.sha256(), photo.size(), photo.width(), photo.height(), photo.takenAt(),
                photo.metadataStatus(), photo.metadataJson(), "TRASH", LocalDateTime.now(),
                photo.createTime(), LocalDateTime.now()
            ))
        ).mapEmpty();
    }

    public Future<Void> restorePhoto(String id) {
        return findPhoto(id).compose(photo ->
            photoRepository.updateIncludingNulls(id, new Photo(
                photo.id(), photo.albumId(), photo.fileName(), photo.relativePath(), photo.sourcePath(),
                photo.mimeType(), photo.sha256(), photo.size(), photo.width(), photo.height(), photo.takenAt(),
                photo.metadataStatus(), photo.metadataJson(), "NORMAL", null,
                photo.createTime(), LocalDateTime.now()
            ))
        ).mapEmpty();
    }

    public Future<List<Photo>> findTrash() {
        return photoRepository.find().map(all -> all.stream()
            .filter(p -> "TRASH".equals(p.status())).toList());
    }

    public Future<Boolean> permanentlyDeletePhoto(String id) {
        return findPhoto(id).compose(photo -> {
            try {
                Files.deleteIfExists(Path.of(photo.sourcePath()));
            } catch (Exception ignored) {
            }
            return photoRepository.delete(id);
        });
    }

    public Future<SmartAlbum> saveSmartAlbum(SmartAlbum album) {
        var now = LocalDateTime.now();
        return smartRepository.save(new SmartAlbum(
            StringUtils.defaultIfBlank(album.id(), UUID.randomUUID().toString()),
            StringUtils.defaultIfBlank(album.name(), "智能相册"),
            StringUtils.defaultIfBlank(album.matchType(), "ALL"),
            StringUtils.defaultIfBlank(album.rulesJson(), "[]"),
            album.enabled() == null || album.enabled(),
            now, now
        ));
    }

    public Future<List<SmartAlbum>> findSmartAlbums() {
        return smartRepository.find();
    }

    public Future<Boolean> deleteSmartAlbum(String id) {
        return smartRepository.delete(id);
    }

    public Future<List<Photo>> findSmartAlbumPhotos(String id) {
        return smartRepository.find(id).compose(album -> photoRepository.find()
            .map(all -> all.stream()
                .filter(p -> "NORMAL".equals(p.status()) && matchesSmartAlbum(p, album))
                .toList()));
    }

    private boolean matchesSmartAlbum(Photo photo, SmartAlbum album) {
        if (!Boolean.TRUE.equals(album.enabled())) return false;
        JsonArray rules;
        try {
            rules = new JsonArray(album.rulesJson());
        } catch (Exception error) {
            return false;
        }
        boolean all = !"ANY".equalsIgnoreCase(album.matchType());
        for (var item : rules) {
            var rule = (JsonObject) item;
            var matched = matchesRule(photo, rule);
            if (all && !matched) return false;
            if (!all && matched) return true;
        }
        return all;
    }

    private boolean matchesRule(Photo photo, JsonObject rule) {
        var field = rule.getString("field");
        var operator = rule.getString("operator", "eq");
        var value = rule.getValue("value");
        var actual = switch (field) {
            case "mimeType" -> photo.mimeType();
            case "width" -> photo.width();
            case "height" -> photo.height();
            case "size" -> photo.size();
            case "takenAt" -> photo.takenAt() == null ? null : photo.takenAt().toString();
            default -> photo.metadataJson();
        };
        if (actual == null) return false;
        return switch (operator) {
            case "neq" -> !String.valueOf(actual).equals(String.valueOf(value));
            case "in" -> value instanceof JsonArray a && a.stream().anyMatch(v ->
                String.valueOf(actual).equals(String.valueOf(v)));
            case "contains" -> String.valueOf(actual).toLowerCase(Locale.ROOT)
                .contains(String.valueOf(value).toLowerCase(Locale.ROOT));
            case "gte" -> String.valueOf(actual).compareTo(String.valueOf(value)) >= 0;
            case "lte" -> String.valueOf(actual).compareTo(String.valueOf(value)) <= 0;
            default -> String.valueOf(actual).equals(String.valueOf(value));
        };
    }

    private String normalizeRoots(String roots) {
        if (StringUtils.isBlank(roots)) return "";
        return Arrays.stream(roots.split("[\\n,]"))
            .map(String::trim).filter(StringUtils::isNotBlank).distinct()
            .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String mergeRoots(String existing, List<String> roots) {
        var merged = new ArrayList<String>();
        if (StringUtils.isNotBlank(existing)) merged.addAll(Arrays.asList(existing.split("\\n")));
        merged.addAll(roots);
        return merged.stream().map(String::trim).filter(StringUtils::isNotBlank).distinct()
            .collect(java.util.stream.Collectors.joining("\n"));
    }

    public Future<Photo> backup(String albumId, Path tempFile, String fileName,
                                String relativePath, String sha256) {
        return findAlbum(albumId).compose(album -> vertx.executeBlocking(() -> {
            var firstRoot = Arrays.stream(album.sourceRoots().split("\\n"))
                .map(String::trim).filter(StringUtils::isNotBlank).findFirst();
            if (firstRoot.isEmpty()) throw new IllegalArgumentException("相册未配置目录");
            var target = Path.of(firstRoot.get()).resolve(relativePath == null ? fileName : relativePath);
            Files.createDirectories(target.getParent());
            Files.copy(tempFile, target, StandardCopyOption.REPLACE_EXISTING);

            var digest = sha256 == null || sha256.isBlank() ? sha256(tempFile) : sha256;
            var dimensions = imageDimensions(target.toFile());
            var now = LocalDateTime.now();
            return new Photo(
                UUID.randomUUID().toString(), albumId, fileName,
                target.getFileName().toString(), target.toString(),
                Files.probeContentType(target), digest, Files.size(target),
                dimensions[0], dimensions[1], null, "SCANNED", "{}", "NORMAL", null, now, now
            );
        })).compose(photoRepository::save);
    }

    private static String sha256(Path path) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            var buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        var hex = new StringBuilder();
        for (var value : digest.digest()) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    private static Integer[] imageDimensions(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            return image == null ? new Integer[]{null, null}
                : new Integer[]{image.getWidth(), image.getHeight()};
        } catch (Exception error) {
            return new Integer[]{null, null};
        }
    }
}
