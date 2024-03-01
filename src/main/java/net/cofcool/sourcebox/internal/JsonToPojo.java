package net.cofcool.sourcebox.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FileUtils;

public class JsonToPojo implements Tool {

    @Override
    public ToolName name() {
        return ToolName.json2POJO;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(Args args) throws Exception {
        String json;
        var path = args.readArg("path");
        if (path.isPresent()) {
            json = FileUtils.readFileToString(new File(path.val()), StandardCharsets.UTF_8);
        } else if (args.readArg("json").isPresent()) {
            json = args.readArg("json").val();
        } else {
            InputStreamReader in = new InputStreamReader(System.in);
            if (in.ready()) {
                json = new BufferedReader(in).lines().collect(Collectors.joining());
            } else {
                throw new IllegalArgumentException("Can not read json from stdin");
            }
        }

        var pkg = args.readArg("pkg").val();
        var clean = args.readArg("clean").val();
        var root = args.readArg("root").val();
        var out = args.readArg("out").val();
        var langStr = args.readArg("lang").val();
        var verStr = args.readArg("ver").val();
        var lang = Lang.parse(langStr, verStr);
        getLogger().info("Write files to " + out);

        writeClass(args.getContext(), root, JsonUtil.toPojo(json, LinkedHashMap.class), pkg, out, lang, Boolean.parseBoolean(clean));
    }

    @SuppressWarnings("unchecked")
    private void writeClass(ToolContext context, String root, Map<Object, Object> result, String pkg, String out, Lang lang, boolean clean) {
        String pathname = out + File.separator + pkg.replace(".", File.separator);
        if (clean) {
            try {
                FileUtils.deleteDirectory(new File(pathname));
            } catch (IOException e) {
                throw new IllegalStateException("Delete " + pathname + " error", e);
            }
        }
        var contents = new LinkedHashSet<String>();
        result.forEach((k1, v1) -> {
            var v = v1;
            String k;
            if (v1 instanceof ListMap listMap) {
                v = listMap.getData();
                k = listMap.getKey();
            } else {
                k = (String) k1;
            }
            var className = String.valueOf(k.charAt(0)).toUpperCase() + k.substring(1);
            var type = className;
            if (v == null) {
                type = "Object";
            } else if (Map.class.isAssignableFrom(v.getClass())  && !((Map<?, ?>) v).isEmpty()) {
                writeClass(context, className, (Map<Object, Object>) v, pkg, out, lang, false);
            } else if (List.class.isAssignableFrom(v.getClass())) {
                type = "Object";
                List<?> list = (List<?>) v;
                if (!list.isEmpty()) {
                    Class<?> aClass = list.get(0).getClass();
                    if (Map.class.isAssignableFrom(aClass)) {
                        writeClass(context, className, (Map<Object, Object>) list.get(0), pkg, out, lang, false);
                        type = className;
                    } else {
                        type = aClass.getName();
                    }
                }
                type = "java.util.List<" + type + ">";
            } else {
                type = v.getClass().getName();
            }

            contents.add(type.replace("java.lang.", "") + " " + k);
        });
        writeFile(context, pkg, lang, pathname, root, contents);
    }

    private void writeFile(ToolContext context, String pkg, Lang lang, String pathname, String className, Set<String> contents) {
        try {
            context.write(pathname + File.separator + lang.fileName(className), lang.render(pkg, className, contents));
            getLogger().info("Generate class " + className + " ok");
        } catch (Exception e) {
            throw new IllegalStateException("Write " + className + " file error", e);
        }
    }



    private static class ListMap extends LinkedHashMap<Object, Object> {

        private ArrayList<Object> data = new ArrayList<>();
        private String key;

        public ListMap(String key) {
            this.key = key;
        }

        @Override
        public Object put(Object key, Object value) {
            data.add(value);
            return super.put(key, value);
        }

        public ArrayList<Object> getData() {
            return data;
        }

        public String getKey() {
            return key;
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }


    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("path", null, "json file path, if not set path or json, will read from stdin", false, "demo.json"))
            .arg(new Arg("json", null, "json content", false, "'{}'"))
            .arg(new Arg("out", "./pojo", "output path", false, null))
            .arg(new Arg("root", "Root", "root class name", false, null))
            .arg(new Arg("lang", Lang.JAVA_RECORD.getLang(), "language: " + Arrays.stream(Lang.values()).map(l -> l.getLang() + "-" + l.getVer()).collect(Collectors.joining(",")), false, null))
            .arg(new Arg("ver", Lang.JAVA_RECORD.getVer(), "language version", false, null))
            .arg(new Arg("pkg", "demo.json", "generated class package", false, null))
            .arg(new Arg("clean", "true", "delete output directory", false, null))
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.WEB));
    }


    enum Lang {
        JAVA_CLASS("java", "legacy", """
                package %s;
                
                // Generate by %s
                public class %s {
                %s
                }
                """),
        JAVA_RECORD("java", "17", """
                package %s;
                
                // Generate by %s
                public record %s (
                %s
                ) {}
                """);

        private final String lang;
        private final String ver;
        private final String template;

        Lang(String lang, String ver, String template) {
            this.lang = lang;
            this.ver = ver;
            this.template = template;
        }

        public String render(String pkg, String className, Set<String> fields) {
            return String.format(template, pkg, App.ABOUT, className,
                fields.stream()
                    .map(a -> {
                        if (this == JAVA_CLASS) {
                            return "private " + a + ";";
                        } else {
                            return a;
                        }
                    })
                    .map(a -> "    " + a)
                    .collect(Collectors.joining(this == JAVA_CLASS ? "\n" : ",\n"))
            );
        }

        public String fileName(String className) {
            return className + "." + lang;
        }

        public static Lang parse(String lang, String ver) {
            for (Lang value : values()) {
                if (value.lang.equalsIgnoreCase(lang) && value.ver.equalsIgnoreCase(ver)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(lang +  " - " + ver + " error");
        }

        public String getLang() {
            return lang;
        }

        public String getVer() {
            return ver;
        }

        public String getTemplate() {
            return template;
        }
    }
}
