package net.cofcool.toolbox.internal;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;

@SuppressWarnings("ALL")
public class Converts implements Tool {

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

    @SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
    @Override
    public void run(Args args) throws Exception {
        var arg = args.readArg("cmd");

        var pipelineArgs = Arrays.asList(arg.val().split("\\|")).iterator();
        if (!pipelineArgs.hasNext()) {
            throw new IllegalArgumentException("Command args not be found");
        }

        String ret = runCommand(pipelineArgs.next(), null);
        while (pipelineArgs.hasNext()) {
           ret = runCommand(pipelineArgs.next(), ret);
        }

        getLogger().info(ret);
    }

    private String runCommand(String arg, String ret) throws Exception {
        var split = arg.trim().split(" ");
        var cmd = split[0];

        String val = null;
        if (split.length > 1) {
            val = String.join(" ", Arrays.copyOfRange(split, 1, split.length)).trim();
        }

        Pipeline pipelineCmd = pipelineMap.get(cmd);
        if (pipelineCmd == null) {
            throw new IllegalArgumentException("Do not support " + cmd);
        }

        String args = ret == null ? val : (val == null ? "" : val + " ") + ret;
        getLogger().debug(cmd + " args: " + args);
        return pipelineCmd.run(args);
    }

    private interface Pipeline {

        String run(String args) throws Exception;

        Arg demo();

    }

    private class Timesp implements Pipeline {

        @Override
        public String run(String args) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .parse(args, LocalDateTime::from).atZone(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli() + "";
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "covert date to timestamp", false, "2011-11-11 11:11:11.123");
        }
    }

    private class Replace implements Pipeline {

        @Override
        public String run(String args) {
            String[] split = args.split(" ");
            return split[2].replace(split[0], split[1]);
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "replace string", false, ". _ test");
        }
    }

    private class Now implements Pipeline {

        @Override
        public String run(String args) throws Exception {
            return System.currentTimeMillis() + "";
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "current timestamp", false, "");
        }
    }

    private class Upper implements Pipeline {

        @Override
        public String run(String args) throws Exception {
            return args.toUpperCase();
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "convert string to upper case", false, "test");
        }
    }

    private class Lower implements Pipeline {

        @Override
        public String run(String args) throws Exception {
            return args.toLowerCase();
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "convert string to lower case", false, "test");
        }
    }

    private class Hdate implements Pipeline {

        @Override
        public String run(String args) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(args)), TimeZone.getDefault().toZoneId()));
        }

        @Override
        public Arg demo() {
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "format timestamp", false, "1231312321");
        }
    }

    private class Md5 implements Pipeline {

        @Override
        public String run(String args) throws Exception{
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(args.getBytes(StandardCharsets.UTF_8));

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
        public String run(String args) throws Exception{
            String[] split = args.split(" ");
            if (split.length != 2) {
                throw new IllegalArgumentException("Base64 arguments size must be 2");
            }
            var type = split[0];
            var val = split[1];
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
            return new Arg(getClass().getSimpleName().toLowerCase(), null, "base64 encoder(en) or decoder(de)", false, "en/de demo");
        }
    }

    private class Random implements Pipeline {

        @Override
        public String run(String args) throws Exception {
            var chars = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
            return RandomGenerator
                .getDefault()
                .ints(Integer.parseInt(args), 0, chars.length())
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
        return new Args()
            .arg(new Arg(
                "cmd",
                null,
                "\n" + pipelineMap.values().stream().map(Pipeline::demo).map(a -> "        " + a.key() + ": " + a.desc() + ". Example: " + a.key() + " " + a.demo()).collect(Collectors.joining("\n")),
                true,
                ""
            ));
    }

}
