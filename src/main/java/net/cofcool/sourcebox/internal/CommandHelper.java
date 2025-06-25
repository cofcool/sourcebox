package net.cofcool.sourcebox.internal;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.CommandIndex;
import net.cofcool.sourcebox.internal.api.NoteConfig;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.runner.WebRunner;
import net.cofcool.sourcebox.util.JsonUtil;

@CustomLog
public class CommandHelper implements WebTool {

    private String port;

    @Override
    public ToolName name() {
        return ToolName.cHelper;
    }

    @Override
    public String getPort() {
        return port;
    }

    @Override
    public void run(Args args) throws Exception {
        args.readArg(NoteConfig.PORT_KEY).ifPresent(a -> port = a.val());
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

        var find = args.readArg("find");
        var q = "/cmd/quick";
        if (find.isPresent()) {
            q= q + "?q="+ URLEncoder.encode(find.val(), StandardCharsets.UTF_8);
        }
        var data = getRequestLocalData(q, Commands.class);
        Object ret;
        if (args.getContext().runnerType() == RunnerType.CLI) {
            ret = toPrintStr(data);
        } else {
            ret = JsonUtil.toJson(data);
        }
        args.getContext().write(ret);
    }

    private String toPrintStr(List<CommandRecord> commandList) {
        return commandList.stream().map(CommandRecord::toString).collect(Collectors.joining("\n"));
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("add", null, "add new command",  false, "@my-md5 mytool --md5= #my"))
            .arg(new Arg("find", "ALL", "find command, can be tag or alias, ALL will list all",  false, "#md5"))
            .arg(new Arg("del", null, "delete command, can be tag or alias, ALL will delete all",  false, "#md5"))
            .arg(new Arg("store", null, "save alias into env, ALL will save all",  false, "ALL"))
            .arg(new Arg("port", WebRunner.PORT_VAL + "", "web server listen port", false, null))
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
}
