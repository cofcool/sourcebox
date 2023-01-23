package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class JsonToPojo implements Tool {

    @Override
    public ToolName name() {
        return ToolName.json2POJO;
    }

    @Override
    public void run(Args args) throws Exception {
        String json;
        var path = args.readArg("path");
        if (path.isPresent()) {
            json = FileUtils.readFileToString(new File(path.get().val()), StandardCharsets.UTF_8);
        } else {
            json = args.readArg("json").get().val();
        }

        var result = new LinkedHashMap<String, Object>();
        parse(result, Type.INIT, json, 0);

        var pkg = args.readArg("pkg").orElse(new Arg("", "demo")).val();
        var clean = args.readArg("clean").orElse(new Arg("", "true")).val();
        var root = args.readArg("root").orElse(new Arg("", "Root")).val();
        var out = args.readArg("out").orElse(new Arg("", "./pojo")).val();
        var langStr = args.readArg("lang").orElse(new Arg("", Lang.JAVA_RECORD.lang)).val();
        var verStr = args.readArg("ver").orElse(new Arg("", Lang.JAVA_RECORD.ver)).val();
        var lang = Lang.parse(langStr, verStr);

        writeClass(root, result, pkg, out, lang, Boolean.parseBoolean(clean));
        getLogger().debug(result);
    }

    @SuppressWarnings("unchecked")
    private void writeClass(String root, Map<String, Object> result, String pkg, String out, Lang lang, boolean clean) {
        String pathname = out + File.separator + pkg.replace(".", File.separator);
        if (clean) {
            try {
                FileUtils.deleteDirectory(new File(pathname));
            } catch (IOException e) {
                throw new IllegalStateException("Delete " + pathname + " error", e);
            }
        }
        var contents = new HashSet<String>();
        result.forEach((k, v) -> {
            var className = String.valueOf(k.charAt(0)).toUpperCase() + k.substring(1);
            var type = "Object";
            if (Map.class.isAssignableFrom(v.getClass())) {
                type = className;
                writeClass(className, (Map<String, Object>) v, pkg, out, lang, false);
            } else if (List.class.isAssignableFrom(v.getClass())) {
                type = "List<Object>";
                List<?> list = (List<?>) v;
                if (!list.isEmpty()) {
                    Class<?> aClass = list.get(0).getClass();
                    if (Map.class.isAssignableFrom(aClass)) {
                        writeClass(className, (Map<String, Object>) list.get(0), pkg, out, lang, false);
                        type = "List<" + className + ">";
                    } else if (String.class.isAssignableFrom(aClass)) {
                        type = "List<String>";
                    } else if (Number.class.isAssignableFrom(aClass)) {
                        type = "List<Integer>";
                    }
                }

            }  else if (String.class.isAssignableFrom(v.getClass())) {
                type = "String";
            }  else if (Number.class.isAssignableFrom(v.getClass())) {
                type = "Integer";
            }
            contents.add(type + " " + k);
        });
        writeFile(pkg, lang, pathname, root, contents);
    }

    private void writeFile(String pkg, Lang lang, String pathname, String className, Set<String> contents) {
        try {
            FileUtils.forceMkdirParent(new File(pathname));
            FileUtils.writeStringToFile(
                    new File(pathname + File.separator + lang.fileName(className)),
                    lang.render(pkg, className, contents),
                    StandardCharsets.UTF_8,
                    false
            );
            getLogger().info("Generate class " + className + " ok");
        } catch (IOException e) {
            throw new IllegalStateException("Write " + className + " file error", e);
        }
    }

    private int parse(List<Object> result, Type curType, String json, int curIdx) {
        var val = new StringBuilder();
        var eleType = Type.INIT;
        while (curIdx < json.length()){
            var token = json.charAt(curIdx);
            curIdx++;
            if (Token.WHITE.match(token)) {
                continue;
            }
            if (Token.ARRAY_END.match(token)) {
                if (eleType == Type.NUMBER) {
                    result.add(val.toString());
                }
                return curIdx;
            }
            if (curType == Type.VALUE) {
                if (eleType == Type.INIT && Token.STRING_START.match(token)) {
                    eleType = Type.STRING;
                } else if (eleType == Type.INIT && Token.NUMBER.match(token)) {
                    eleType = Type.NUMBER;
                    val.append(token);
                } else if (eleType == Type.NUMBER && Token.COMMON.match(token)) {
                    result.add(Integer.parseInt(val.toString()));
                    val = new StringBuilder();
                    eleType = Type.NEXT;
                } else if (eleType == Type.STRING && Token.STRING_END.match(token)) {
                    result.add(val.toString());
                    val = new StringBuilder();
                    eleType = Type.NEXT;
                } else if (Token.STRING_END.match(token) && eleType == Type.NEXT) {
                    eleType = Type.STRING;
                } else if (Token.NUMBER.match(token) && eleType == Type.NEXT) {
                    eleType = Type.NUMBER;
                    val.append(token);
                } else if (!(Token.COMMON.match(token))) {
                    val.append(token);
                }
                continue;
            }

            if (Token.OBJ_START.match(token)) {
                var map = new LinkedHashMap<String, Object>();
                curIdx = parse(map, Type.OBJ, json, curIdx);
                result.add(map);
            } else if (Token.STRING_START.match(token) || Token.NUMBER.match(token)) {
                var list = new ArrayList<>();
                curIdx = parse(list, Type.VALUE, json, --curIdx);
                result.addAll(list);
                return curIdx;
            }
        }
        return curIdx;
    }

    private int parse(Map<String, Object> result, Type curType, String json, int curIdx) {
        var key = new StringBuilder();
        var val = new StringBuilder();
        while (curIdx < json.length()){
            var token = json.charAt(curIdx);
            curIdx++;
            if (Token.WHITE.match(token)) {
                continue;
            }
            if (curType != Type.BEFORE_VALUE && Token.OBJ_START.match(token)) {
                curType = Type.OBJ;
                continue;
            }
            if (Token.OBJ_END.match(token)) {
                if (curType == Type.VALUE) {
                    result.put(key.toString(), Integer.parseInt(val.toString()));
                }
                return curIdx;
            }
            if (curType != Type.BEFORE_VALUE && Token.ARRAY_START.match(token)) {
                curType = Type.ARRAY;
                continue;
            }
            if (Token.ARRAY_END.match(token)) {
                return curIdx;
            }

            if ((curType == Type.OBJ || curType == Type.NEXT) && Token.STRING_START.match(token)) {
                curType = Type.KEY;
                continue;
            }
            if (curType == Type.KEY && Token.STRING_END.match(token)) {
               curType = Type.KEY_NEXT;
                continue;
            }

            if (curType == Type.KEY_NEXT && Token.COLON.match(token)) {
                curType = Type.BEFORE_VALUE;
                continue;
            }

            if (curType == Type.INIT && Token.COMMON.match(token)) {
                curType = Type.NEXT;
                continue;
            }

            if (curType == Type.BEFORE_VALUE) {
                if (Token.STRING_START.match(token)) {
                    curType = Type.VALUE;
                    continue;
                }
                if (Token.NUMBER.match(token)) {
                    val.append(token);
                    curType = Type.VALUE;
                    continue;
                }
                if (Token.OBJ_START.match(token)) {
                    var map = new LinkedHashMap<String, Object>();
                    result.put(key.toString(), map);
                    curIdx = parse(map, Type.OBJ, json, curIdx);
                    key = new StringBuilder();
                    val = new StringBuilder();
                    curType = Type.NEXT;
                    continue;
                }
                if (Token.ARRAY_START.match(token)) {
                    var list = new ArrayList<>();
                    result.put(key.toString(), list);
                    curIdx = parse(list, Type.ARRAY, json, curIdx);
                    key = new StringBuilder();
                    val = new StringBuilder();
                    curType = Type.NEXT;
                    continue;
                }
            }

            if (curType == Type.VALUE && Token.STRING_END.match(token)) {
                curType = Type.INIT;
                result.put(key.toString(), val.toString());
                key = new StringBuilder();
                val = new StringBuilder();
                continue;
            }

            if (curType == Type.VALUE && (Token.COMMON.match(token) || Token.OBJ_END.match(token))) {
                curType = Token.OBJ_END.match(token) ? Type.INIT : Type.NEXT;
                result.put(key.toString(), Integer.parseInt(val.toString()));
                key = new StringBuilder();
                val = new StringBuilder();
                continue;
            }

            if (curType == Type.KEY) {
                key.append(token);
            }
            if (curType == Type.VALUE) {
                val.append(token);
            }
        }
        return curIdx;
    }


    @Override
    public String help() {
        return "[--path=demo.json] [--root=Root] [--json=\"{}\"] --out=./pojo [--lang=java] [--ver=17] [--pkg=json.demo] [--clean=true]";
    }


    enum Type {
        INIT, NEXT, OBJ, STRING, NUMBER, ARRAY, KEY, KEY_NEXT, VALUE, END, BEFORE_VALUE
    }

    enum Token {
        OBJ_START("{"),
        OBJ_END("}"),
        ARRAY_START("["),
        ARRAY_END("]"),
        COLON(":"),
        COMMON(","),
        WHITE(" \n"),
        STRING_START("\""),
        STRING_END("\""),
        NUMBER("0123456789");

        private final String id;

        Token(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public boolean match(char val) {
            return id.contains(String.valueOf(val));
        }
    }

    enum Lang {
        JAVA_CLASS("java", "", """
                package %s;
                
                // Generate by CofCool@ToolBox %s
                public class %s {
                    %s
                }
                """),
        JAVA_RECORD("java", "17", """
                package %s;
                
                // Generate by CofCool@ToolBox %s
                public record %s (
                    %s
                ) {
                }
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
            return String.format(template, pkg, JsonToPojo.class.getPackage().getImplementationVersion(), className, fields.stream().map(a -> {
                if (this == JAVA_CLASS) {
                     return "private " + a + ";";
                } else {
                    return a + ",";
                }
            }).collect(Collectors.joining("\n    ")));
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
