package net.cofcool.toolbox;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Tool.Arg;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import net.cofcool.toolbox.runner.CLIRunner;
import net.cofcool.toolbox.runner.WebRunner;
import org.apache.commons.io.IOUtils;


@SuppressWarnings({"unchecked", "ConstantConditions"})
public class App {

    public static String ABOUT;

    public static final Set<Tool> ALL_TOOLS = new HashSet<>();

    private static final Map<RunnerType, ToolRunner> RUNNER_MAP = Map.of(
        RunnerType.WEB, new WebRunner(),
        RunnerType.CLI, new CLIRunner()
    );

    static final Args ALIAS = new Args();

    private static final String VERSION_TXT = "/version.txt";

    public static void main(String[] args) {
        var pArgs = new Tool.Args(args)
            .copyAliasFrom(ALIAS)
            .setupConfig(
                new Args()
                    .arg(new Arg("debug", "false", "", false, null))
                    .arg(new Arg("help", null, "", false, null))
                    .arg(new Arg("tool", null, "", false, "converts"))
                    .arg(new Arg("mode", RunnerType.CLI.name(), "interface type", false, null))
            );
        LoggerFactory.setDebug("true".equalsIgnoreCase(pArgs.readArg("debug").val()));

        var logger = LoggerFactory.getLogger(App.class);
        logger.debug("Args:");
        logger.debug(pArgs);

        var notRun = new AtomicBoolean(true);

        pArgs.readArg("help").ifPresent(a -> {
            for (Tool tool : ALL_TOOLS) {
                if (tool.name().name().equals(a.val())) {
                    notRun.set(false);
                    logger.info(tool.config().toHelpString());
                }
            }
        });

        var runner = RUNNER_MAP.get(RunnerType.valueOf(pArgs.readArg("mode").val()));
        try {
            notRun.set(!runner.run(pArgs));
        } catch (Exception e) {
            notRun.set(false);
            logger.error(e);
        }

        if (notRun.get()) {
            logger.error("Please check tool name");
            logAbout(logger);
        }
    }

    static {
        try {
            ABOUT = IOUtils.toString(App.class.getResource(VERSION_TXT), StandardCharsets.UTF_8);
            for (ToolName tool : ToolName.values()) {
                ALIAS.copyAliasFrom(cacheClass(tool.getTool()).config());
            }
        } catch (Exception e) {
            throw new RuntimeException("Init tools error", e);
        }
    }

    private static Tool cacheClass(Class<? extends Tool> type) throws Exception {
        var tool = ((Constructor<Tool>) type.getConstructor()).newInstance();
        ALL_TOOLS.add(tool);
        return tool;
    }

    private static void logAbout(Logger logger) {
        logger.info("About: " + ABOUT);
        logger.info("Example: --tool=demo --path=tmp");
        logger.info("Help: --help={COMMAND}, like: --help=rename");
        logger.info("Interface: --mode={CLI}, support: " + RUNNER_MAP.keySet());
        logger.info("Tools:\n    " + ALL_TOOLS.stream().map(Tool::name).map(ToolName::toString).collect(Collectors.joining("\n    ")));
    }
}
