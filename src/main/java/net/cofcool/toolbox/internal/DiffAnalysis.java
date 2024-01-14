package net.cofcool.toolbox.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class DiffAnalysis implements Tool {

    public static final String LOG_MSG_ID = "logMsgId;";

    @Override
    public ToolName name() {
        return ToolName.analysisDiff;
    }

    @Override
    public void run(Args args) throws Exception {
        List<String> lines;
        var path = args.readArg("path").val();
        var diffPath = args.readArg("diffPath");
        if (diffPath.isPresent()) {
            lines = FileUtils.readLines(new File(diffPath.val()), StandardCharsets.UTF_8);
        } else {
            InputStreamReader in = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            if (in.ready()) {
                lines = new BufferedReader(in).lines().toList();
            } else {
                throw new IllegalArgumentException("Can not read diff from stdin");
            }
        }

        var diffs = parseDiffs(lines);

        getLogger().debug("Diff msg: {0}", diffs);

        var logSize = Integer.parseInt(args.readArg("logSize").val());
        var uncommit = Boolean.parseBoolean(args.readArg("uncommit").val());
        var formatter = Outputs.valueOf(args.readArg("formatter").val());

        var outItems = new ArrayList<String>();
        for (Diff diff : diffs) {
            for (Change change : diff.changes) {
                var p = Runtime
                    .getRuntime()
                    .exec(
                        new String[] {
                            "git", "log", "--format=" + LOG_MSG_ID + "%an;%cI;%h;%s", "-L",
                            change.line + "," + change.line + ":" + diff.path
                        },
                        null,
                        new File(path)
                    );
                var output = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);

                var tmpDiffStr = new ArrayList<String>();
                var idx = -1;
                for (String s : output.split("\n")) {
                    if (s.startsWith(LOG_MSG_ID)) {
                        idx++;

                        var msg = s.substring(LOG_MSG_ID.length());
                        var split = msg.split(";");
                        change.author = split[0];
                        var date = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(split[1]);
                        change.date = DateTimeFormatter.ISO_LOCAL_DATE.format(date);
                        change.commitId = split[2];
                        change.commitMsg = String.join(";", Arrays.copyOfRange(split, 3, split.length));

                        if (idx <= logSize && (uncommit || !tmpDiffStr.isEmpty())) {
                            var tmpDiffs = uncommit ?
                                Collections.singletonList(new Diff().setChanges(Collections.singletonList(
                                    new Change().setCurrent(change.current).setOld(change.old)
                                        .setPath(change.path))))
                                : parseDiffs(tmpDiffStr);
                            for (Diff logDiff : tmpDiffs) {
                                for (Change logChange : logDiff.changes) {
                                    outItems.add(formatter.format(change, logChange));
                                }
                            }
                        }
                        tmpDiffStr.clear();
                    } else if (StringUtils.isNoneBlank(s)) {
                        tmpDiffStr.add(s);
                    }
                }
            }
        }

        var outPath = args.readArg("out");
        if (outPath.isPresent()) {
            FileUtils.writeLines(new File(outPath.val()), outItems);
        } else {
            args.getContext().write(String.join("\n", outItems));
        }
    }

    private static ArrayList<Diff> parseDiffs(List<String> lines) {
        var token = Token.change;
        var diffs = new ArrayList<Diff>();
        Diff tmpDiff = null;
        for (String line : lines) {
            if (token == Token.change && line.startsWith("diff")) {
                token = Token.diff;
                tmpDiff = new Diff();
                tmpDiff.id = line;
                diffs.add(tmpDiff);
                continue;
            }
            if (token == Token.diff && line.startsWith("index")) {
                token = Token.index;
                tmpDiff.index = line.substring(6);
                continue;
            }
            if ((token == Token.index || token == Token.diff) && line.startsWith("--- ")) {
                token = Token.path;
                tmpDiff.path = line.substring(6);
                continue;
            }
            if (token == Token.path && line.startsWith("@@ ")) {
                token = Token.change;
                tmpDiff.changes.add(new Change(line).setPath(tmpDiff.path));
                continue;
            }
            if (token == Token.change) {
                if (line.startsWith("@@ ")) {
                    Objects.requireNonNull(tmpDiff).changes.add(new Change(line).setPath(tmpDiff.path));
                    continue;
                }
                if (line.startsWith("-")) {
                    Objects.requireNonNull(tmpDiff).changes.getLast().old = line.substring(1).trim();
                    continue;
                }
                if (line.startsWith("+")) {
                    Objects.requireNonNull(tmpDiff).changes.getLast().current = line.substring(1).trim();
                }
            }
        }
        return diffs;
    }

    enum Outputs implements OutputFormatter {
        csv((c,l) -> String.join(
            "|",
            l.old, l.current,
            StringUtils.difference(l.old, l.current),
            c.author, c.date, c.commitId, c.commitMsg
        ));

        private final OutputFormatter formatter;

        Outputs(OutputFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public String format(Change commitChange, Change logChange) {
            return formatter.format(commitChange, logChange);
        }
    }

    @FunctionalInterface
    private interface OutputFormatter {

        String format(Change commitChange, Change logChange);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("diffPath", null, "diff file path, can use some git commands to generate, like: git diff --unified=0 --diff-filter=M. Also can use stdin", false, "./demo.diff"))
            .arg(new Arg("path", null, "git project path", true, "./demo"))
            .arg(new Arg("logSize", "1", "log size", false, null))
            .arg(new Arg("formatter", "csv", "output formatter: " + Arrays.toString(Outputs.values()), false, null))
            .arg(new Arg("uncommit", "false", "if true, will log that dose not be committed files", false, null))
            .arg(new Arg("out", null, "out file path", false, "./diff-analysis.csv"))
            .alias("analysisDiff", name(), "path", null);
    }

    enum Token {
        none, diff, index, path, change
    }

    @Data
    @Accessors(chain = true)
    private static class Diff {
        String id;
        String index;
        String path;
        List<Change> changes = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    static class Change {
        String old;
        String current;
        String line;
        String id;
        int total;
        String path;

        String commitId;
        String commitMsg;
        String date;
        String author;

        public Change(String line) {
            var tmp = line.substring(3);
            int splitIdx = tmp.indexOf("@");
            tmp = tmp.substring(0, splitIdx - 1).split(" ")[0].substring(1);
            if (tmp.contains(",")) {
                String[] split = tmp.split(",");
                this.line = split[0];
                this.total = Integer.parseInt(split[1]);
            } else {
                this.line = tmp;
            }
            this.id = line.length() > splitIdx + 6 ? line.substring(splitIdx + 6) : "";
        }
    }

}
