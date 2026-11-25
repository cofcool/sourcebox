package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.imageio.ImageIO;
import lombok.CustomLog;
import lombok.SneakyThrows;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.internal.api.entity.Photo;
import net.cofcool.sourcebox.internal.api.entity.PhotoAlbum;
import net.cofcool.sourcebox.internal.api.entity.SmartAlbum;
import net.cofcool.sourcebox.util.QueryBuilder;
import net.cofcool.sourcebox.util.RepositoryUtil;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public class PhotoService {
    public static final String SCAN_ADDRESS = "sourcebox.photo.scan";
    public static final String BACKUP_ADDRESS = "sourcebox.photo.backup";
    private static final List<String> EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "bmp",
        "webp", "heic", "heif", "mp4");

    private final Vertx vertx;
    private final SqlRepository<PhotoAlbum> albumRepository;
    private final SqlRepository<Photo> photoRepository;
    private final SqlRepository<SmartAlbum> smartRepository;
    private final ConcurrentMap<String, JsonObject> jobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, JsonObject> backupJobs = new ConcurrentHashMap<>();

    public PhotoService(Vertx vertx) {
        this.vertx = vertx;
        albumRepository = RepositoryUtil.getRepository(PhotoAlbum.class);
        photoRepository = RepositoryUtil.getRepository(Photo.class);
        smartRepository = RepositoryUtil.getRepository(SmartAlbum.class);
        vertx.eventBus().consumer(SCAN_ADDRESS, message -> {
            if (!(message.body() instanceof JsonObject job)) return;
            executeScan(job.getString("jobId"), job.getJsonObject("request"));
        });
        vertx.eventBus().consumer(BACKUP_ADDRESS, message -> {
            if (!(message.body() instanceof JsonObject job)) return;
            executeBackup(job);
        });
    }

    @SneakyThrows
    public Future<PhotoAlbum> saveAlbum(PhotoAlbum album) {
        var now = LocalDateTime.now();
        var roots = normalizeRoots(album.sourceRoots());
        if (StringUtils.isBlank(roots)) {
            roots = App.globalCfgDir("photos");
            FileUtils.forceMkdir(new File(roots));
        }
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
            .put("jobId", jobId).put("status", "QUEUED")
            .put("scanned", 0).put("added", 0).put("skipped", 0).put("failed", 0);
        jobs.put(jobId, result);

        vertx.eventBus().send(SCAN_ADDRESS, new JsonObject()
            .put("jobId", jobId)
            .put("request", request.copy()));

        return Future.succeededFuture(result.copy());
    }

    private void executeScan(String jobId, JsonObject request) {
        var result = jobs.get(jobId);
        if (result == null) {
            return;
        }
        result.put("status", "RUNNING");
        var albumId = request.getString("albumId");

        Future<PhotoAlbum> albumFuture;
        if (StringUtils.isNotBlank(albumId)) {
            albumFuture = findAlbum(albumId);
        } else {
            var roots = request.getJsonArray("sourceRoots");
            if (roots == null) {
                roots = new JsonArray();
                var one = request.getString("rootPath");
                if (StringUtils.isNotBlank(one)) {
                    roots.add(one);
                }
            }
            var rootList = roots.stream().map(Object::toString)
                .filter(StringUtils::isNotBlank).distinct().toList();
            var recursive = request.getBoolean("recursive", true);
            var albumName = StringUtils.defaultIfBlank(
                request.getString("albumName"),
                rootList.isEmpty() ? "未命名相册"
                    : Path.of(rootList.getFirst()).getFileName().toString()
            );
            albumFuture = saveAlbum(PhotoAlbum.builder()
                .name(albumName).type("MANUAL").sourceRoots(String.join("\n", rootList))
                .recursive(recursive).status("NORMAL").build());
        }

        albumFuture.compose(a -> {
            var rootList = Arrays.stream(a.sourceRoots().split("\\n"))
                .map(String::trim).filter(StringUtils::isNotBlank).distinct().toList();
            var recursive = a.recursive() == null || a.recursive();
            log.info("Start photo scan job {0}, album={1}, roots={2}, recursive={3}",
                jobId, a.name(), rootList, recursive);
            var candidates = collectCandidates(a, rootList, recursive);
            var album = candidates.album;
            var total = candidates.files.size();
            result.put("scanned", total);
            log.info("Photo scan job {0} found {1} image files in album {2}",
                jobId, total, album.name());
            return scanCandidates(album, candidates.files, result);
        }).onSuccess(ignored -> {
            result.put("status", "COMPLETED");
            log.info("Photo scan job {0} completed: added={1}, skipped={2}, failed={3}",
                jobId, result.getInteger("added"), result.getInteger("skipped"),
                result.getInteger("failed"));
        }).onFailure(error -> {
            result.put("status", "FAILED").put("error", error.getMessage());
            log.error("Photo scan job " + jobId + " failed", error);
        });
    }

    public JsonObject scanJob(String jobId) {
        return jobs.get(jobId);
    }

    private Future<JsonObject> scanCandidates(PhotoAlbum album, List<Path> files, JsonObject result) {
        Future<Void> chain = Future.succeededFuture();
        for (var file : files) {
            chain = chain.compose(ignored ->
                scanFile(album, file).onSuccess(added -> {
                    var key = added ? "added" : "skipped";
                    result.put(key, result.getInteger(key) + 1);
                    if (added) {
                        log.info("Added photo {0} to album {1}", file.toAbsolutePath(), album.name());
                    } else {
                        log.debug("Skipped duplicate photo {0} in album {1}", file.toAbsolutePath(), album.name());
                    }
                }).recover(error -> {
                    result.put("failed", result.getInteger("failed") + 1);
                    log.error("Failed to scan photo " + file.toAbsolutePath(), error);
                    return Future.succeededFuture(false);
                }).mapEmpty()
            );
        }
        return chain.map(result);
    }

    private record ScanCandidates(PhotoAlbum album, List<Path> files) {}

    private ScanCandidates collectCandidates(PhotoAlbum album, List<String> roots, boolean recursive) {
        var files = new ArrayList<Path>();
        for (var root : roots) {
            var rootPath = Path.of(root);
            if (!Files.exists(rootPath)) {
                log.info("Photo scan skips missing root {0}", rootPath.toAbsolutePath());
                continue;
            }
            try (var paths = recursive ? Files.walk(rootPath) : Files.walk(rootPath, 1)) {
                files.addAll(paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        var ext = FilenameUtils.getExtension(path.getFileName().toString())
                            .toLowerCase();
                        return EXTENSIONS.contains(ext);
                    })
                    .toList()
                );
            } catch (Exception error) {
                log.error("Failed to scan photo root " + rootPath.toAbsolutePath(), error);
            }
            log.info("Finished scanning photo root {0}", rootPath.toAbsolutePath());
        }
        return new ScanCandidates(album, files);
    }

    @SneakyThrows
    private Future<Boolean> scanFile(PhotoAlbum album, Path file) {
        var hash = sha256(file);
        var dimensions = imageDimensions(file.toFile());
        var relative = relativePath(album, file);
        var mime = Files.probeContentType(file);
        var modified = Files.getLastModifiedTime(file).toInstant();
        var now = LocalDateTime.now();
        var takenAt = LocalDateTime.ofInstant(modified, ZoneId.systemDefault());
        var photo = new Photo(
            UUID.randomUUID().toString(), album.id(), file.getFileName().toString(),
            relative, file.toAbsolutePath().toString(), mime, hash, Files.size(file),
            dimensions[0], dimensions[1], takenAt, "SCANNED",
            new JsonObject()
                .put("size", Files.size(file))
                .put("width", dimensions[0])
                .put("height", dimensions[1])
                .put("modifiedAt", takenAt.toString())
                .encode(),
            "NORMAL", null, now, now);
        return photoRepository.find(Photo.builder()
                .albumId(photo.albumId()).sha256(photo.sha256()).status("NORMAL").build())
            .compose(existing -> {
                if (!existing.isEmpty()) {
                    return Future.succeededFuture(false);
                }
                return photoRepository.save(photo).map(saved -> true);
            });
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

    public Future<List<Photo>> findAllPhotos() {
        return photoRepository.find();
    }

    public Future<List<Photo>> findPhotoPage(String year, String status, String category,
                                              int page, int size) {
        if (page < 1 || size < 1) {
            return Future.failedFuture("page and size must be positive");
        }
        LocalDate yearStart;
        if (StringUtils.isNotBlank(year)) {
            try {
                yearStart = LocalDate.of(Integer.parseInt(year), 1, 1);
            } catch (Exception e) {
                return Future.failedFuture("invalid year: " + year);
            }
        } else {
            yearStart = null;
        }

        var offset = (long) (page - 1) * size;
        var query = QueryBuilder.builder()
            .select("p.*")
            .from("photo p")
            .leftJoin("photo_album pa ON p.album_id=pa.id")
            .orderBy("COALESCE(p.taken_at,p.create_time) DESC")
            .limit(size);
        if (offset > 0) query.offset(offset);
        if (yearStart != null) {
            query.and("COALESCE(p.taken_at,p.create_time) >= ? AND COALESCE(p.taken_at,p.create_time) < ?",
                yearStart.atStartOfDay(), yearStart.plusYears(1).atStartOfDay());
        }
        if (StringUtils.isNotBlank(status)) {
            query.and("p.status=?", status);
        }
        if (StringUtils.isNotBlank(category)) {
            query.and("COALESCE(pa.name,'未分类相册')=?", category);
        }
        return photoRepository.find(query);
    }

    public Future<PhotoStatistics> findStatistics(String year, String status, String category) {
        LocalDate yearStart;
        if (StringUtils.isNotBlank(year)) {
            try {
                yearStart = LocalDate.of(Integer.parseInt(year), 1, 1);
            } catch (Exception e) {
                return Future.failedFuture("invalid year: " + year);
            }
        } else {
            yearStart = null;
        }

        var totalQuery = photoStatisticsFilter(
            QueryBuilder.builder().count().from("photo p")
                .leftJoin("photo_album pa ON p.album_id=pa.id"),
            yearStart, status, category);
        var statusQuery = photoStatisticsFilter(
            QueryBuilder.builder()
                .select("COALESCE(p.status, '未分类') AS name")
                .select("COUNT(*) AS cnt")
                .from("photo p")
                .leftJoin("photo_album pa ON p.album_id=pa.id")
                .groupBy("COALESCE(p.status, '未分类')")
                .orderBy("name"),
            yearStart, status, category);
        var categoryQuery = photoStatisticsFilter(
            QueryBuilder.builder()
                .select("COALESCE(pa.name, '未分类相册') AS name")
                .select("COUNT(*) AS cnt")
                .from("photo p")
                .leftJoin("photo_album pa ON p.album_id=pa.id")
                .groupBy("COALESCE(pa.name, '未分类相册')")
                .orderBy("name"),
            yearStart, status, category);
        var statusOptionsQuery = QueryBuilder.builder()
            .select("DISTINCT status AS name")
            .from("photo")
            .and("status IS NOT NULL")
            .orderBy("name");
        var categoryOptionsQuery = QueryBuilder.builder()
            .select("DISTINCT name AS name")
            .from("photo_album")
            .and("status<>?", "DELETED")
            .orderBy("name");
        var albumCountQuery = QueryBuilder.builder()
            .count()
            .from("photo_album")
            .and("status<>?", "DELETED");

        return Future.all(
            photoRepository.count(totalQuery),
            photoRepository.execute(statusQuery),
            photoRepository.execute(categoryQuery),
            photoRepository.execute(statusOptionsQuery),
            photoRepository.execute(categoryOptionsQuery),
            albumRepository.count(albumCountQuery)
        ).map(result -> new PhotoStatistics(
            result.resultAt(0),
            ((Integer) result.resultAt(5)).longValue(),
            photoDimensionCounts(result.resultAt(1)),
            photoDimensionCounts(result.resultAt(2)),
            photoOptionNames(result.resultAt(3)),
            photoOptionNames(result.resultAt(4))
        ));
    }

    private QueryBuilder photoStatisticsFilter(QueryBuilder query, LocalDate yearStart,
                                                String status, String category) {
        if (yearStart != null) {
            query.and("COALESCE(p.taken_at,p.create_time) >= ? AND COALESCE(p.taken_at,p.create_time) < ?",
                yearStart.atStartOfDay(), yearStart.plusYears(1).atStartOfDay());
        }
        if (StringUtils.isNotBlank(status)) {
            query.and("p.status=?", status);
        }
        if (StringUtils.isNotBlank(category)) {
            query.and("COALESCE(pa.name,'未分类相册')=?", category);
        }
        return query;
    }

    private List<DimensionCount> photoDimensionCounts(RowSet<Row> rows) {
        var counts = new ArrayList<DimensionCount>();
        for (Row row : rows) {
            counts.add(new DimensionCount(row.getString("NAME"), row.getLong("CNT").intValue()));
        }
        return counts;
    }

    private List<String> photoOptionNames(RowSet<Row> rows) {
        var names = new ArrayList<String>();
        for (Row row : rows) {
            var name = row.getString("NAME");
            if (StringUtils.isNotBlank(name)) names.add(name);
        }
        return names;
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

    public JsonObject backup(String albumId, Path tempFile, String fileName,
                             String relativePath, String sha256) {
        var jobId = UUID.randomUUID().toString();
        startBackupJob(jobId, albumId, null);
        backupItem(jobId, albumId, tempFile, fileName, relativePath, sha256);
        return backupJobs.get(jobId).copy();
    }

    public JsonObject startBackupJob(String jobId, String albumId, String sourceDirectory) {
        var result = new JsonObject()
            .put("jobId", jobId)
            .put("status", "QUEUED")
            .put("albumId", albumId)
            .put("sourceDirectory", sourceDirectory)
            .put("total", 0)
            .put("completed", 0)
            .put("failed", 0)
            .put("pending", 0)
            .put("finishRequested", false);
        backupJobs.put(jobId, result);
        return result.copy();
    }

    public JsonObject backupItem(String jobId, String albumId, Path tempFile,
                                 String fileName, String relativePath, String sha256) {
        var result = backupJobs.computeIfAbsent(jobId, id -> new JsonObject()
            .put("jobId", id).put("status", "QUEUED").put("albumId", albumId)
            .put("total", 0).put("completed", 0).put("failed", 0)
            .put("pending", 0).put("finishRequested", false));
        result.put("status", "RUNNING");
        result.put("total", result.getInteger("total") + 1);
        result.put("pending", result.getInteger("pending") + 1);

        vertx.eventBus().send(BACKUP_ADDRESS, new JsonObject()
            .put("jobId", jobId)
            .put("albumId", albumId)
            .put("tempFile", tempFile.toAbsolutePath().toString())
            .put("fileName", fileName)
            .put("relativePath", relativePath)
            .put("sha256", sha256));
        return result.copy();
    }

    public JsonObject finishBackupJob(String jobId) {
        var result = backupJobs.get(jobId);
        if (result == null) return null;
        result.put("finishRequested", true);
        if (result.getInteger("pending") == 0) {
            if (result.getInteger("failed") > 0) {
                result.put("status", "COMPLETED_WITH_ERRORS");
            } else {
                result.put("status", "COMPLETED");
            }
        } else {
            result.put("status", "RUNNING");
        }
        return result.copy();
    }

    public JsonObject backupJob(String jobId) {
        return backupJobs.get(jobId);
    }

    private void executeBackup(JsonObject job) {
        var jobId = job.getString("jobId");
        var result = backupJobs.get(jobId);
        if (result == null) return;
        result.put("status", "RUNNING");

        var albumId = job.getString("albumId");
        var tempFile = Path.of(job.getString("tempFile"));
        var fileName = job.getString("fileName");
        var relativePath = job.getString("relativePath");
        var sha256 = job.getString("sha256");

        executeBackupFile(albumId, tempFile, fileName, relativePath, sha256)
            .onSuccess(photo -> {
                result.put("completed", result.getInteger("completed") + 1)
                    .put("photoId", photo.id());
                result.put("pending", Math.max(0, result.getInteger("pending") - 1));
                if (result.getString("sourceDirectory") == null && result.getInteger("pending") == 0) {
                    result.put("status", "COMPLETED");
                } else if (result.getBoolean("finishRequested")
                    && result.getInteger("pending") == 0) {
                    result.put("status",
                        result.getInteger("failed") > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
                }
                log.info("Photo backup job {0} completed: photo={1}", jobId, photo.id());
            })
            .onFailure(error -> {
                result.put("failed", result.getInteger("failed") + 1)
                    .put("error", error.getMessage());
                result.put("pending", Math.max(0, result.getInteger("pending") - 1));
                if (result.getString("sourceDirectory") == null && result.getInteger("pending") == 0) {
                    result.put("status", "FAILED");
                } else if (result.getBoolean("finishRequested")
                    && result.getInteger("pending") == 0) {
                    result.put("status",
                        result.getInteger("failed") > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
                }
                log.error("Photo backup job " + jobId + " failed", error);
            });
    }

    private Future<Photo> executeBackupFile(String albumId, Path tempFile, String fileName,
                                            String relativePath, String sha256) {
        return findAlbum(albumId)
            .compose(album -> createBackupPhoto(albumId, tempFile, fileName, relativePath, sha256, album))
            .compose(photoRepository::save);
    }

    @SneakyThrows
    private static Future<Photo> createBackupPhoto(String albumId, Path tempFile, String fileName,
        String relativePath, String sha256, PhotoAlbum album) {
        var firstRoot = Arrays.stream(album.sourceRoots().split("\\n"))
            .map(String::trim).filter(StringUtils::isNotBlank).findFirst();
        if (firstRoot.isEmpty()) throw new IllegalArgumentException("相册未配置目录");
        var target = Path.of(firstRoot.get()).resolve(
            relativePath == null ? fileName : relativePath);
        Files.createDirectories(target.getParent());
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);

        var digest = sha256 == null || sha256.isBlank() ? sha256(target) : sha256;
        var dimensions = imageDimensions(target.toFile());
        var now = LocalDateTime.now();
        return Future.succeededFuture(new Photo(
            UUID.randomUUID().toString(), albumId, fileName,
            target.getFileName().toString(), target.toString(),
            Files.probeContentType(target), digest, Files.size(target),
            dimensions[0], dimensions[1], null, "SCANNED", "{}", "NORMAL", null, now, now
        ));
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

    public record DimensionCount(String name, int count) {}

    public record PhotoStatistics(
        int total,
        long albumCount,
        List<DimensionCount> statusCounts,
        List<DimensionCount> categoryCounts,
        List<String> statusOptions,
        List<String> categoryOptions
    ) {}
}
