package net.cofcool.sourcebox.internal;

import java.io.File;
import java.net.URLEncoder;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.SneakyThrows;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.CommandIndex;
import net.cofcool.sourcebox.internal.api.CommandService.ImportParam;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

@CustomLog
public class CommandHelper implements WebTool {

    private static final String TMP_TSB_D_TMP_JSONL = System.getProperty("java.io.tmpdir") + File.separator + "tsb-d-tmp.jsonl";
    private ToolContext toolContext;

    @Override
    public ToolName name() {
        return ToolName.cHelper;
    }

    @Override
    public void run(Args args) throws Exception {
        toolContext = args.getContext();
        var command = args.readArg("add");
        if (command.isPresent()) {
            postRequestLocalData(
                "/cmd/quick",
                CommandRecord.builder().cmd(command.val()).build(),
                CommandRecord.class
            );
            return;
        }

        var importArg = args.readArg("import");
        if (importArg.isPresent()) {
            importHis(importArg.val());
            return;
        }

        var del = args.readArg("del");
        if (del.isPresent()) {
            if (deleteRequestLocalData("/cmd/"+ URLEncoder.encode(del.val(), StandardCharsets.UTF_8))) {
                log.info("Delete {0}", del.val());
            }
            return;
        }

        var store = args.readArg("store");
        if (store.isPresent()) {
            getRequestLocalData("/cmd/store/"+ URLEncoder.encode(store.val(), StandardCharsets.UTF_8), Boolean.class);
            return;
        }

        var export = args.readArg("export");
        if (export.isPresent()) {
            var body = getRequestLocalData("/cmd/export", JSONL.class);
            FileUtils.writeLines(new File(export.val()), body);
            return;
        }

        var download = args.readArg("download");
        if (download.isPresent()) {
            String s = download.val();
            var i = s.lastIndexOf(":");
            var body = Utils.requestAPI(s.substring(0, i), s.substring(i+1), "/cmd/export", JSONL.class, Builder::GET, e -> {
                if (e != null) {
                    throw new IllegalStateException("request " + s +" export error", e);
                }
            });
            FileUtils.writeLines(new File(TMP_TSB_D_TMP_JSONL), body);
            importHis("down:jsonline:"+TMP_TSB_D_TMP_JSONL);
            return;
        }

        Arg find = args.readArg("find");
        if (find.isPresent()) {
            var q = "/cmd/quick?q="+ URLEncoder.encode(find.val(), StandardCharsets.UTF_8);
            var data = getRequestLocalData(q, Commands.class);
            toolContext.write(toPrintStr(data));
            return;
        }

        if (toolContext.runnerType() == RunnerType.CLI) {
            new InteractiveShell().launch();
        }
    }

    private String toPrintStr(List<CommandRecord> commandList) {
        return commandList.stream().map(CommandRecord::toString).collect(Collectors.joining("\n"));
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("add", null, "add new command",  false, "@my-md5 mytool --md5= #my"))
            .arg(new Arg("find", null, "find command, can be tag or alias, ALL will list all",  false, "#md5"))
            .arg(new Arg("del", null, "delete command, can be tag or alias, ALL will delete all",  false, "#md5"))
            .arg(new Arg("store", null, "save alias into env, ALL will save all",  false, "ALL"))
            .arg(new Arg("import", null, "import bash history, zsh history or jsonline",  false, "local:bash:~/.bash_history"))
            .arg(new Arg("export", null, "export all history with the out path",  false, "./export-his.txt"))
            .arg(new Arg("download", null, "download history from some server and import to local db",  false, "http://xxx:38080"))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
    }

    @Override
    public boolean supportCommand() {
        return true;
    }

    @Override
    public Class<? extends WebRouter> routerType() {
        return CommandIndex.class;
    }

    public static class Commands extends ListData<CommandRecord> {

    }

    public static class JSONL extends ListData<String> {

    }

    class OnlineCompleter implements Completer {

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            var q = "/cmd/quick?cmd="+ URLEncoder.encode(line.line(), StandardCharsets.UTF_8);
            var data = getRequestLocalData(q, Commands.class);
            data.forEach(d -> {
                    candidates.add(new Candidate(d.cmd()));
                }
            );
        }
    }

    @SneakyThrows
    private void importHis(String path) {
        var split = path.split(":");
        if (split.length == 3) {
            var param = new ImportParam(
                split[1],
                FileUtils.readFileToString(new File(split[2]), StandardCharsets.UTF_8),
                split[0]
            );
            postRequestLocalData("/cmd/import", param, boolean.class);
        } else {
            throw new IllegalArgumentException("import path error " + path);
        }
    }

    class InteractiveShell {
        void launch() throws Exception {
            if (Utils.isLocalhost((String) App.getGlobalConfig(ToolRunner.ADDRESS_KEY))) {
                importHis("local:zsh:" + System.getProperty("user.home") + "/.zsh_history");
            }
            Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

            Completer completer = new OnlineCompleter();

            LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .completer(completer)
                .build();

            while (true) {
                try {
                    String line = reader.readLine("$>> ");
                    if (StringUtils.isBlank(line)) {continue;};
                    line = line.trim();
                    requestLocalData(
                        "/cmd/enter/" + CommandRecord.builder().cmd(line).build().id(),
                        CommandRecord.class, b -> b.POST(BodyPublishers.noBody()), e -> {
                            if (e != null) {
                                log.info("Request enter error", e);
                            }
                        }
                    );

                    toolContext.write(line);
                    return;
                } catch (UserInterruptException e) {
                    terminal.writer().println("\nInterrupt，enter 'exit' to exit");
                } catch (EndOfFileException e) {
                    terminal.writer().println("\nEOF (Ctrl+D)，exit");
                    return;
                } catch (Exception e) {
                    terminal.writer().println("\nRead input command error: " + e);
                }
            }
        }
    }
}
