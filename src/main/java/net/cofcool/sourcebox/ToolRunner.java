package net.cofcool.sourcebox;

import net.cofcool.sourcebox.Tool.Args;
import org.apache.commons.lang3.StringUtils;

public interface ToolRunner {

    String ADDRESS_KEY = "web.addr";
    String PORT_KEY = "web.port";
    String USER_KEY = "web.username";
    String PASSWD_KEY = "web.password";
    String WEB_FILE_SHARE_KEY = "web.fileshare";
    int DEFAULT_PORT = 38080;
    String DEFAULT_ADDRESS = "http://localhost";

    default boolean run(Args args, Args globalArgs) throws Exception {
        return run(args.copyConfigFrom(globalArgs));
    }

    boolean run(Args args) throws Exception;

    default String help() {
        return null;
    }

    static void initGlobalConfig() {
        App.setGlobalConfig(ADDRESS_KEY, DEFAULT_ADDRESS);
        App.setGlobalConfig(PORT_KEY, String.valueOf(DEFAULT_PORT));
    }

    static void setupAPIServer(String address, String port) {
        if (StringUtils.isBlank(address)) {
            address = DEFAULT_ADDRESS;
        }
        if (StringUtils.isBlank(port)) {
            port = String.valueOf(DEFAULT_PORT);
        }
        App.setGlobalConfig(ADDRESS_KEY, address);
        App.setGlobalConfig(PORT_KEY, port);
    }

}
