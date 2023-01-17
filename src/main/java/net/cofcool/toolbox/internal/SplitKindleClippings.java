package net.cofcool.toolbox.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class SplitKindleClippings implements Tool {
    @Override
    public ToolName name() {
        return ToolName.kindle;
    }

    @Override
    public void run(Args args) throws Exception {
        var path = args.readArg("path").get().val();
        var out = args.readArg("out").orElse(new Arg("", FilenameUtils.getBaseName(path) + ".md")).val();
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
        System.out.println("Write file to " + out);
        FileUtils.writeStringToFile(new File(out), strs, StandardCharsets.UTF_8);
    }

    @Override
    public String help() {
        return "--path=kindle.txt --out=kindle.md";
    }
}
