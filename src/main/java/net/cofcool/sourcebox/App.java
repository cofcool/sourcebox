package net.cofcool.sourcebox;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.cofcool.sourcebox.Tool.Arg;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.logging.ConsoleLogger;
import net.cofcool.sourcebox.logging.Logger;
import net.cofcool.sourcebox.logging.LoggerFactory;
import net.cofcool.sourcebox.runner.CLIRunner;
import net.cofcool.sourcebox.runner.GUIRunner;
import net.cofcool.sourcebox.runner.WebRunner;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


@SuppressWarnings({"unchecked", "ConstantConditions"})
public class App {

    static final String ZIP_FILE = "sourcebox-config.zip";
    public static String ABOUT;

    private static final Set<Tool> ALL_TOOLS = new HashSet<>();

    private static final Map<RunnerType, ToolRunner> RUNNER_MAP = Map.of(
        RunnerType.WEB, new WebRunner(),
        RunnerType.CLI, new CLIRunner(),
        RunnerType.GUI, new GUIRunner()
    );

    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    static final Args ALIAS = new Args();

    static String GLOBAL_CFG_DIR = FilenameUtils.concat(System.getProperty("user.home"), ".mytool");
    private static String GLOBAL_CFG;

    public static Optional<Object> getOpGlobalConfig(String key) {
        return Optional.ofNullable(OBJECT_MAP.get(key));
    }

    public static Object getGlobalConfig(String key) {
        return OBJECT_MAP.get(key);
    }

    public static void setGlobalConfig(String key, Object obj) {
        OBJECT_MAP.put(key, obj);
    }


    public static void main(String[] args) throws Exception {
        var pArgs = new Tool.Args(args)
            .copyAliasFrom(ALIAS)
            .copyConfigFrom(
                new Args()
                    .arg(new Arg("debug", "false", "", false, null))
                    .arg(new Arg("archive", null, "archive config", false, "true"))
                    .arg(new Arg("help", null, "", false, null))
                    .arg(new Arg("tool", null, "", false, "converts"))
                    .arg(new Arg("defaultConfig", null, "", false, ""))
                    .arg(new Arg("mode", RunnerType.CLI.name(), "interface type", false, null))
            );
        LoggerFactory.setDebug(Boolean.parseBoolean(pArgs.readArg("debug").val()));
        var logger = new ConsoleLogger(App.class);

        pArgs.getArgVal("cfg").ifPresentOrElse(
            a -> GLOBAL_CFG = a,
            () -> GLOBAL_CFG = FilenameUtils.concat(GLOBAL_CFG_DIR, "mytool.cfg")
        );

        var cfg = new File(GLOBAL_CFG);
        if (!cfg.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cfg.getParentFile().mkdirs();

            var dcfg = new HashSet<Arg>();
            pArgs.readArg("defaultConfig").ifPresent(i -> {
                for (Tool tool : ALL_TOOLS) {
                    tool.config()
                        .forEach((k, v) -> dcfg.add(new Arg(tool.name().name() + "." + k, v.val())));
                }
            });

            try {
                FileUtils.writeLines(cfg, "utf-8", dcfg.stream().map(a -> a.key() + "=" + a.val()).toList());
            } catch (IOException e) {
                logger.error("Create " + cfg + " file error", e);
            }

            if (!dcfg.isEmpty()) {
                logger.info("Generate default config file {0}", cfg);
                return;
            }
        }
        pArgs.copyConfigFrom(new Args(cfg));

        logger.debug("Args: {0}", pArgs);

        var help = pArgs.readArg("help");
        if (help.isPresent()) {
            for (Tool tool : ALL_TOOLS) {
                if (tool.name().name().equals(help.val())) {
                    logger.info(tool.config().toHelpString());
                }
            }
            return;
        }

        var archive = pArgs.readArg("archive");
        if (archive.isPresent() && archive.test(a -> a.equalsIgnoreCase("true"))) {
            Utils.zipDir(GLOBAL_CFG_DIR, ZIP_FILE);
            logger.info("Create archive file {0} ok", ZIP_FILE);
            return;
        }

        var notRun = new AtomicBoolean(true);
        try {
            var mode = pArgs.readArg("mode").val();
            var runner = RUNNER_MAP.get(RunnerType.valueOf(mode));
            if (runner == null) {
                throw new IllegalArgumentException("Unknown mode: " + mode);
            }
            ToolRunner.initGlobalConfig();
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
            ABOUT = "CofCool@TheSourceBox " + App.class.getPackage().getImplementationVersion();
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

    public static ToolRunner getRunner(RunnerType runnerType) {
        return RUNNER_MAP.get(runnerType);
    }

    private static Tool cacheClass(Class<? extends Tool> type) throws Exception {
        var tool = ((Constructor<Tool>) type.getConstructor()).newInstance();
        ALL_TOOLS.add(tool);
        return tool;
    }

    public static Set<Tool> supportTools(RunnerType type) {
        return ALL_TOOLS.stream().filter(tool -> tool.config().supportsType(type)).collect(Collectors.toSet());
    }

    public static Optional<Tool> getTool(String name) {
        return ALL_TOOLS.stream().filter(tool -> tool.name().name().equals(name)).findAny();
    }

    private static void logAbout(Logger logger) {
        logger.info(STR."About: \{ABOUT}");
        logger.info("Example: --tool=demo --path=tmp");
        logger.info("Help: --help='{COMMAND}', like: --help=rename");
        logger.info("Archive: --archive=true, archive config");
        logger.info("Default Config: --defaultConfig=, generate default config file when it does not exist");
        logger.info(STR."Interface: --mode='{CLI}', support: \{RUNNER_MAP.entrySet().stream()
            .map(e -> {
                String help = e.getValue().help();
                return e.getKey() + (help != null ? (STR.": arguments: [\{help}]") : "");
            })
            .collect(Collectors.joining("; "))}");
        logger.info("Global config file path: --cfg={0}", GLOBAL_CFG);
        logger.info(STR."Tools:\n    \{ALL_TOOLS.stream().map(Tool::name).map(ToolName::toString)
            .collect(Collectors.joining("\n    "))}");
    }
}
