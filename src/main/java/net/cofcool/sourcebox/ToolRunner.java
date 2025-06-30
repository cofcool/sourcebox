package net.cofcool.sourcebox;

import java.net.http.HttpRequest.Builder;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;

public interface ToolRunner {

    String ADDRESS_KEY = "web.addr";
    String PORT_KEY = "web.port";
    String USER_KEY = "web.username";
    String PASSWD_KEY = "web.password";
    int DEFAULT_PORT = 38080;
    String DEFAULT_ADDRESS = "http://localhost";

    boolean run(Args args) throws Exception;

    default String help() {
        return null;
    }

    static void initGlobalConfig() {
        App.setGlobalConfig(ADDRESS_KEY, DEFAULT_ADDRESS);
        App.setGlobalConfig(PORT_KEY, String.valueOf(DEFAULT_PORT));
    }

    static boolean checkLocalAPIServer(String address, String port) {
        if (StringUtils.isBlank(address)) {
            address = DEFAULT_ADDRESS;
        }
        if (StringUtils.isBlank(port)) {
            port = String.valueOf(DEFAULT_PORT);
        }
        App.setGlobalConfig(ADDRESS_KEY, address);
        App.setGlobalConfig(PORT_KEY, port);

        var flag = new AtomicBoolean();
        if (address.contains("127.0.0.1") || address.contains("localhost")) {
            Utils.requestAPI(address, port, "/", List.class, Builder::GET, e -> {
                if (e == null || e instanceof IllegalStateException) {
                    flag.set(true);
                }
            });
        }

        return flag.get();
    }

}
