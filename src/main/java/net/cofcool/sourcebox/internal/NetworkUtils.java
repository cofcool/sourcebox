package net.cofcool.sourcebox.internal;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;

public class NetworkUtils implements Tool {

    private final Map<String, Util> utilMap = Map.of(
        "dns", new DnsLookup(),
        "ip", new IpInfo()
    );

    @Override
    public ToolName name() {
        return ToolName.netUtils;
    }

    @Override
    public void run(Args args) throws Exception {
        var util = args.readArg("util").val();
        var in = args.readArg("in").val();

        args.getContext().write(utilMap.get(util).run(in));
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("util", null, STR."util name, support: \{utilMap.keySet()}, note: ip info from ip-api.com", true, "count"))
            .arg(new Arg("in", null, "input string", true, "localhost"));
    }

    private interface Util {

        String run(String in) throws Exception;
    }

    private record IpInfo() implements Util {

        @Override
        public String run(String in) throws Exception {
            var client = WebClient.create(
                Vertx.vertx(),
                new WebClientOptions()
                    .setDefaultHost("ip-api.com")
                    .setDefaultPort(80)
                    .setConnectTimeout(10_000)
            );
            var resp = client.get(STR."/json/\{in}").send();
            while (!resp.isComplete()) {
                TimeUnit.MILLISECONDS.sleep(500);
            }
            client.close();

            if (resp.failed()) {
                throw new IllegalStateException(resp.cause());
            }

            return resp.result().bodyAsString();
        }
    }

    private record DnsLookup() implements Util {

        @Override
        public String run(String in) throws Exception {
            var client = Vertx.vertx().createDnsClient();
            var f = Future.all(
                client.resolveA(in),
                client.resolveCNAME(in),
                client.resolveMX(in),
                client.resolveTXT(in)
            );

            var i = 0;
            while (i < 4 && !f.isComplete(i)) {
                TimeUnit.MILLISECONDS.sleep(50);
                i++;
            }

            client.close();

            return STR."""
                A: \{f.resultAt(0)}
                CNAME: \{f.resultAt(1)}
                MX: \{f.resultAt(2)}
                TXT: \{f.resultAt(3)}
                """;
        }
    }

}
