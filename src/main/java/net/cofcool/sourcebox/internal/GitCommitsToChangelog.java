package net.cofcool.sourcebox.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * git-log PRETTY FORMATS
 * <code>git log --format="%an;%d;%h;%s"</code>
 */
public class GitCommitsToChangelog implements Tool {

    private static final String LATEST = "latest";
    private static final int MAX_TAG_SIZE = 100;

    @Override
    public ToolName name() {
        return ToolName.gitCommits2Log;
    }

    // (HEAD -> master, origin/master, origin/HEAD);117826a;Converts: now, replace;CofCool
    //;1788569;add debug mode
    //;1ee4312;update doc
    //;b676feb;update doc
    // (tag: 1.0.1);a9d3e04;add Converts
    @Override
    public void run(Args args) throws Exception {
        String out = args.readArg("out").val();
        var logFile = args.readArg("log");
        var requiredTag = args.readArg("tag");
        var noTag = args.readArg("noTag").val().equalsIgnoreCase("true");
        var full = requiredTag.isPresent() || noTag || args.readArg("full").val().equalsIgnoreCase("true");
        var style = Style.valueOf(args.readArg("style").val());
        var logId = args.readArg("logId");

        String commitLog;
        if (logFile.isPresent()) {
            commitLog = FileUtils.readFileToString(new File(logFile.val()), StandardCharsets.UTF_8);
        } else {
            String path = args.readArg("path").requiredVal("path must not be null");

            String command = "git log --format=%an;%d;%h;%s";
            getLogger().info("Run command: " + command);
            Process process = Runtime
                    .getRuntime()
                    .exec(
                            command.split(" "),
                            null,
                            new File(path)
                    );

            commitLog = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);

            if (commitLog.isBlank()) {
                String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                if (error != null && !error.isEmpty()) {
                    getLogger().error(error);
                    return;
                }
            }
        }

        if (commitLog != null && !commitLog.isEmpty()) {
            var commits = new ArrayDeque<String>();
            var currentTag = new AtomicReference<>(LATEST);
            Arrays.stream(commitLog.split("\n"))
                .map(a -> a.split(";"))
                .filter(a -> a.length > 3)
                .map(a -> new Commit(a[0], a[1], a[2], String.join(";", Arrays.copyOfRange(a, 3, a.length))))
                .map(c -> {
                    c.tag().filter(t -> !noTag).ifPresent(currentTag::set);
                    return new Commit(c.username, currentTag.get(), c.hash, c.message);
                })
                .sorted(Comparator.comparing(Commit::ref).reversed())
                .filter(c -> full || !c.ref.equalsIgnoreCase(LATEST))
                .filter(c -> args.readArg("user").test(u -> u.equalsIgnoreCase(c.username())))
                .collect(Collectors.groupingBy(Commit::ref, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .limit(full ? MAX_TAG_SIZE :  1)
                .forEach(e -> {
                    var t = e.getKey();
                    var c = e.getValue();
                    getLogger().debug(t);
                    if (requiredTag.isPresent() && !requiredTag.val().equals(t)) {
                        return;
                    }
                    if (!t.isBlank()) {
                        commits.add("## " + (t.equalsIgnoreCase(LATEST) || t.startsWith("v") || t.startsWith("V") ? "" : "v") + t + "\n");
                    }
                    c
                        .stream()
                        .filter(cf -> logId.test(cf.message::contains))
                        .collect(Collectors.groupingBy(cm -> cm.styleId(style)))
                        .forEach((k, v) -> {
                            k.ifPresent(kt -> commits.add("### " + kt + "\n"));
                            commits.add(v.stream()
                                .map(Commit::toString)
                                .map(cm -> logId.isPresent() ? cm.replace(logId.val(), "") : cm)
                                .collect(Collectors.joining("\n", "", "\n"))
                            );
                        });
                });
            args.getContext().write(out, String.join("\n", commits));
            getLogger().info("Generate " + out + " ok");
        }
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("path", null, "project directory, must set this or log", false, "./demo/"))
            .arg(new Arg("log", null, "Git commit log fil", false, "./git-commit-log.txt"))
            .arg(new Arg("out", "./target/changelog.md", "generate file output path", false, null))
            .arg(new Arg("user", null, "username filter", false, "cofcool"))
            .arg(new Arg("tag", null, "read commit log to the tag", false, "1.0.0"))
            .arg(new Arg("noTag", "false", "if true, read all commit log and write into file", false, null))
            .arg(new Arg("logId", null, "mark the commit history as the changelog", false, "#log"))
            .arg(new Arg("full", "false", "if true, read all tags and commit log, then write into file", false, null))
            .arg(new Arg("style", Style.simple.name(), "changelog file format, like " + Arrays.toString(Style.values()), false, null))
            .alias("gitLog", name(), "path", null);
    }

    enum Style {
        simple(""), angular("^(fix|feat|docs|style|refactor|pref|test|chore)\\(.+\\): .+");

        private final Pattern pattern;

        Style(String pattern) {
            this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
        }

        public Optional<String> find(String val) {
            Matcher matcher = pattern.matcher(val);
            if (matcher.find()) {
                try {
                    return Optional.of(matcher.group(1));
                } catch (IndexOutOfBoundsException ignore) {}
            }
            return Optional.empty();
        }
    }

    private record Commit(
        String username,
        String ref,
        String hash,
        String message
    ) {
        public Optional<String> tag() {
            if (ref.contains("tag")) {
                return Arrays.stream(ref.replace("(", "").replace(")", "")
                        .split(",")).filter(a -> a.trim().startsWith("tag"))
                        .map(a -> a.split(":")[1].trim()).findAny();
            }
            return Optional.empty();
        }

        public Optional<String> styleId(Style style) {
            return style.find(message);
        }

        @Override
        public String toString() {
            return "* " + message + "(" + hash + ")";
        }
    }
}
