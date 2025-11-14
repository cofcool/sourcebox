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
            .arg(new Arg("util", null, String.format("util name, support: %s, note: ip info from ip-api.com", utilMap.keySet()), true, "count"))
            .arg(new Arg("in", null, "input string, if the in is 'my' will get current public ip", true, "localhost"));
    }

    private interface Util {

        String run(String in) throws Exception;
    }

    private record IpInfo() implements Util {

        @Override
        public String run(String in) throws Exception {
            var vertx = Vertx.vertx();
            var client = WebClient.create(
                vertx,
                new WebClientOptions()
                    .setDefaultHost("ip-api.com")
                    .setDefaultPort(80)
                    .setConnectTimeout(10_000)
            );
            var url = switch (in) {
                case "my" -> "/json";
                default -> "/json/" + in;
            };
            var resp = client.get(url).send();
            while (!resp.isComplete()) {
                TimeUnit.MILLISECONDS.sleep(500);
            }
            client.close();
            vertx.close();

            if (resp.failed()) {
                throw new IllegalStateException(resp.cause());
            }

            return resp.result().bodyAsString();
        }
    }

    private record DnsLookup() implements Util {

        @Override
        public String run(String in) throws Exception {
            var vertx = Vertx.vertx();
            var client = vertx.createDnsClient();
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
            vertx.close();

            return String.format("""
                A: %s
                CNAME:%s
                MX: %s
                TXT: %s
                """, f.resultAt(0), f.resultAt(1), f.resultAt(2), f.resultAt(3)
            );
        }
    }

}
