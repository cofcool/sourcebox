package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Test;

class ConvertsTest {

    final Converts converts = new Converts();

    @Test
    void runPipeline() throws Exception {
        converts.run(new Args().arg("cmd", "md5 xxxxx | upper | replace B a | lower"));
    }

    @Test
    void run() throws Exception {
        System.out.println(converts.help());
        converts.run(new Args().arg("cmd", "md5 xxxxx"));
        converts.run(new Args().arg("cmd", "hdate " + System.currentTimeMillis()));
        converts.run(new Args().arg("cmd", "timesp 2023-01-12 11:54:45.256"));
        converts.run(new Args().arg("cmd", "upper asdasd"));
        converts.run(new Args().arg("cmd", "lower ASSS"));
        converts.run(new Args().arg("cmd", "now"));
        converts.run(new Args().arg("cmd", "replace asd.asda.asda . _"));
    }
}