
package net.cofcool.sourcebox.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import lombok.Getter;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


@CustomLog
public class CodeGenerator implements Tool {


    @Override
    public ToolName name() {
        return ToolName.generate;
    }

    @Override
    public void run(Args args) throws Exception {
        var out = args.readArg("out").val();
        var configPath = args.readArg("config").requiredVal("Config path can not be null");

        var templates = Templates.getTemplate(args.readArg("template").val());
        var ret = templates.generate(FileUtils.readFileToByteArray(new File(configPath)));

        File workDir = Paths.get(out, templates.getPkg().split("\\.")).toFile();
        FileUtils.forceMkdirParent(workDir);
        ret.forEach((k, v) -> {
            var names = k.split("\\.");
            names[names.length - 1] = names[names.length - 1] + ".java";

            try {
                FileUtils.writeStringToFile(FileUtils.getFile(workDir, names), v, StandardCharsets.UTF_8);
                log.info("Write class {0} ok", k);
            } catch (IOException e) {
                throw new RuntimeException("Write class fil error", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadTemplate(String path, Map<String, Object> config) {
        try {
            Class<?> aClass = new TemplateLoader(path).loadClass(FilenameUtils.getBaseName(path));
            var tmp = aClass.getConstructor().newInstance();
            Method method = aClass.getMethod("generate", Map.class);
            return (Map<String, String>) method.invoke(tmp, config);
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("generate error", e);
        }
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("out", "./code-generate", "out directory", false, null))
            .arg(new Arg("template", "spring", "template name (" + Templates.TEMPLATES_MAP.keySet() + ") or class file", false, "./demo.class"))
            .arg(new Arg("config", null, """
                json config file, like: '{"pkg": "com.example"}'
                """, true, "demo.json"))
            .alias("generate", name(), "config", null);
    }

    private static class TemplateLoader extends ClassLoader {

        private final String path;
        private final String name;

        TemplateLoader(String path) {
            this.path = path;
            this.name = FilenameUtils.getBaseName(path);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (this.name.equals(name)) {
                try {
                    var b = FileUtils.readFileToByteArray(new File(path));
                    return defineClass(name, b, 0, b.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException("Read file " + path + " error" , e);
                }
            }

            return super.findClass(name);
        }
    }

    interface Generator {
        Map<String, String> generate(Config config);
    }

    static class Templates {

        private static final Map<String, Templates> TEMPLATES_MAP = Map.of("spring", new Templates(new SpringTemplate()));

        private final Object tp;
        @Getter
        private String pkg;

        private Templates(Object tp) {
            this.tp = tp;
        }

        public static Templates custom(String path) {
            return new Templates(path);
        }

        public static Templates getTemplate(String key) {
            var temp = TEMPLATES_MAP.get(key);
            if (temp == null) {
                temp = custom(key);
            }

            return temp;
        }

        @SuppressWarnings("unchecked")
        public Map<String, String> generate(byte[] config) {
            if (tp instanceof Generator generator) {
                var pojo = JsonUtil.toPojo(config, Config.class);
                pkg = pojo.pkg();
                return generator.generate(pojo);
            } else if (tp instanceof String path) {
                var pojo = JsonUtil.toPojo(config, HashMap.class);
                pkg = (String) pojo.get("pkg");
                return loadTemplate(path, pojo);
            }

            throw new IllegalArgumentException(tp.getClass() + " type error");
        }
    }

    private record SpringTemplate() implements Generator {

        @Override
        public Map<String,String> generate(Config config) {
            var ret = new HashMap<String, String>();
            for (String s : config.entities()) {
                ret.put(
                    s + "Controller",
                    "package " + config.pkg() + ";\n\n"
                    + """
                    import org.springframework.web.bind.annotation.RequestMapping;
                    import org.springframework.web.bind.annotation.RestController;
                    
                    @RestController
                    """
                    + "@RequestMapping(value = \"/" + s.toLowerCase() + "\")\n"
                    + "public class " + s + "Controller {\n\n}"
                );
                ret.put(
                    "service." + s + "Service",
                    "package " + config.pkg() + ".service;\n\n"
                    + """
                    import org.springframework.stereotype.Service;

                    @Service
                    """
                    + "public class " + s + "Service {\n\n}"
                );
            }

            return ret;
        }
    }

    record Config(
        String pkg,
        Set<String> entities
    ) { }

}