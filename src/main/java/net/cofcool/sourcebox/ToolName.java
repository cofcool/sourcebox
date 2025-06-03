package net.cofcool.sourcebox;

import net.cofcool.sourcebox.internal.ClippingsToMd;
import net.cofcool.sourcebox.internal.CodeGenerator;
import net.cofcool.sourcebox.internal.Converts;
import net.cofcool.sourcebox.internal.DiffAnalysis;
import net.cofcool.sourcebox.internal.DirWebServer;
import net.cofcool.sourcebox.internal.FileNameFormatter;
import net.cofcool.sourcebox.internal.FileTools;
import net.cofcool.sourcebox.internal.GitCommitsToChangelog;
import net.cofcool.sourcebox.internal.GoAdapter;
import net.cofcool.sourcebox.internal.HtmlDownloader;
import net.cofcool.sourcebox.internal.JsonFormatter;
import net.cofcool.sourcebox.internal.JsonToPojo;
import net.cofcool.sourcebox.internal.LinkCovertTool;
import net.cofcool.sourcebox.internal.NetworkUtils;
import net.cofcool.sourcebox.internal.ShellStarter;
import net.cofcool.sourcebox.internal.SimpleNote;
import net.cofcool.sourcebox.internal.ToDo;
import net.cofcool.sourcebox.internal.TrelloToLogseqImporter;
import net.cofcool.sourcebox.internal.commandhelper.CommandHelper;

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
    note("simple web notebook, routes: /note, /action", SimpleNote.class),
    json("make json more readable", JsonFormatter.class),
    htmlDown("download html from links", HtmlDownloader.class),
    cHelper("manage commands, like alias, tag...", CommandHelper.class),
    generate("generate code from template", CodeGenerator.class),
    analysisDiff("analysis diff file", DiffAnalysis.class),
    fileTools("some utils about file", FileTools.class),
    netUtils("some utils about network", NetworkUtils.class),
    todo("Simple To Do tool", ToDo.class),
    task("repeat execute task(go)", GoAdapter.Task.class),
    mobileBackup("backup android phone files by adb(go)", GoAdapter.MobileBackup.class);

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
