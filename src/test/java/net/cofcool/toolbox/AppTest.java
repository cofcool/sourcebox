package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.RunnerType;
import net.cofcool.toolbox.logging.Logger;
import net.cofcool.toolbox.logging.LoggerFactory;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void runWithNoName() {
        App.main(new String[]{"--tool=test"});
    }

    @Test
    void run() {
        App.main(new String[]{"--tool=" + ToolName.converts.name(),  "--cmd=now"});
    }

    @Test
    void runWithHelp() {
        App.main(new String[]{"--tool=" + ToolName.converts.name()});
    }

    @Test
    void runWithHelp1() {
        App.main(new String[]{"--help=" + ToolName.converts.name()});
    }

    @Test
    void printAllHelp() {
        for (Tool tool : App.supportTools(RunnerType.CLI)) {
            System.out.println(tool.name());
            System.out.println(tool.config().toHelpString());
            System.out.println("-------");
        }
    }

    @Test
    void logAllHelp() {
        Logger log = LoggerFactory.getLogger(this.getClass());
        for (Tool tool : App.supportTools(RunnerType.CLI)) {
            log.info(tool.config().toHelpString());
        }
    }


}