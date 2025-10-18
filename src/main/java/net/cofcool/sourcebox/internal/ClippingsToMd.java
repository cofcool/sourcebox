package net.cofcool.sourcebox.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class ClippingsToMd implements Tool {
    @Override
    public ToolName name() {
        return ToolName.clippings2Md;
    }

    @Override
    public void run(Args args) throws Exception {
        var path = args.readArg("path").val();
        var out = args.readArg("out").optVal().orElse(
            STR."\{FilenameUtils.getFullPath(path)}\{FilenameUtils.getBaseName(path)}.md"
        );
        var type = ClipType.valueOf(args.readArg("type").val());

        var fileContent = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);

        var highlights = switch (type) {
            case kindle -> kindle(fileContent);
            case mrexpt -> mrexpt(fileContent);
        };
        var ret = highlights
            .stream()
            .collect(Collectors.groupingBy(Highlight::getName))
            .entrySet()
            .stream()
            .map(e ->
                "## " + e.getKey() + "\n\n"
                + e.getValue().stream().map(Highlight::toString).collect(Collectors.joining("\n\n"))
                + "\n"
            )
            .collect(Collectors.joining("\n"));
        getLogger().info("Write file to " + out);

        args.getContext().write(out, ret);
    }


    private List<Highlight> mrexpt(String fileContent) {
        var highlights = new ArrayList<Highlight>();
        try (var reader = new BufferedReader(new StringReader(fileContent))) {
            var line = "";
            var highlight = new Highlight();
            var idx = -1;
            while ((line = reader.readLine()) != null) {
                if (idx == -1 && line.equals("#")) {
                    idx = 0;
                    continue;
                }
                if (idx >= 0) {
                    idx++;
                }

                if (idx == 3) {
                    highlight.setName(FilenameUtils.getBaseName(line));
                }
                if (idx == 10) {
                    highlight.setTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(line)), ZoneId.systemDefault()));
                }
                if (idx == 11 && !line.isEmpty()) {
                    highlight.setBookmark(true);
                    highlight.setHighlight(line);
                }
                if (idx == 12) {
                    highlight.setComment(line);
                }
                if (idx == 13) {
                    idx = -1;
                    if (!highlight.isBookmark()) {
                        highlight.setHighlight(line);
                    }
                    highlights.add(highlight);

                    highlight = new Highlight();
                }
            }

        } catch (IOException ex) {
            throw new IllegalStateException("Parsing mrexpt error", ex);
        }
        return highlights;
    }

    private List<Highlight> kindle(String fileContent) {
        return Arrays
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
                return new Highlight(bookName, content);
            })
            .collect(Collectors.toList());
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("path", null, "clipboard file path or content", true, "./kindle.txt"))
            .arg(new Arg("type", ClipType.kindle.name(), "support: " + Arrays.toString(ClipType.values()), false, null))
            .arg(new Arg("out", null, "output path", false, "./book.md"))
            .runnerTypes(EnumSet.of(RunnerType.WEB, RunnerType.CLI));
    }

    @Data
    @NoArgsConstructor
    private static class Highlight {

        static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String name;
        String comment;
        boolean bookmark;
        String highlight;
        LocalDateTime time;

        public Highlight(String name, String highlight) {
            this.name = name;
            this.highlight = highlight;
        }

        @Override
        public String toString() {
            var str = "* > " + highlight;
            if (bookmark) {
                str += "(🔖)";
            }
            if (time != null) {
                str += "(" + time.format(formatter) + ")";
            }
            if (!StringUtils.isBlank(comment)) {
                str = str + "\n\n   * " + comment ;
            }

            return str;
        }
    }

    public enum ClipType {
        kindle, mrexpt
    }
}
