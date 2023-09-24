package net.cofcool.toolbox;

import net.cofcool.toolbox.internal.ClippingsToMd;
import net.cofcool.toolbox.internal.CodeGenerator;
import net.cofcool.toolbox.internal.Converts;
import net.cofcool.toolbox.internal.DirWebServer;
import net.cofcool.toolbox.internal.FileNameFormatter;
import net.cofcool.toolbox.internal.GitCommitsToChangelog;
import net.cofcool.toolbox.internal.HtmlDownloader;
import net.cofcool.toolbox.internal.JsonFormatter;
import net.cofcool.toolbox.internal.JsonToPojo;
import net.cofcool.toolbox.internal.LinkCovertTool;
import net.cofcool.toolbox.internal.ShellStarter;
import net.cofcool.toolbox.internal.SimpleNote;
import net.cofcool.toolbox.internal.TrelloToLogseqImporter;
import net.cofcool.toolbox.internal.commandhelper.CommandHelper;

public enum ToolName {
    trelloLogseqImporter("read trello backup json file and convert to logseq md file", TrelloToLogseqImporter.class),
    shell("run shell command", ShellStarter.class),
    link2Tool("convert link file to md", LinkCovertTool.class),
    clippings2Md("read kindle clipboard file(or others file) then convert to md file", ClippingsToMd.class),
    gitCommits2Log("generate changelog file from git commit log", GitCommitsToChangelog.class),
    converts("some simple utilities about string, like base64 encode", Converts.class),
    json2POJO("convert json structure to POJO class", JsonToPojo.class),
    dirWebServer("start a simple web directory server", DirWebServer.class),
    rename("rename file conveniently", FileNameFormatter.class),
    note("simple web notebook", SimpleNote.class),
    json("make json more readable", JsonFormatter.class),
    htmlDown("download html from links", HtmlDownloader.class),
    cHelper("manage commands, like alias, tag...", CommandHelper.class),
    generate("generate code from template", CodeGenerator .class);

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
