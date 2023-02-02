package net.cofcool.toolbox.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;

public class SplitKindleClippings implements Tool {
    @Override
    public ToolName name() {
        return ToolName.kindle;
    }

    @Override
    public void run(Args args) throws Exception {
        var path = args.readArg("path").val();
        var out = args.readArg("out").val();
        var fileContent = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
        String strs = Arrays
                .stream(fileContent.split("=========="))
                .map(s -> {
                    String[] split = s.split("\n");
                    var bookName = "";
                    var content = "";
                    if (split.length == 5) {
                        bookName = split[0];
                        content = split[3];
                    } else {
                        bookName = split[1];
                        content = split[4];
                    }
                    return String.format("### %s\n\n%s\n\n", bookName, content);
                })
                .collect(Collectors.joining());
        getLogger().info("Write file to " + out);
        FileUtils.writeStringToFile(new File(out), strs, StandardCharsets.UTF_8);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("path", null, "clipboard file path", true, "./kindle.txt"))
            .arg(new Arg("out", "./kindle.md", "output path", false, null));
    }
}
