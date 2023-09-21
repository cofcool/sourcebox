package net.cofcool.toolbox.internal.commandhelper;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.WebTool;

@CustomLog
public class CommandHelper implements WebTool {

    private CommandManager commandManager;

    @Override
    public ToolName name() {
        return ToolName.cHelper;
    }

    @Override
    public void run(Args args) throws Exception {
        var path = args.readArg("filepath").val();

        var cmg = getCommandManager(path);

        var command = args.readArg("add");
        if (command.isPresent()) {
            cmg.save(command.val());
            return;
        }

        args.readArg("find").ifPresent(a -> args.getContext().write(toPrintStr(cmg.findByAT(a.val()))));

        args.readArg("store").ifPresent(a -> {
            cmg.store(a.val());
        });
    }

    private String toPrintStr(List<Command> commandList) {
        return commandList.stream().map(Command::toString).collect(Collectors.joining("\n"));
    }

    CommandManager getCommandManager(String path) {
        if (commandManager == null) {
            commandManager = new CommandManager(path);
        }

        return commandManager;
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("add", null, "add new command",  false, "@my-md5 mytool --md5= #my"))
            .arg(new Arg("filepath", "./commands.json", "commands file path",  false, null))
            .arg(new Arg("find", "ALL", "find command, can be tag or alias, ALL will list all",  false, "#md5"))
            .arg(new Arg("store", null, "save alias into env, ALL will save all",  false, "ALL"));
    }

    @Override
    public Router router(Vertx vertx) {
        return null;
    }


}
