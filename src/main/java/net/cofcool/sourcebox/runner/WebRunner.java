package net.cofcool.sourcebox.runner;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.CustomLog;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.util.VertxUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * config listen port: {@link WebVerticle#PORT_KEY}
 */
@CustomLog
public class WebRunner implements ToolRunner {

    public static final int PORT_VAL =  WebVerticle.PORT_VAL;

    @Override
    public boolean run(Args args) throws Exception {
        Vertx v = Vertx.vertx();
        new WebVerticle(RunnerType.WEB, a -> new WebToolContext()).deploy(v, null, args)
            .onComplete(VertxUtils.logResult(log, e -> v.close()));
        return true;
    }

    @Override
    public String help() {
        return String.join(", ", WebVerticle.USER_KEY,  WebVerticle.PASSWD_KEY,  WebVerticle.PORT_KEY);
    }

    static class WebToolContext implements ToolContext {

        Map<String, String> out = new ConcurrentHashMap<>();

        @Override
        public ToolContext write(String name, String in) {
            if (name == null) {
                name = ToolContext.randomName();
            }
            out.put(name, in);
            return this;
        }

        @Override
        public RunnerType runnerType() {
            return RunnerType.WEB;
        }

        @Override
        public JsonObject toObject() {
            String name;
            if (out.isEmpty()) {
                return JsonObject.of("result", "true");
            } else if (out.size() == 1) {
                name = out.values().toArray(String[]::new)[0];
            } else {
                name = VertxUtils.resourcePath(RandomStringUtils.randomAlphabetic(10) + ".zip");
                try (var zipOut = new ZipOutputStream(new FileOutputStream(name))) {
                    for (Entry<String, String> entry : out.entrySet()) {
                        zipOut.putNextEntry(new ZipEntry(entry.getKey()));
                        IOUtils.write(entry.getValue(), zipOut, StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                    log.error("Write zip file error", e);
                    throw new RuntimeException(e);
                }
                log.info("Generate file {0} ok", name);
            }
            return JsonObject.of("result", name);
        }


    }
}
