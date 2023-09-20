package net.cofcool.toolbox.internal.commandhelper;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.WebTool;

public class CommandHelper implements WebTool {

    @Override
    public ToolName name() {
        return ToolName.cHelper;
    }

    @Override
    public void run(Args args) throws Exception {

    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("add", null, "add new command",  false, "@my-md5 mytool --md5= #my"))
            .arg(new Arg("find", null, "find command",  false, "#md5"))
            .arg(new Arg("alias", null, "list all alias",  false, "@md5"))
            .arg(new Arg("store", null, "add alias into env",  false, "@md5"))
            .arg(new Arg("storeAll", null, "add all alias into env",  false, null))
            .arg(new Arg("tag", null, "list all command",  false, "#md5"));
    }

    @Override
    public Router router(Vertx vertx) {
        return null;
    }


}
