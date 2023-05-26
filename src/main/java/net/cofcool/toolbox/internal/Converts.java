package net.cofcool.toolbox.internal;

import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;

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
        getLogger().info(runCommand(args));
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
        getLogger().debug(cmd + " args: " + args + "; ret: " + ret);
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
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(args.readArg(INPUT).val())), TimeZone.getDefault().toZoneId()));
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "format timestamp", false, "1231312321");
        }
    }

    private class Md5 implements Pipeline {

        @Override
        public String run(Args args) throws Exception{
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(args.readArg(INPUT).val().getBytes(StandardCharsets.UTF_8));

            byte[] byteArray = messageDigest.digest();

            StringBuilder md5StrBuff = new StringBuilder();

            for (byte b : byteArray) {
                if (Integer.toHexString(0xFF & b).length() == 1) {
                    md5StrBuff.append("0").append(Integer.toHexString(0xFF & b));
                } else {
                    md5StrBuff.append(Integer.toHexString(0xFF & b));
                }
            }

            return md5StrBuff.toString();
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

    private class Url implements Pipeline {

        @Override
        public String run(Args args) throws Exception{
            var type = args.readArg("utype").val();;
            var val = args.readArg(INPUT).val();;
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

    private class Random implements Pipeline {

        @Override
        public String run(Args args) throws Exception {
            var chars = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
            return RandomGenerator
                .getDefault()
                .ints(Integer.parseInt(args.readArg(INPUT).getVal().orElse("10")), 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining(""));
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "random string with giving length", false, "10");
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
            .arg(new Arg(INPUT, null, "input string", false, ""));
        pipelineMap.keySet().forEach(s -> args.alias(s, name(), "cmd", (before, arg, alias) -> {
            before.put(alias.val(), Arg.of(alias.val(), arg.key()));
            before.put(INPUT, Arg.of(INPUT, arg.val()));
        }));
        return args;
    }

}
