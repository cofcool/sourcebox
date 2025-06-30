package net.cofcool.sourcebox.internal;

import java.net.URLEncoder;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.CommandIndex;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.internal.api.entity.ListData;
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

    private void importHis() {
        getRequestLocalData("/cmd/import", boolean.class);
    }

    class InteractiveShell {
        void launch() throws Exception {
            importHis();
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
                    if (line == null || line.trim().isEmpty()) continue;

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
