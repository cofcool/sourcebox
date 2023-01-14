package net.cofcool.toolbox.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;

public class Converts implements Tool {

    @Override
    public ToolName name() {
        return ToolName.converts;
    }

    @SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
    @Override
    public void run(Args args) throws Exception {
        var arg = args.readArg("cmd").get();
        var split = arg.val().split(" ");
        var cmd = split[0];

        String val = null;
        if (split.length > 1) {
            val = String.join(" ", Arrays.copyOfRange(split, 1, split.length)).trim();
        }

        var ret = switch (cmd) {
            case "md5" -> md5(val);
            case "kindle" -> splitKindleClippings(val);
            case "upper" -> val.toUpperCase();
            case "lower" -> val.toLowerCase();
            case "hdate" -> hdate(val);
            case "timesp" -> timesp(val);
            case "now" -> System.currentTimeMillis() + "";
            case "replace" -> replace(val);
            default -> throw new IllegalArgumentException("do not support " + val);
        };
        System.out.println(ret);
    }

    private String timesp(String val) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").parse(val, LocalDateTime::from).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + "";
    }

    private String replace(String val) {
        String[] split = val.split(" ");
        return split[0].replace(split[1], split[2]);
    }

    private String hdate(String val) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(val)), TimeZone.getDefault().toZoneId()));
    }

    private String md5(String val) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.reset();
        messageDigest.update(val.getBytes(StandardCharsets.UTF_8));

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

    private String splitKindleClippings(String path) {
        return Arrays
            .stream(path.split("=========="))
            .map(s -> {
                String[] split = s.split("\n");
                var bookName = "";
                var content = "";
                if (split.length == 5) {
                    bookName = split[0];
                    content = split[3];
                } else {
                    bookName = split[1];
                    content = split[4];
                }
                return String.format("### %s\n\n%s\n\n", bookName, content);
            })
            .collect(Collectors.joining());
    }

    @Override
    public String help() {
        return """
               --cmd=xxx
               commands:
               * md5 xxx
               * kindle xxxx.txt
               * upper/lower xxx
               * hdate 1231312321
               * timesp 2011-11-11 11:11:11.123
               * now
               * replace test . _
               """;
    }
}
