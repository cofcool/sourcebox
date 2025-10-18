package net.cofcool.sourcebox.internal;

import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.util.DataSize;
import net.cofcool.sourcebox.util.Utils;

@SuppressWarnings({"InnerClassMayBeStatic", "unused"})
public class Converts implements Tool {

    public static final String INPUT = "in";

    private final Map<String, Pipeline> pipelineMap = new LinkedHashMap<>();

    public Converts() {
        for (Class<?> clazz : getClass().getDeclaredClasses()) {
            if (!Modifier.isAbstract(clazz.getModifiers()) && Pipeline.class.isAssignableFrom(clazz)) {
                try {
                    pipelineMap.put(clazz.getSimpleName().toLowerCase(),  (Pipeline)(clazz.getDeclaredConstructor(getClass()).newInstance(this)));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public ToolName name() {
        return ToolName.converts;
    }

    @Override
    public void run(Args args) throws Exception {
        args.getContext().write(runCommand(args));
    }

    protected String runCommand(Args args) throws Exception {
        var pipelines = new ArrayList<String>();
        pipelines.add(args.readArg("cmd").val());

        args.readArg("pipeline").ifPresent(a -> {
            pipelines.addAll(Arrays.stream(a.val().split("\\|")).map(String::trim).toList());
        });

        var cmds = pipelines.iterator();
        var ret = runCommand(cmds.next(), args, null);
        while (cmds.hasNext()) {
            ret = runCommand(cmds.next(), args, ret);
        }

        return ret;
    }

    private String runCommand(String cmd, Args args, String ret) throws Exception {
        var pipelineCmd = pipelineMap.get(cmd);
        if (pipelineCmd == null) {
            throw new IllegalArgumentException("Do not support " + cmd);
        }
        if (ret != null) {
            args.arg(INPUT, ret);
        }
        ret = pipelineCmd.run(args);
        getLogger().debug("{0} args: {1}; ret: {2}", cmd, args, ret);
        return ret;
    }

    private interface Pipeline {

        String run(Args args) throws Exception;

        Arg demo();

    }

    private class Timesp implements Pipeline {

        @Override
        public String run(Args args) {
            return String.valueOf(
                DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .parse(args.readArg(INPUT).val(), LocalDateTime::from)
                    .atZone(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli()
            );
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "covert date to timestamp", false, "2011-11-11 11:11:11.123");
        }
    }

    private class Replace implements Pipeline {

        @Override
        public String run(Args args) {
            return args.readArg(INPUT).val().replace(args.readArg("old").val(), args.readArg("new").val());
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "replace string", false, "test -old=t --new=e");
        }
    }

    private class Now implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            return String.valueOf(System.currentTimeMillis());
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "current timestamp", false, "");
        }
    }

    private class Upper implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            return args.readArg(INPUT).val().toUpperCase();
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "convert string to upper case", false, "test");
        }
    }

    private class Hex implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            var radix = args.readArg("radix").optVal().map(Integer::parseUnsignedInt).orElse(10);
            var newRadix = args.readArg("nradix").optVal().map(Integer::parseUnsignedInt).orElse(2);
            var val = Long.valueOf(args.readArg(INPUT).val(), radix);

            return Long.toString(val, newRadix);
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "number base conversion", false, "1970 --radix=10 --nradix=2");
        }
    }

    private class Lower implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            return args.readArg(INPUT).val().toLowerCase();
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "convert string to lower case", false, "test");
        }
    }

    private class Hdate implements Pipeline {

        @Override
        public String run(Args args) {
            var v = Long.parseLong(args.readArg(INPUT).val());
            Instant instant;
            if (v >= Integer.MAX_VALUE) {
                instant = Instant.ofEpochMilli(v);
            } else {
                instant = Instant.ofEpochSecond(v);
            }
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .format(LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId()));
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "format timestamp", false, "1231312321");
        }
    }

    private class DataUnit implements Pipeline {

        @Override
        public String run(Args args) {
            return DataSize.of(args.readArg(INPUT).val()).toString();
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "display data size for human", false, "1231312321k");
        }
    }

    private class Md5 implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            return Utils.md5(args.readArg(INPUT).val());
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "calculate string md5", false, "demo");
        }
    }

    private class Base64 implements Pipeline {

        @Override
        public String run(Args args) throws Exception{
            var type = args.readArg("btype").val();
            var val = args.readArg(INPUT).val();
            if (type.equalsIgnoreCase("en")) {
                return new String(java.util.Base64.getEncoder().encode(val.getBytes(StandardCharsets.UTF_8)));
            } else if (type.equalsIgnoreCase("de"))  {
                return new String(java.util.Base64.getDecoder().decode(val));
            } else {
                throw new IllegalArgumentException("Base64 first argument must be en or de");
            }
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "base64 encoder(en) or decoder(de)", false, "demo --btype=en/de");
        }
    }

    private class CUrl implements Pipeline {

        @Override
        public String run(Args args) throws Exception{
            var type = args.readArg("utype").val();
            var val = args.readArg(INPUT).val();
            if (type.equalsIgnoreCase("en")) {
                return URLEncoder.encode(val, StandardCharsets.UTF_8);
            } else if (type.equalsIgnoreCase("de"))  {
                return URLDecoder.decode(val, StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException("Url first argument must be en or de");
            }
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "url encoder(en) or decoder(de)", false, "demo --utype=en/de");
        }
    }

    private class MorseCode implements Pipeline {

        private static final Map<Character, String> TABLE = Map.<Character, String>ofEntries(
            Map.entry('A', ".-"),
            Map.entry('B', "-..."),
            Map.entry('C', "-.-."),
            Map.entry('D', "-.."),
            Map.entry('E', "."),
            Map.entry('F', "..-."),
            Map.entry('G', "--."),
            Map.entry('H', "...."),
            Map.entry('I', ".."),
            Map.entry('J', ".---"),
            Map.entry('K', "-.-"),
            Map.entry('L', ".-.."),
            Map.entry('M', "--"),
            Map.entry('N', "-."),
            Map.entry('O', "---"),
            Map.entry('P', ".--."),
            Map.entry('Q', "--.-"),
            Map.entry('R', ".-."),
            Map.entry('S', "..."),
            Map.entry('T', "-"),
            Map.entry('U', "..-"),
            Map.entry('V', "...-"),
            Map.entry('W', ".--"),
            Map.entry('X', "-..-"),
            Map.entry('Y', "-.--"),
            Map.entry('Z', "--.."),
            Map.entry('1', ".----"),
            Map.entry('2', "..---"),
            Map.entry('3', "...--"),
            Map.entry('4', "....-"),
            Map.entry('5', "....."),
            Map.entry('6', "-...."),
            Map.entry('7', "--..."),
            Map.entry('8', "---.."),
            Map.entry('9', "----."),
            Map.entry('0', "-----"),
            Map.entry(',', "--..--"),
            Map.entry(';', "-.-.-."),
            Map.entry(':', "---..."),
            Map.entry('.', ".-.-.-"),
            Map.entry('\'', ".----."),
            Map.entry('"', ".-..-."),
            Map.entry('?', "..--.."),
            Map.entry('/', "-..-."),
            Map.entry('-', "-....-"),
            Map.entry('(', "-.--."),
            Map.entry(')', "-.--.-"),
            Map.entry('!', "-.-.--"),
            Map.entry('$', "...-..-"),
            Map.entry('@', ".--.-."),
            Map.entry('=', "-...-")
        );
        private static final Map<String, String> TABLE_R = new HashMap<>(TABLE.size());

        static {
            TABLE.forEach((k, v) -> TABLE_R.put(v, String.valueOf(k)));
        }

        @Override
        public String run(Args args) throws Exception{
            var type = args.readArg("mtype").val();
            var val = args.readArg(INPUT).val();
            if (type.equalsIgnoreCase("en")) {
                return val.chars().mapToObj(a -> TABLE.getOrDefault(Character.toUpperCase((char) a), "")).collect(Collectors.joining(" "));
            } else if (type.equalsIgnoreCase("de"))  {
                return Arrays.stream(val.split(" {2}"))
                    .map(a -> Arrays
                        .stream(a.split(" "))
                        .map(TABLE_R::get)
                        .collect(Collectors.joining())
                    )
                    .collect(Collectors.joining(" "));
            } else {
                throw new IllegalArgumentException("MorseCode first argument must be en or de");
            }
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "MorseCode encoder(en) or decoder(de), two space split word, single space split letter", false, "demo --mtype=en/de");
        }
    }

    private class Random implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            var chars = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
            return RandomGenerator
                .getDefault()
                .ints(Integer.parseInt(args.readArg(INPUT).optVal().orElse("10")), 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining(""));
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "random string with giving length", false, "10");
        }
    }

    private class Security implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            var type = args.readArg("stype").val();
            var val = args.readArg(INPUT).val();
            var key = args.readArg("key").val();
            if (key.length() < 24) {
                key = "0".repeat(24-key.length()) + key;
            }
            if (type.equalsIgnoreCase("en")) {
                return Utils.desdeEncrypt(key, val);
            } else if (type.equalsIgnoreCase("de"))  {
                return Utils.desdeDecrypt(key, val);
            } else {
                throw new IllegalArgumentException("Security first argument must be en or de");
            }
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "encrypt or decrypt with giving key, default algorithm is Triple DES", false, "demo --stype=en/de --key=test");
        }
    }

    @Override
    public Args config() {
        var args = new Args()
            .arg(new Arg(
                "cmd",
                null,
                "\n" + pipelineMap.values().stream().map(Pipeline::demo).map(a -> "        " + a.key() + ": " + a.desc() + ". Example: --" + INPUT + "=" + a.demo()).collect(Collectors.joining("\n")),
                true,
                ""
            ))
            .arg(new Arg("pipeline", null, "next commands, like: md5 | replace", false, ""))
            .arg(new Arg(INPUT, null, "input string", false, ""))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
        pipelineMap.keySet().forEach(s -> args.alias(s, name(), "cmd", "cmd="+ s + " --in", (before, arg, alias) -> {
            before.put(alias.val(), Arg.of(alias.val(), arg.key()));
            before.put(INPUT, Arg.of(INPUT, arg.val()));
        }));
        return args;
    }

}
