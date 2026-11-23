package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import net.cofcool.sourcebox.internal.api.entity.PhotoAlbum;
import net.cofcool.sourcebox.internal.api.entity.SmartAlbum;
import net.cofcool.sourcebox.util.VertxUtils;

import java.io.File;
import java.nio.file.Files;

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
            service.backup(albumId, java.nio.file.Path.of(uploaded.uploadedFileName()),
                    fileName == null ? new File(uploaded.fileName()).getName() : fileName,
                    relativePath, sha256)
                .onSuccess(photo -> r.json(photo))
                .onFailure(error -> r.response().setStatusCode(400).end(error.getMessage()));
        });

        var rootRouter = Router.router(vertx);
        rootRouter.route("/photo/*").subRouter(router);
        return rootRouter;
    }
}
