package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import org.junit.jupiter.api.Test;

class NetworkUtilsTest extends BaseTest {

    @Override
    protected void init() throws Exception {
        super.init();
    }

    @Override
    protected Tool instance() {
        return new NetworkUtils();
    }

    @Test
    void runWithDNS() throws Exception {
        instance().run(args.arg("util", "dns").arg("in", "localhost"));
    }

    @Test
    void runWithIpInfo() throws Exception {
        instance().run(args.arg("util", "ip").arg("in", "39.156.66.10"));
    }

    @Test
    void runWithMyIpInfo() throws Exception {
        instance().run(args.arg("util", "ip").arg("in", "my"));
    }
}