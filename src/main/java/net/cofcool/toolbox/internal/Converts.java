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
        var arg = args.readArg("cmd").get();

        var pipelineArgs = Arrays.asList(arg.val().split("\\|")).iterator();
        if (!pipelineArgs.hasNext()) {
            throw new IllegalArgumentException("Command args not be found");
        }

        String ret = runCommand(pipelineArgs.next(), null);
        while (pipelineArgs.hasNext()) {
           ret = runCommand(pipelineArgs.next(), ret);
        }

        System.out.println(ret);
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

        return pipelineCmd.run(ret == null ? val : ret + (val == null ? "" : " " + val));
    }

    private interface Pipeline {

        String run(String args) throws Exception;

        String demo();

    }

    private class Timesp implements Pipeline {

        @Override
        public String run(String args) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .parse(args, LocalDateTime::from).atZone(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli() + "";
        }

        @Override
        public String demo() {
            return "2011-11-11 11:11:11.123";
        }
    }

    private class Replace implements Pipeline {

        @Override
        public String run(String args) {
            String[] split = args.split(" ");
            return split[0].replace(split[1], split[2]);
        }

        @Override
        public String demo() {
            return "test . _";
        }
    }

    private class Now implements Pipeline {

        @Override
        public String run(String args) throws Exception {
            return System.currentTimeMillis() + "";
        }

        @Override
        public String demo() {
            return "";
        }
    }

    private class Upper implements Pipeline {

        @Override
        public String run(String args) throws Exception {
            return args.toUpperCase();
        }

        @Override
        public String demo() {
            return "test";
        }
    }

    private class Lower implements Pipeline {

        @Override
        public String run(String args) throws Exception {
            return args.toLowerCase();
        }

        @Override
        public String demo() {
            return "test";
        }
    }

    private class Hdate implements Pipeline {

        @Override
        public String run(String args) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(args)), TimeZone.getDefault().toZoneId()));
        }

        @Override
        public String demo() {
            return "1231312321";
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
        public String demo() {
            return "demo";
        }
    }


    @Override
    public String help() {
        return String.format(
                """
                --cmd=xxx
                commands:
                %s
                """,
                pipelineMap.entrySet().stream()
                        .map(a -> "* " + a.getKey() + " " + a.getValue().demo())
                        .collect(Collectors.joining("\n"))
        );
    }

}
