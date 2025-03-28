package net.cofcool.sourcebox.internal;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import net.cofcool.sourcebox.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConvertsTest extends BaseTest {
    
    @Test
    void runPipeline() throws Exception {
        Assertions.assertEquals(
            "J_HN",
            instance()
                .runCommand(
                    args
                        .arg("pipeline", "replace")
                        .arg("cmd", "upper")
                        .arg("in", "john")
                        .arg("old", "O")
                        .arg("new", "_")
                )
        );
    }

    @Test
    void md5() throws Exception {
        Assertions.assertEquals(
            "900150983cd24fb0d6963f7d28e17f72",
            instance().runCommand(args.arg("cmd", "md5").arg("in", "abc"))
        );
    }

    @Test
    void hdate() throws Exception {
        var val = ZonedDateTime.now();
        Assertions.assertEquals(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(val),
            instance().runCommand(args.arg("cmd", "hdate").arg("in", val.toInstant().toEpochMilli() + ""))
        );
        Assertions.assertEquals(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(val) + ".000",
            instance().runCommand(args.arg("cmd", "hdate").arg("in", val.toInstant().getEpochSecond() + ""))
        );
    }

    @Test
    void timesp() throws Exception {
        var val = ZonedDateTime.now();
        Assertions.assertEquals(
            val.toInstant().toEpochMilli() + "",
            instance().runCommand(args.arg("cmd", "timesp").arg("in", val.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))))
        );
    }

    @Test
    void upper() throws Exception {
        Assertions.assertEquals(
            "ABC",
            instance().runCommand(args.arg("cmd", "upper").arg("in", "abc"))
        );
    }

    @Test
    void lower() throws Exception {
        Assertions.assertEquals(
            "abc",
            instance().runCommand(args.arg("cmd", "lower").arg("in", "ABC"))
        );
    }

    @Test
    void now() throws Exception {
        System.out.println(instance().runCommand(args.arg("cmd", "now")));
    }

    @Test
    void replace() throws Exception {
        Assertions.assertEquals(
            "1BC",
            instance()
                .runCommand(
                    args
                        .arg("cmd", "replace")
                        .arg("in", "ABC")
                        .arg("old", "A")
                        .arg("new", "1")
                )
        );
    }

    @Test
    void base64() throws Exception {
        Assertions.assertEquals(
            "ZGVtbw==",
            instance()
                .runCommand(
                    args
                        .arg("cmd", "base64")
                        .arg("btype", "en")
                        .arg("in", "demo")
                )
        );
        Assertions.assertEquals(
            "demo",
            instance()
                .runCommand(
                    args
                        .arg("cmd", "base64")
                        .arg("btype", "de")
                        .arg("in", "ZGVtbw==")
                )
        );
    }

    @Test
    void url() throws Exception {
        Assertions.assertEquals(
            "%E6%B5%8B%E8%AF%95",
            instance()
                .runCommand(
                    args
                        .arg("cmd", "curl")
                        .arg("utype", "en")
                        .arg("in", "测试")
                )
        );
        Assertions.assertEquals(
            "测试",
            instance()
                .runCommand(
                    args
                        .arg("cmd", "curl")
                        .arg("utype", "de")
                        .arg("in", "%E6%B5%8B%E8%AF%95")
                )
        );
    }

    @Test
    void random() throws Exception {
        System.out.println(instance().runCommand(args.arg("cmd", "random")));
        System.out.println(instance().runCommand(args.arg("cmd", "random").arg("in", "10")));
    }

    @Test
    void dataUnit() throws Exception {
        System.out.println(instance().runCommand(args.arg("cmd", "dataunit").arg("in", "1131313k")));
        System.out.println(instance().runCommand(args.arg("cmd", "dataunit").arg("in", "113131321")));
    }

    @Test
    void morseCode() throws Exception {
        var en = instance().runCommand(args
            .arg("cmd", "morsecode")
            .arg("in", "i love you")
            .arg("mtype", "en")
        );
        Assertions.assertEquals("..  .-.. --- ...- .  -.-- --- ..-", en);
        var de = instance().runCommand(args
            .arg("cmd", "morsecode")
            .arg("in", ".... . .-.. .-.. ---  .-- --- .-. .-.. -..")
            .arg("mtype", "de")
        );
        Assertions.assertEquals("HELLO WORLD", de);
    }

    @Test
    void hex() throws Exception {
        var en = instance().runCommand(args
            .arg("cmd", "hex")
            .arg("in", "1970")
            .arg("radix", "10")
            .arg("nradix", "2")
        );
        var de = instance().runCommand(args
            .arg("cmd", "hex")
            .arg("in", en)
            .arg("radix", "2")
            .arg("nradix", "10")
        );
        Assertions.assertEquals("1970", de);
    }

    @Test
    void desdeSecurity() throws Exception {
        var en = instance().runCommand(args
            .arg("cmd", "security")
            .arg("in", "12345")
            .arg("stype", "en")
            .arg("key", "1234")
        );
        Assertions.assertEquals("TppeTj5n2iU=", en);
        var de = instance().runCommand(args
            .arg("cmd", "security")
            .arg("in", "TppeTj5n2iU=")
            .arg("stype", "de")
            .arg("key", "1234")
        );
        Assertions.assertEquals("12345", de);
    }

    @Test
    void exception() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> instance().run(args.arg("cmd", "base64 urlen adasd.com/%4asd;")));
    }

    @Override
    protected Converts instance() {
        return new Converts();
    }
}