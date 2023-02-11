package net.cofcool.toolbox;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Tool.Arg;
import net.cofcool.toolbox.Tool.Args;
import org.apache.commons.io.IOUtils;


@SuppressWarnings({"unchecked", "ConstantConditions"})
public class App {

    public static String ABOUT;

    static final Set<Tool> ALL_TOOLS = new HashSet<>();

    private static final String VERSION_TXT = "/version.txt";

    static {
        try {
            ABOUT = IOUtils.toString(App.class.getResourceAsStream(VERSION_TXT), StandardCharsets.UTF_8);
            for (ToolName tool : ToolName.values()) {
                cacheClass(tool.getTool());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void cacheClass(Class<? extends Tool> type) throws Exception {
        Constructor<Tool> constructor = (Constructor<Tool>) type.getConstructor();
        ALL_TOOLS.add(constructor.newInstance());
    }

    public static void main(String[] args) {
        var pArgs = new Tool.Args(args).setupConfig(
            new Args()
                .arg(new Arg("debug", "false", "", false, null))
                .arg(new Arg("tool", null, "", false, "converts"))
        );
        LoggerFactory.setDebug("true".equalsIgnoreCase(pArgs.readArg("debug").val()));

        var logger = LoggerFactory.getLogger(App.class);
        var notRun = new AtomicBoolean(true);
        pArgs.readArg("tool").ifPresent(a -> {
            for (Tool tool : ALL_TOOLS) {
                if (tool.name().name().equals(a.val())) {
                    notRun.set(false);
                    logger.info("Start run " + tool.name());
                    try {
                        tool.run(pArgs.setupConfig(tool.config()));
                    } catch (Throwable e) {
                        logger.error(e);
                        logger.info("Help");
                        logger.info(tool.config());
                    }
                }
            }
        });
        if (notRun.get()) {
            logger.error("Please check tool argument");
            logAbout(pArgs, logger);
        }
    }

    private static void logAbout(Args pArgs, Logger logger) {
        logger.info("About: " + ABOUT);
        logger.info("Example: --tool=demo --path=tmp");
        logger.info("Tools:\n    " + ALL_TOOLS.stream().map(Tool::name).map(ToolName::toString).collect(Collectors.joining("\n    ")));
        if (LoggerFactory.isDebug()) {
            logger.info("Args: ");
            logger.info(pArgs);
        }
    }
}
