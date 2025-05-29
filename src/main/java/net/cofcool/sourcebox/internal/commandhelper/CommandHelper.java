package net.cofcool.sourcebox.internal.commandhelper;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FilenameUtils;

@CustomLog
public class CommandHelper implements Tool {

    private CommandManager commandManager;

    @Override
    public ToolName name() {
        return ToolName.cHelper;
    }

    @Override
    public void run(Args args) throws Exception {
        var path = args.readArg("filepath").val();

        var cmg = getCommandManager(path, args.readArg("aliasPath").val());

        var command = args.readArg("add");
        if (command.isPresent()) {
            cmg.save(command.val());
            return;
        }

        var del = args.readArg("del");
        if (del.isPresent()) {
            cmg.findByAT(del.val()).forEach(c -> {
                log.info("Delete {0}", c.id());
                cmg.delete(c.id());
            });
            return;
        }

        args.readArg("find").ifPresent(a -> {
            var data = cmg.findByAT(a.val());
            Object ret = null;
            if (args.getContext().runnerType() == RunnerType.CLI) {
                ret = toPrintStr(data);
            } else {
                ret = JsonUtil.toJson(data);
            }
            args.getContext().write(ret);
        });

        args.readArg("store").ifPresent(a -> {
            cmg.store(a.val());
        });
    }

    private String toPrintStr(List<Command> commandList) {
        return commandList.stream().map(Command::toString).collect(Collectors.joining("\n"));
    }

    CommandManager getCommandManager(String path, String alias) {
        if (commandManager == null) {
            commandManager = new CommandManager(path, alias);
        }

        return commandManager;
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("add", null, "add new command",  false, "@my-md5 mytool --md5= #my"))
            .arg(new Arg("filepath", "./commands.json", "commands file path",  false, null))
            .arg(new Arg("aliasPath", "./alias", "alias file path",  false, null))
            .arg(new Arg("find", "ALL", "find command, can be tag or alias, ALL will list all",  false, "#md5"))
            .arg(new Arg("del", null, "delete command, can be tag or alias, ALL will delete all",  false, "#md5"))
            .arg(new Arg("store", null, "save alias into env, ALL will save all",  false, "ALL"))
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.GUI));
    }

    @Override
    public Args defaultConfig(String globalDir) {
        return new Args()
            .arg("filepath", FilenameUtils.concat(globalDir, "commands.json"))
            .arg("aliasPath", FilenameUtils.concat(globalDir, "alias"));
    }


}
