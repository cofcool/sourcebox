package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Test;

class ConvertsTest {

    final Converts converts = new Converts();

    @Test
    void run() throws Exception {
        System.out.println(converts.help());
        converts.run(new Args(4).arg("cmd", "md5 xxxxx"));
        converts.run(new Args(4).arg("cmd", "hdate " + System.currentTimeMillis()));
        converts.run(new Args(4).arg("cmd", "timesp 2023-01-12 11:54:45.256"));
        converts.run(new Args(4).arg("cmd", "upper asdasd"));
        converts.run(new Args(4).arg("cmd", "lower ASSS"));
    }
}