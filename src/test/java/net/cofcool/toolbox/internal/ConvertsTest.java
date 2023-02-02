package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class ConvertsTest extends BaseTest {
    
    @Test
    void runPipeline() throws Exception {
        instance().run(args.arg("cmd", "md5 xxxxx | upper | replace B a | lower"));
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
    }

    @Override
    protected Tool instance() {
        return new Converts();
    }
}