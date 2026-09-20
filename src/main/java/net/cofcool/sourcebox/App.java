package net.cofcool.sourcebox;

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
import org.apache.commons.lang3.StringUtils;

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
                    .arg(new Arg("completion", null, "generate completion script: bash|zsh|all", false, "bash"))
                    .arg(new Arg("completionOut", null, "output dir or file for completion script", false, ""))
                    .arg(new Arg("completionAlias", "sourcebox", "command alias", false, ""))
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

        var completion = pArgs.readArg("completion");
        if (completion.isPresent()) {
            try {
                String out = pArgs.readArg("completionOut").val();
                generateCompletion(completion.val(), out, pArgs.readArg("completionAlias").val());
                logger.info("Generate completion for " + completion.val() + " finished");
            } catch (Exception e) {
                logger.error("Generate completion error", e);
            }
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
        logger.info("About: "+ ABOUT);
        logger.info("Example: --tool=demo --path=tmp");
        logger.info("Help: --help='{COMMAND}', like: --help=rename");
        logger.info("Archive: --archive=true, archive config");
        logger.info("Completion: --completion='{SHELL}', bash/zsh/all");
        logger.info("Default Config: --defaultConfig=, generate default config file when it does not exist");
        logger.info("Interface: --mode='{CLI}', support: "+ RUNNER_MAP.entrySet().stream()
            .map(e -> {
                String help = e.getValue().help();
                return e.getKey() + (help != null ? (": arguments: [" + help +"]") : "");
            })
            .collect(Collectors.joining("; ")));
        logger.info("Global config file path: --cfg={0}", GLOBAL_CFG);
        logger.info("Tools:\n    " + ALL_TOOLS.stream().map(Tool::name).map(ToolName::toString)
            .collect(Collectors.joining("\n    ")));
    }

    static void generateCompletion(String shell, String out, String commandName) throws Exception {
        if (shell.equalsIgnoreCase("all")) {
            generateShellCompletion("bash", out, commandName);
            generateShellCompletion("zsh", out, commandName);
        } else {
            generateShellCompletion(shell, out, commandName);
        }
    }

    private static void generateShellCompletion(String shell, String outPath, String commandName) throws Exception {
        String content = switch (shell) {
            case "bash" -> buildBashCompletion(commandName);
            case "zsh"  -> buildZshCompletion(commandName);
            default -> throw new IllegalArgumentException("Unknown shell: " + shell);
        };

        writeCompletionScript(shell, content, outPath, commandName);
    }

    private static void writeCompletionScript(String shell, String scriptContent, String outPath, String commandName) throws IOException {
        if (StringUtils.isBlank(outPath)) {
            String defaultDir = resolveCompletionDir(shell);
            File dir = new File(defaultDir);
            dir.mkdirs();
            File target = new File(dir, resolveCompletionFileName(shell));
            FileUtils.writeStringToFile(target, scriptContent, "utf-8");
            return;
        }

        File f = new File(outPath);
        if (f.isDirectory()) {
            FileUtils.writeStringToFile(new File(f, resolveCompletionFileName(shell)), scriptContent, "utf-8");
        } else {
            FileUtils.writeStringToFile(f, scriptContent, "utf-8");
        }
    }

    private static String resolveCompletionDir(String shell) {
        return FilenameUtils.concat(System.getProperty("user.home"), switch (shell) {
            case "bash" -> ".local/share/bash-completion/completions";
            case "zsh"  -> ".zsh/completions";
            default -> throw new IllegalArgumentException("Unknown shell: " + shell);
        });
    }

    private static String resolveCompletionFileName(String shell) {
        return switch (shell) {
            case "bash" -> "sourcebox";
            case "zsh"  -> "_sourcebox";
            default -> throw new IllegalArgumentException("Unknown shell: " + shell);
        };
    }

    private static String buildBashCompletion(String commandName) {
        var sb = new StringBuilder();
        sb.append("_sourcebox_completion() {\n");
        sb.append("  local cur=${COMP_WORDS[COMP_CWORD]}\n");
        sb.append("  local i\n");
        String toolList = App.ALL_TOOLS.stream().map(t -> t.name().name()).collect(Collectors.joining(" "));
        sb.append("  local TOOLS=\"").append(toolList).append("\"\n");
        sb.append("  local GLOBAL_OPTS=\"--help --tool --mode --cfg --debug --archive --defaultConfig --completion --completionOut\"\n\n");

        sb.append("  if [[ ${cur} == --tool=* ]] ; then\n");
        sb.append("    local val=${cur#--tool=}\n");
        sb.append("    COMPREPLY=( $(compgen -W \"${TOOLS}\" -- \"$val\") )\n");
        sb.append("    return\n");
        sb.append("  fi\n");
        sb.append("  if [[ ${COMP_WORDS[COMP_CWORD-1]} == \"--tool\" ]]; then\n");
        sb.append("    COMPREPLY=( $(compgen -W \"${TOOLS}\" -- \"$cur\") )\n");
        sb.append("    return\n");
        sb.append("  fi\n\n");

        sb.append("  local selTool=\"\"\n");
        sb.append("  for i in \"${COMP_WORDS[@]}\"; do\n");
        sb.append("    case \"$i\" in --tool=*) selTool=${i#--tool=}; break;; esac\n");
        sb.append("  done\n");
        sb.append("  if [[ -n $selTool ]]; then\n");
        sb.append("    case \"$selTool\" in\n");
        for (Tool t : App.ALL_TOOLS) {
            String name = t.name().name();
            var args = t.config().values().stream().map(a -> "--" + a.key()).collect(Collectors.joining(" "));
            if (args.isBlank()) args = "";
            sb.append("      ").append(name).append(")\n");
            sb.append("        COMPREPLY=( $(compgen -W \"" + args + "\" -- \"$cur\") )\n");
            sb.append("        return\n");
            sb.append("        ;;\n");
        }
        sb.append("    esac\n");
        sb.append("  fi\n\n");
        sb.append("  COMPREPLY=( $(compgen -W \"${GLOBAL_OPTS}\" -- \"$cur\") )\n");
        sb.append("}\n");
        sb.append("complete -F _sourcebox_completion ").append(commandName).append("\n");
        return sb.toString();
    }

    private static String buildZshCompletion(String commandName) {
        var sb = new StringBuilder();
        String toolList = App.ALL_TOOLS.stream().map(t -> t.name().name()).collect(Collectors.joining(" "));
        sb.append("#compdef ").append(commandName).append("\n\n");
        sb.append("_sourcebox() {\n");
        sb.append("  local state\n");
        sb.append("  typeset -A opt_args\n");
        sb.append("  local -a tools\n");
        sb.append("  tools=( ").append(toolList).append(" )\n\n");
        sb.append("  _arguments \\\n");
        sb.append("    '--help[show help]' \\\n");
        sb.append("    '--tool=[tool]:( ").append(toolList).append(" )' \\\n");
        sb.append("    '--mode=[runner mode]' \\\n");
        sb.append("    '--cfg=[config file]' \\\n");
        sb.append("    '--completion=[generate completion: bash|zsh|all]' \\\n");
        sb.append("    '*: :->rest' && return 0\n\n");
        sb.append("  if [[ $state == rest ]]; then\n");
        sb.append("    local selTool\n");
        sb.append("    for w in \"${words[@]}\"; do\n");
        sb.append("      case $w in --tool=*) selTool=${w#--tool=};; esac\n");
        sb.append("    done\n\n");
        sb.append("    if [[ -n $selTool ]]; then\n");
        sb.append("      case $selTool in\n");
        for (Tool t : App.ALL_TOOLS) {
            String name = t.name().name();
            sb.append("        ").append(name).append(")\n");
            sb.append("          _arguments \\\n");
            for (Arg arg : t.config().values()) {
                String option = "--" + arg.key() + "=[" + escapeZshSingleQuotedText(arg.desc()) + "]";
                sb.append("            '").append(option).append("' \\\n");
            }
            sb.append("            '*: :->rest'\n");
            sb.append("          ;;\n");
        }
        sb.append("      esac\n");
        sb.append("    fi\n");
        sb.append("  fi\n");
        sb.append("}\n\n");
        sb.append("compdef _sourcebox ").append(commandName).append("\n");
        return sb.toString();
    }

    private static String escapeZshSingleQuotedText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace(",", "\\,")
                .replace("'", "'\\''");
    }
}
