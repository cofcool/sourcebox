package net.cofcool.toolbox;

import java.io.File;
import java.io.IOException;
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
import net.cofcool.toolbox.logging.Logger;
import net.cofcool.toolbox.logging.LoggerFactory;
import net.cofcool.toolbox.runner.CLIRunner;
import net.cofcool.toolbox.runner.WebRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;


@SuppressWarnings({"unchecked", "ConstantConditions"})
public class App {

    public static String ABOUT;

    private static final Set<Tool> ALL_TOOLS = new HashSet<>();

    private static final Map<RunnerType, ToolRunner> RUNNER_MAP = Map.of(
        RunnerType.WEB, new WebRunner(),
        RunnerType.CLI, new CLIRunner()
    );

    static final Args ALIAS = new Args();

    private static final String VERSION_TXT = "/version.txt";

    static String GLOBAL_CFG_DIR = FilenameUtils.concat(System.getProperty("user.home"), ".mytool");
    private static String GLOBAL_CFG;


    public static void main(String[] args) throws Exception {
        var pArgs = new Tool.Args(args)
            .copyAliasFrom(ALIAS)
            .copyConfigFrom(
                new Args()
                    .arg(new Arg("debug", "false", "", false, null))
                    .arg(new Arg("help", null, "", false, null))
                    .arg(new Arg("tool", null, "", false, "converts"))
                    .arg(new Arg("mode", RunnerType.CLI.name(), "interface type", false, null))
            );
        LoggerFactory.setDebug(Boolean.parseBoolean(pArgs.readArg("debug").val()));
        var logger = LoggerFactory.getLogger(App.class);

        pArgs.getArgVal("cfg").ifPresentOrElse(
            a -> GLOBAL_CFG = a,
            () -> GLOBAL_CFG = FilenameUtils.concat(GLOBAL_CFG_DIR, "mytool.cfg")
        );

        var cfg = new File(GLOBAL_CFG);
        if (!cfg.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cfg.getParentFile().mkdirs();

            var dcfg = new HashSet<Arg>();
            for (Tool tool : ALL_TOOLS) {
                var df = tool.defaultConfig(GLOBAL_CFG_DIR);
                if (df != null) {
                    df.forEach((k, v) -> dcfg.add(new Arg(tool.name().name() + "." + k, v.val())));
                }
            }
            try {
                FileUtils.writeLines(cfg, "utf-8", dcfg.stream().map(a -> a.key() + "=" + a.val()).toList());
                logger.debug("Init config file {0}", cfg);
            } catch (IOException e) {
                logger.error("Create " + cfg + " file error", e);
            }
        }
        pArgs.copyConfigFrom(new Args(cfg));

        logger.debug("Args: {0}", pArgs);

        var notRun = new AtomicBoolean(true);

        pArgs.readArg("help").ifPresent(a -> {
            for (Tool tool : ALL_TOOLS) {
                if (tool.name().name().equals(a.val())) {
                    notRun.set(false);
                    logger.info(tool.config().toHelpString());
                }
            }
        });

        try {
            var runner = RUNNER_MAP.get(RunnerType.valueOf(pArgs.readArg("mode").val()));
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

    public static String globalCfgDir(String subPath) {
        return FilenameUtils.concat(GLOBAL_CFG_DIR, subPath);
    }

    private static Tool cacheClass(Class<? extends Tool> type) throws Exception {
        var tool = ((Constructor<Tool>) type.getConstructor()).newInstance();
        ALL_TOOLS.add(tool);
        return tool;
    }

    public static Set<Tool> supportTools(RunnerType type) {
        return ALL_TOOLS.stream().filter(tool -> tool.config().supportsType(type)).collect(Collectors.toSet());
    }

    private static void logAbout(Logger logger) {
        logger.info("About: " + ABOUT);
        logger.info("Example: --tool=demo --path=tmp");
        logger.info("Help: --help='{COMMAND}', like: --help=rename");
        logger.info("Interface: --mode='{CLI}', support: "
            + RUNNER_MAP.entrySet().stream()
            .map(e -> {
                String help = e.getValue().help();
                return e.getKey() + (help != null ? (": arguments: [" + help + "]") : "");
            })
            .collect(Collectors.joining("; ")));
        logger.info("Global config file path: --cfg={0}", GLOBAL_CFG);
        logger.info("Tools:\n    " + ALL_TOOLS.stream().map(Tool::name).map(ToolName::toString).collect(Collectors.joining("\n    ")));
    }
}
