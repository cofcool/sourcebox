package net.cofcool.toolbox;

public enum ToolName {
    trelloLogseqImporter("read trello backup json file and convert to logseq md file"),
    shell("run shell command"),
    link2Tool("convert link file to md"),
    kindle("read kindle clipboard file and convert to md file"),
    gitCommits2Log("generate changelog file from git commit log"),
    converts("some simple utilities about string, like base64 encode"),
    json2POJO("convert json structure to POJO class");

    private final String desc;

    ToolName(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return name() + ": " + desc;
    }

    public String getDesc() {
        return desc;
    }
}
