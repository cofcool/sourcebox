package net.cofcool.toolbox.internal;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import net.cofcool.toolbox.BaseTest;
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
                        .arg("cmd", "url")
                        .arg("utype", "en")
                        .arg("in", "测试")
                )
        );
        Assertions.assertEquals(
            "测试",
            instance()
                .runCommand(
                    args
                        .arg("cmd", "url")
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
    void exception() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> instance().run(args.arg("cmd", "base64 urlen adasd.com/%4asd;")));
    }

    @Override
    protected Converts instance() {
        return new Converts();
    }
}