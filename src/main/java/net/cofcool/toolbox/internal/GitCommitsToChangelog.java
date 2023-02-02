package net.cofcool.toolbox.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * git-log PRETTY FORMATS
 * <code>git log --format="%d;%h;%s"</code>
 */
public class GitCommitsToChangelog implements Tool {

    private final Tool delegate = new ShellStarter();

    @Override
    public ToolName name() {
        return ToolName.gitCommits2Log;
    }

    // (HEAD -> master, origin/master, origin/HEAD);117826a;Converts: now, replace
    //;1788569;add debug mode
    //;1ee4312;update doc
    //;b676feb;update doc
    // (tag: 1.0.1);a9d3e04;add Converts
    @Override
    public void run(Args args) throws Exception {
        String out = args.readArg("out").val();
        var logFile = args.readArg("log");
        var requiredTag = args.readArg("tag");
        var noTag = args.readArg("no-tag").val().equalsIgnoreCase("true");

        String commitLog;
        if (logFile.isPresent()) {
            commitLog = FileUtils.readFileToString(new File(logFile.val()), StandardCharsets.UTF_8);
        } else {
            String path = args.readArg("path").val();

            String command = "git log --format=%d;%h;%s";
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
            AtomicInteger tag = new AtomicInteger(0);
            Arrays.stream(commitLog.split("\n"))
                    .map(a -> a.split(";"))
                    .filter(a -> a.length > 2)
                    .map(a -> new Commit(a[0], a[1], String.join(";", Arrays.copyOfRange(a, 2, a.length))))
                    .forEach(c -> {
                        getLogger().debug(c);
                        if (noTag) {
                            commits.add(c.toString());
                            return;
                        }
                        if (tag.get() == 0) {
                            c.tag().ifPresent(t -> {
                                if (!t.equals(requiredTag.val())) {
                                    return;
                                }
                                tag.set(1);
                                commits.addFirst("## " + (t.startsWith("v") || t.startsWith("V") ? "" : "v") + t + "\n");
                                commits.add(c.toString());
                            });
                        } else if (tag.get() == 1) {
                            if (c.tag().isPresent()) {
                                tag.set(2);
                            } else {
                                commits.add(c.toString());
                            }
                        }
                    });
            FileUtils.writeStringToFile(new File(out), String.join("\n", commits), StandardCharsets.UTF_8);
            getLogger().info("Generate " + out + " ok");
        }
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("path", null, "project directory, must set this or log", false, "./demo/"))
            .arg(new Arg("log", null, "Git commit log fil", false, "./git-commit-log.txt"))
            .arg(new Arg("out", "./target/changelog.md", "generate file output path", false, null))
            .arg(new Arg("tag", null, "read commit log to the tag", false, "1.0.0"))
            .arg(new Arg("no-tag", "false", "if true, read all commit log and write into file", false, null));
    }

    private record Commit(
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

        @Override
        public String toString() {
            return "* " + message + "(" + hash + ")";
        }
    }
}
