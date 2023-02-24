package net.cofcool.toolbox;

import net.cofcool.toolbox.internal.Converts;
import net.cofcool.toolbox.internal.DirWebServer;
import net.cofcool.toolbox.internal.GitCommitsToChangelog;
import net.cofcool.toolbox.internal.JsonToPojo;
import net.cofcool.toolbox.internal.LinkCovertTool;
import net.cofcool.toolbox.internal.ShellStarter;
import net.cofcool.toolbox.internal.SplitKindleClippings;
import net.cofcool.toolbox.internal.TrelloToLogseqImporter;

public enum ToolName {
    trelloLogseqImporter("read trello backup json file and convert to logseq md file", TrelloToLogseqImporter.class),
    shell("run shell command", ShellStarter.class),
    link2Tool("convert link file to md", LinkCovertTool.class),
    kindle("read kindle clipboard file and convert to md file", SplitKindleClippings.class),
    gitCommits2Log("generate changelog file from git commit log", GitCommitsToChangelog.class),
    converts("some simple utilities about string, like base64 encode", Converts.class),
    json2POJO("convert json structure to POJO class", JsonToPojo.class),
    dirWebServer("start a simple web directory server", DirWebServer.class);

    private final String desc;
    private final Class<? extends Tool> tool;

    ToolName(String desc, Class<? extends Tool> tool) {
        this.desc = desc;
        this.tool = tool;
    }

    @Override
    public String toString() {
        return name() + ": " + desc;
    }

    public String getDesc() {
        return desc;
    }

    public Class<? extends Tool> getTool() {
        return tool;
    }
}
