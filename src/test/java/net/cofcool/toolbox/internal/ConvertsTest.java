package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConvertsTest extends BaseTest {
    
    @Test
    void runPipeline() throws Exception {
        instance().run(args.arg("cmd", "random 10 | md5 | lower | base64 en | replace = ."));
    }

    @Test
    void run() throws Exception {
        System.out.println(instance().config());
        instance().run(args.arg("cmd", "md5 xxxxx"));
        instance().run(args.arg("cmd", "hdate " + System.currentTimeMillis()));
        instance().run(args.arg("cmd", "timesp 2023-01-12 11:54:45.256"));
        instance().run(args.arg("cmd", "upper asdasd"));
        instance().run(args.arg("cmd", "lower ASSS"));
        instance().run(args.arg("cmd", "now"));
        instance().run(args.arg("cmd", "replace asd.asda.asda . _"));
        instance().run(args.arg("cmd", "base64 en demo"));
        instance().run(args.arg("cmd", "base64 de ZGVtbw=="));
        instance().run(args.arg("cmd", "url en 测试"));
        instance().run(args.arg("cmd", "url de %E6%B5%8B%E8%AF%95"));
        instance().run(args.arg("cmd", "random 10"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> instance().run(args.arg("cmd", "base64 urlen adasd.com/%4asd;")));
    }

    @Override
    protected Tool instance() {
        return new Converts();
    }
}