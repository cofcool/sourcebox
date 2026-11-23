package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhotoManager implements WebTool {
    @Override
    public ToolName name() {
        return ToolName.photo;
    }

    @Override
    public void run(Args args) throws Exception {
        var roots = args.getArgVal("roots")
            .filter(value -> !value.isBlank())
            .map(value -> Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(root -> !root.isBlank())
                .toList())
            .orElseGet(() -> args.getArgVal("root")
                .filter(value -> !value.isBlank())
                .map(List::of)
                .orElseGet(() -> args.getArgVal("scan")
                    .filter(value -> !value.isBlank())
                    .map(List::of)
                    .orElseGet(List::of)));
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("Please configure root or roots");
        }
        postRequestLocalData(
            "/photo/scans",
            Map.of(
                "sourceRoots", roots,
                "albumName", java.nio.file.Path.of(roots.getFirst()).getFileName().toString(),
                "recursive", Boolean.parseBoolean(args.getArgVal("recursive").orElse("true"))
            ),
            Map.class
        );
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("root", null, "photo scan root directory", false, "/data/photos"))
            .arg(new Arg("roots", null, "comma-separated photo scan directories", false, "/data/photos1,/data/photos2"))
            .arg(new Arg("scan", null, "scan a photo directory", false, "/data/photos"))
            .arg(new Arg("recursive", "true", "scan image directories recursively", false, "true"));
    }
}
