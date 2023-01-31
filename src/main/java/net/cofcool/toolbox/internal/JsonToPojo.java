package net.cofcool.toolbox.internal;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.cofcool.toolbox.App;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
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
            json = FileUtils.readFileToString(new File(path.get().val()), StandardCharsets.UTF_8);
        } else if (args.readArg("json").isPresent()) {
            json = args.readArg("json").get().val();
        } else {
            json = new BufferedReader(new InputStreamReader(System.in)).lines().collect(Collectors.joining());
        }

        var pkg = args.readArg("pkg").orElse(new Arg("", "demo")).val();
        var clean = args.readArg("clean").orElse(new Arg("", "true")).val();
        var root = args.readArg("root").orElse(new Arg("", "Root")).val();
        var out = args.readArg("out").orElse(new Arg("", "./pojo")).val();
        var langStr = args.readArg("lang").orElse(new Arg("", Lang.JAVA_RECORD.lang)).val();
        var verStr = args.readArg("ver").orElse(new Arg("", Lang.JAVA_RECORD.ver)).val();
        var lang = Lang.parse(langStr, verStr);
        getLogger().info("Write files to " + out);

        writeClass(root, new Gson().fromJson(json, LinkedHashMap.class), pkg, out, lang, Boolean.parseBoolean(clean));
    }

    @SuppressWarnings("unchecked")
    private void writeClass(String root, Map<Object, Object> result, String pkg, String out, Lang lang, boolean clean) {
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
                writeClass(className, (Map<Object, Object>) v, pkg, out, lang, false);
            } else if (List.class.isAssignableFrom(v.getClass())) {
                type = "Object";
                List<?> list = (List<?>) v;
                if (!list.isEmpty()) {
                    Class<?> aClass = list.get(0).getClass();
                    if (Map.class.isAssignableFrom(aClass)) {
                        writeClass(className, (Map<Object, Object>) list.get(0), pkg, out, lang, false);
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

    private Node parse(Node cur, Map<Object, Object> result) {
        var key = new StringBuilder();
        var val = new StringBuilder();
        var curType = Type.INIT;
        var eleType = Type.INIT;
        while (cur != null) {
            cur = cur.next;
            if (cur == null) {
                continue;
            }

            Token token = cur.token;
            if (token == Token.WHITE) {
                continue;
            }

            if (curType == Type.INIT && token == Token.OBJ_START) {
                curType = Type.OBJ;
                continue;
            }
            if (curType == Type.INIT && token == Token.ARRAY_START) {
                curType = Type.ARRAY;
                continue;
            }
            if ((curType == Type.OBJ || curType == Type.NEXT) && token == Token.DOUBLE_QUOTES) {
                curType = Type.KEY;
                continue;
            }
            if (curType == Type.KEY && token == Token.DOUBLE_QUOTES) {
                curType = Type.KEY_NEXT;
                continue;
            }
            if (curType == Type.KEY_NEXT && token == Token.COLON) {
                curType = Type.BEFORE_VALUE;
                continue;
            }
            if (curType == Type.KEY) {
                key.append(cur.data());
            }
            if (curType == Type.ARRAY) {
                if (token == Token.OBJ_START) {
                    var map = new LinkedHashMap<>();
                    result.put(key.toString(), map);
                    cur = parse(cur.prev, map);
                    key = new StringBuilder();
                    val = new StringBuilder();
                    eleType = Type.OBJ;
                    continue;
                }
                if (token == Token.DOUBLE_QUOTES) {
                    if (eleType == Type.INIT) eleType = Type.STRING;
                    continue;
                }
                if (token == Token.COMMON) {
                    if (cur.prev.token == Token.OBJ_END) {
                        continue;
                    }
                    result.put(key.toString(), toValue(key, eleType));
                    key = new StringBuilder();
                    val = new StringBuilder();
                    eleType = Type.INIT;
                    continue;
                }
                if (token == Token.ARRAY_END) {
                    if (eleType != Type.INIT) {
                        result.put(key.toString(), toValue(key, eleType));
                    }
                    return cur;
                }
                key.append(cur.data());
                continue;
            }
            if (curType == Type.BEFORE_VALUE) {
                if (token == Token.DOUBLE_QUOTES) {
                    eleType = Type.STRING;
                    continue;
                }
                if (token == Token.OBJ_START || token == Token.ARRAY_START) {
                    var map = token == Token.ARRAY_START ? new ListMap(key.toString()) : new LinkedHashMap<>();
                    result.put(key.toString(), map);
                    cur = parse(cur.prev, map);
                    key = new StringBuilder();
                    val = new StringBuilder();
                    curType = Type.NEXT;
                    eleType = token == Token.OBJ_START ? Type.OBJ : Type.ARRAY;
                    continue;
                }

                curType = Type.VALUE;
                val.append(cur.data());
                continue;
            }

            if (curType == Type.VALUE) {
                if (token == Token.DOUBLE_QUOTES) {
                    continue;
                }
                if (token == Token.COMMON || token == Token.OBJ_END) {
                    curType = Type.NEXT;
                    Object v = toValue(val, eleType);
                    result.put(key.toString(), v);
                    key = new StringBuilder();
                    val = new StringBuilder();
                    if (token == Token.OBJ_END) {
                        return cur;
                    }
                    continue;
                }
                eleType = Type.INIT;
                val.append(cur.data());
            }

        }

        return cur;
    }

    private static Object toValue(StringBuilder val, Type eleType) {
        Object v = val.toString();
        if (eleType != Type.STRING && eleType != Type.OBJ && eleType != Type.ARRAY) {
            try {
                v = Long.parseLong(val.toString());
                if ((Long) v < Integer.MAX_VALUE) {
                    v = ((Long) v).intValue();
                }
            } catch (NumberFormatException e) {
                v = "true".equalsIgnoreCase(val.toString()) || "false".equalsIgnoreCase(val.toString()) ? Boolean.parseBoolean(val.toString()) : val.toString();
            }
        }
        return v;
    }

    private Node parse(String json) {
        var curIdx = 0;
        var length = 0;
        Node root = new Node();
        Node cur = root;
        while (curIdx < json.length()) {
            var token = json.charAt(curIdx);
            curIdx++;

            var ret = cur.checkToken(token);
            if (ret.next) {
                length++;
                if (ret.node != cur) {
                    cur.next = ret.node;
                    ret.node.prev = cur;
                    cur = ret.node;
                } else {
                    var node = new Node();
                    cur.next = node;
                    node.prev = cur;
                    cur = node;
                }
            } else {
                cur.next = ret.node;
                ret.node.prev = cur;
                cur = ret.node;
            }
        }

        root.length = length;
        return root;
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

    private static class Node {
        StringBuilder data = new StringBuilder();
        Token token;
        Node next;
        Node prev;
        int length;

        @Override
        public String toString() {
            return "Node{" +
                    "data=" + data +
                    ", token=" + token +
                    ", next=" + next.token +
                    '}';
        }

        Node appendToken(char val) {
            data.append(val);
            return this;
        }

        Node token(Token token) {
            this.token = token;
            return this;
        }

        public String data() {
            return data.toString();
        }

        CheckResult checkToken(char val) {
            if (token != null) {
                var ret = parse(val);
                if (ret != null) {
                    return new CheckResult(ret, true);
                } else {
                    return new CheckResult(new Node().appendToken(val), false);
                }
            }

            Node parse = parse(val);
            if (parse != null) {
                return new CheckResult(parse, true);
            }

            data.append(val);

            Optional<Token> fullParse = Token.parse(data.toString());
            if (fullParse.isPresent()) {
                token = fullParse.get();
                return new CheckResult(this, true);
            }

            return new CheckResult(this, false);
        }

        static Node parse(char val) {
            return Token.parse(String.valueOf(val))
                    .map(value -> new Node().token(value).appendToken(val))
                    .orElse(null);
        }
    }

    record CheckResult(
            Node node,
            boolean next
    ) {}


    @Override
    public String help() {
        return "[--path=demo.json] [--root=Root] [--json=\"{}\"] [--out=./pojo] [--lang=java] [--ver=17] [--pkg=json.demo] [--clean=true]";
    }


    enum Type {
        INIT, NEXT, OBJ, ARRAY, STRING, NUMBER, BOOLEAN, KEY, KEY_NEXT, VALUE, END, BEFORE_VALUE
    }

    enum Token {
        OBJ_START(new String[]{"{"}),
        OBJ_END(new String[]{"}"}),
        ARRAY_START(new String[]{"["}),
        ARRAY_END(new String[]{"]"}),
        COLON(new String[]{":"}),
        COMMON(new String[]{","}),
        WHITE(new String[]{" ", "\n", "\t"}),
        DOUBLE_QUOTES(new String[]{"\""}),
        BOOLEAN(new String[]{"true", "false"});

        private final String[] tokens;

        Token(String[] tokens) {
            this.tokens = tokens;
        }

        public String[] getTokens() {
            return tokens;
        }

        public boolean match(char val) {
            return match(String.valueOf(val));
        }

        public boolean match(String val) {
            return Arrays.asList(tokens).contains(val);
        }

        public static Optional<Token> parse(String token) {
            for (Token value : Token.values()) {
                if (value.match(token)) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    enum Lang {
        JAVA_CLASS("java", "", """
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
