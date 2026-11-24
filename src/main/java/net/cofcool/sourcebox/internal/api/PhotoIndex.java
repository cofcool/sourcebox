package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import net.cofcool.sourcebox.internal.api.entity.PhotoAlbum;
import net.cofcool.sourcebox.internal.api.entity.SmartAlbum;
import net.cofcool.sourcebox.util.VertxUtils;

import java.io.File;

public class PhotoIndex {
    public static Router buildRouter(Vertx vertx) {
        var service = new PhotoService(vertx);
        var router = VertxUtils.createBodyRouter(vertx);

        router.get("/albums").respond(r -> service.findAlbums());
        router.post("/albums").respond(r ->
            service.saveAlbum(r.body().asPojo(PhotoAlbum.class)));
        router.get("/albums/:id").respond(r -> service.findAlbum(r.pathParam("id")));
        router.put("/albums/:id").respond(r -> {
            var incoming = r.body().asPojo(PhotoAlbum.class);
            return service.saveAlbum(new PhotoAlbum(
                r.pathParam("id"), incoming.name(), incoming.type(), incoming.sourceRoots(),
                incoming.recursive(), incoming.coverPhotoId(), incoming.status(), incoming.createTime(), incoming.updateTime()
            ));
        });
        router.delete("/albums/:id").respond(r -> service.deleteAlbum(r.pathParam("id")));
        router.get("/albums/:id/photos").respond(r -> service.findAlbumPhotos(r.pathParam("id")));
        router.post("/albums/:id/scan").respond(r -> service.scan(r.body().asJsonObject()
            .put("albumId", r.pathParam("id"))));

        router.get("/exportAll").respond(r ->
            Future.all(
                service.findAlbums(),
                service.findAllPhotos(),
                service.findSmartAlbums()
            ).map(result -> new io.vertx.core.json.JsonObject()
                .put("albums", result.resultAt(0))
                .put("photos", result.resultAt(1))
                .put("smartAlbums", result.resultAt(2))));

        router.get("/statistics/summary").respond(r -> service.findStatistics(
            r.queryParam("date").stream().findFirst().orElse(null),
            r.queryParam("state").stream().findFirst().orElse(null),
            r.queryParam("category").stream().findFirst().orElse(null)
        ));
        router.get("/photos").respond(r -> service.findPhotoPage(
            r.queryParam("date").stream().findFirst().orElse(null),
            r.queryParam("state").stream().findFirst().orElse(r.queryParam("status").stream().findFirst().orElse(null)),
            r.queryParam("category").stream().findFirst().orElse(null),
            parsePage(r.queryParam("page").stream().findFirst().orElse(null)),
            parsePageSize(r.queryParam("size").stream().findFirst().orElse(null))
        ));

        router.post("/scans").respond(r -> service.scan(r.body().asJsonObject()));
        router.get("/scans/:jobId").handler(r -> {
            var job = service.scanJob(r.pathParam("jobId"));
            if (job == null) r.response().setStatusCode(404).end();
            else r.json(job);
        });

        router.get("/photos/:id").respond(r -> service.findPhoto(r.pathParam("id")));
        router.delete("/photos/:id").respond(r -> service.moveToTrash(r.pathParam("id")).map("ok"));
        router.post("/photos/:id/restore").respond(r -> service.restorePhoto(r.pathParam("id")).map("ok"));
        router.get("/photos/:id/content").handler(r -> service.findPhoto(r.pathParam("id"))
            .onSuccess(photo -> r.response().sendFile(photo.sourcePath()))
            .onFailure(error -> r.response().setStatusCode(404).end()));
        router.get("/photos/:id/thumbnail").handler(r -> service.findPhoto(r.pathParam("id"))
            .onSuccess(photo -> r.response().sendFile(photo.sourcePath()))
            .onFailure(error -> r.response().setStatusCode(404).end()));

        router.get("/trash/photos").respond(r -> service.findTrash());
        router.post("/trash/:id/restore").respond(r -> service.restorePhoto(r.pathParam("id")).map("ok"));
        router.delete("/trash/:id").respond(r -> service.permanentlyDeletePhoto(r.pathParam("id")));

        router.get("/smart-albums").respond(r -> service.findSmartAlbums());
        router.post("/smart-albums").respond(r -> service.saveSmartAlbum(r.body().asPojo(SmartAlbum.class)));
        router.put("/smart-albums/:id").respond(r -> {
            var incoming = r.body().asPojo(SmartAlbum.class);
            return service.saveSmartAlbum(new SmartAlbum(
                r.pathParam("id"), incoming.name(), incoming.matchType(), incoming.rulesJson(),
                incoming.enabled(), incoming.createTime(), incoming.updateTime()
            ));
        });
        router.delete("/smart-albums/:id").respond(r -> service.deleteSmartAlbum(r.pathParam("id")));
        router.get("/smart-albums/:id/photos").respond(r ->
            service.findSmartAlbumPhotos(r.pathParam("id")));

        router.post("/backups").handler(r -> {
            var body = r.body().asJsonObject();
            var jobId = java.util.UUID.randomUUID().toString();
            var job = service.startBackupJob(
                jobId,
                body.getString("albumId"),
                body.getString("sourceDirectory")
            );
            r.response().setStatusCode(202);
            r.json(job);
        });

        router.post("/backups/:jobId/items").handler(r -> {
            var albumId = r.request().getParam("albumId");
            var fileName = r.request().getParam("fileName");
            var relativePath = r.request().getParam("relativePath");
            var sha256 = r.request().getParam("sha256");
            var uploads = r.fileUploads();
            if (albumId == null || uploads.isEmpty()) {
                r.response().setStatusCode(400).end("albumId and file are required");
                return;
            }
            var uploaded = uploads.getFirst();
            var job = service.backupItem(r.pathParam("jobId"), albumId,
                    java.nio.file.Path.of(uploaded.uploadedFileName()),
                    fileName == null ? new File(uploaded.fileName()).getName() : fileName,
                    relativePath, sha256);
            r.response().setStatusCode(202);
            r.json(job);
        });

        router.post("/backups/:jobId/finish").handler(r -> {
            var job = service.finishBackupJob(r.pathParam("jobId"));
            if (job == null) r.response().setStatusCode(404).end();
            else r.json(job);
        });

        router.get("/backups/:jobId").handler(r -> {
            var job = service.backupJob(r.pathParam("jobId"));
            if (job == null) r.response().setStatusCode(404).end();
            else r.json(job);
        });

        var rootRouter = Router.router(vertx);
        rootRouter.route("/photo/*").subRouter(router);
        return rootRouter;
    }

    private static int parsePage(String value) {
        return value == null ? 1 : Integer.parseInt(value);
    }

    private static int parsePageSize(String value) {
        return value == null ? 10 : Integer.parseInt(value);
    }
}
