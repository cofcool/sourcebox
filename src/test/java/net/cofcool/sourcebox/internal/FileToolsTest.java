package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Utils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.CRC32;

class FileToolsTest extends BaseTest {

    static final String INPUT_PATH = Utils.getTestResourcePath("/fileTools.txt");
    static final String SAMPLE_PATH = Utils.getTestResourcePath("/fileToolsSample.txt");

    @TempDir
    Path tmpDir;

    @Override
    protected Tool instance() {
        return new FileTools();
    }

    @Test
    void runWithDup() throws Exception {
        Files.write(tmpDir.resolve("computer-xx-2-2021.txt"), "xxx".getBytes());
        tmpDir.resolve("dir").toFile().mkdirs();
        Files.write(tmpDir.resolve("dir", "computer-xx-1-2022.txt"), "xxx".getBytes());
        instance().run(args.arg("util", "dup")
            .arg("path", tmpDir.toString())
        );
    }

    @Test
    void runWithDupExtractsDelimitedFeatures() {
        var book = FileDuplicateParser.BookNameParser.parse(Path.of("【作者-张三】长夜难明-2021.epub"));
        var plain = FileDuplicateParser.BookNameParser.parse(Path.of("无标记书名.txt"));

        Assertions.assertEquals("张三", book.author());
        Assertions.assertEquals("长夜难明", book.title());
        Assertions.assertEquals(2021, book.year());
        Assertions.assertTrue(book.bracketInfo().contains("作者-张三"));
        Assertions.assertEquals("", plain.author());
        Assertions.assertTrue(plain.bracketInfo().isEmpty());

        var similarPlain = FileDuplicateParser.BookNameParser.parse(Path.of("无标记书名修订.txt"));
        var result = FileDuplicateParser.BookSimilarity.compare(plain, similarPlain);
        double expected = Math.round((result.titleScore() * 0.70
            + result.overallLevenshtein() * 0.05
            + result.jaroWinkler() * 0.025
            + result.jaccard() * 0.025) / 0.8 * 10000) / 10000.0;
        Assertions.assertEquals(expected, result.score());
    }

    @Test
    void runWithDupSameName() throws Exception {
        Files.write(tmpDir.resolve("dup.txt"), "xxx".getBytes());
        tmpDir.resolve("dir").toFile().mkdirs();
        Files.write(tmpDir.resolve("dir", "dup.txt"), "xxx".getBytes());
        instance().run(args.arg("util", "dup")
            .arg("path", tmpDir.toString())
            .arg("dupOnlySameName", "true")
        );
    }

    @Test
    void runWithDupToJson() throws Exception {
        Files.write(tmpDir.resolve("dup.txt"), "xxx".getBytes());
        tmpDir.resolve("dir").toFile().mkdirs();
        Files.write(tmpDir.resolve("dir", "dup.txt"), "xxx".getBytes());
        instance().run(args.arg("util", "dup")
            .arg("path", tmpDir.toString())
            .arg("dupOutjson", "true")
        );
    }

    @Test
    void runWithSplit() throws Exception {
        instance().run(args.arg("util", "split")
            .arg("path", INPUT_PATH)
            .arg("splitIdx", "2")
            .arg("splitDirection", "back")
        );
    }

    @Test
    void runWithCount() throws Exception {
        instance().run(args
            .arg("util", "count")
            .arg("path", INPUT_PATH)
            .arg("samplePath", SAMPLE_PATH)
            .arg("threadSize", "2")
        );
    }

    @Test
    void runWithCount1() throws Exception {
        String output = tmpDir.resolve("runWithCount1.txt").toString();
        instance().run(args
            .arg("util", "count")
            .arg("path", INPUT_PATH)
            .arg("out", output)
            .arg("samplePath", SAMPLE_PATH)
            .arg("threadSize", "2")
        );
        Assertions.assertTrue(FileUtils.readFileToString(new File(output), StandardCharsets.UTF_8).startsWith("道"));
    }

    @Test
    void runWithDeleteFromFileList() throws Exception {
        var first = Files.writeString(tmpDir.resolve("first.txt"), "first");
        var second = Files.writeString(tmpDir.resolve("second.txt"), "second");
        var list = Files.writeString(tmpDir.resolve("files.txt"),
            first + "\n# ignored\n\n" + second + "\n");

        instance().run(args.arg("util", "delete").arg("path", list.toString()));

        Assertions.assertFalse(Files.exists(first));
        Assertions.assertFalse(Files.exists(second));
    }

    @Test
    void runWithDeleteFromJson() throws Exception {
        var remove = Files.writeString(tmpDir.resolve("remove.txt"), "remove");
        var keep = Files.writeString(tmpDir.resolve("keep.txt"), "keep");
        var list = Files.writeString(tmpDir.resolve("files.json"), """
            [
              {"file":"%s", "delete":true, "ext1":"a", "ext2":"b"},
              {"file":"%s", "delete":false, "ext1":"c", "ext2":"d"}
            ]
            """.formatted(remove, keep));

        instance().run(args.arg("util", "delete").arg("path", list.toString()));

        Assertions.assertFalse(Files.exists(remove));
        Assertions.assertTrue(Files.exists(keep));
    }

    @Test
    void runWithForeachDryRunAndPlaceholders() throws Exception {
        var target = Files.writeString(tmpDir.resolve("alpha.txt"), "alpha");
        Files.writeString(tmpDir.resolve("skip.bin"), "skip");
        var foreachArgs = args.arg("util", "foreach")
            .arg("path", tmpDir.toString())
            .arg("filter", "alpha\\.txt")
            .arg("foreachDo", "touch $order-$filenoext.$ext.processed $file.processed %file.percentprocessed");

        instance().run(foreachArgs);
        Assertions.assertFalse(Files.exists(tmpDir.resolve("1-alpha.txt.processed")));
        Assertions.assertFalse(Files.exists(tmpDir.resolve("alpha.txt.processed")));
        Assertions.assertFalse(Files.exists(tmpDir.resolve("alpha.txt.percentprocessed")));

        instance().run(foreachArgs.arg("dry-run", "false"));
        Assertions.assertTrue(Files.exists(tmpDir.resolve("1-alpha.txt.processed")));
        Assertions.assertTrue(Files.exists(tmpDir.resolve("alpha.txt.processed")));
        Assertions.assertTrue(Files.exists(tmpDir.resolve("alpha.txt.percentprocessed")));
        Assertions.assertTrue(Files.exists(target));
    }

    @Test
    void runWithForeachFolders() throws Exception {
        var child = Files.createDirectories(tmpDir.resolve("child"));

        instance().run(args.arg("util", "foreach")
            .arg("path", tmpDir.toString())
            .arg("folder", "true")
            .arg("filter", ".*")
            .arg("dry-run", "false")
            .arg("foreachDo", "touch $file/.visited"));

        Assertions.assertFalse(Files.exists(tmpDir.resolve(".visited")));
        Assertions.assertTrue(Files.exists(child.resolve(".visited")));
    }

    @Test
    void runWithClippingsToMd() throws Exception {
        Path path1 = tmpDir.resolve("clippingsToMdWithKindle.md");
        instance().run(args.arg("util", "clippings2Md")
            .arg("path", Utils.getTestResourcePath("/clippingsKindle.txt")).arg("out", path1.toString()));
        Assertions.assertTrue(path1.toFile().exists());
    }

    @Test
    void runWithClippingsToMdMrexpt() throws Exception {
        Path path1 = tmpDir.resolve("clippingsToMdWithMrexpt.md");
        instance().run(
            args.arg("util", "clippings2Md")
                .arg("path", Utils.getTestResourcePath("/clippingsMrexpt.mrexpt"))
                .arg("out", path1.toString())
                .arg("clippingsToMdType", FileTools.ClippingsToMd.ClipType.mrexpt.name())
        );
        Assertions.assertTrue(path1.toFile().exists());
    }

    @Test
    void runWithClippingsToMdNoOut() throws Exception {
        Path path1 = tmpDir.resolve("runWithNoOut.md");
        var origin =  Utils.getTestResourcePath("/clippingsMrexpt.mrexpt");
        var newFile = tmpDir.resolve("runWithNoOut.mrexpt");
        FileUtils.copyFile(new File(origin), newFile.toFile());
        instance().run(
            args.arg("util", "clippings2Md")
                .arg("path", newFile.toString())
                .arg("clippingsToMdType", FileTools.ClippingsToMd.ClipType.mrexpt.name())
        );
    }


    @Test
    void runWithZipMerger() throws Exception {
        Path firstZip = tmpDir.resolve("first.zip");
        Path secondZip = tmpDir.resolve("second.zip");
        Path mergedZip = tmpDir.resolve("merged.zip");
        writeZip(firstZip, List.of(
            new ZipTestEntry("folder/first.txt", "first", false),
            new ZipTestEntry("second.log", "second", true)
        ));
        writeZip(secondZip, List.of(new ZipTestEntry("third.txt", "third", false)));

        instance().run(
            args.arg("util", "zipMerger")
                .arg("path", String.join(",", firstZip.toString(), secondZip.toString()))
                .arg("out", mergedZip.toString())
        );

        try (ZipFile zip = ZipFile.builder().setPath(mergedZip).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            List<String> names = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<Integer> methods = new ArrayList<>();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                names.add(entry.getName());
                methods.add(entry.getMethod());
                try (InputStream input = zip.getInputStream(entry)) {
                    contents.add(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                }
            }

            Assertions.assertEquals(List.of("1.txt", "2.log", "3.txt"), names);
            Assertions.assertEquals(List.of("first", "second", "third"), contents);
            Assertions.assertEquals(List.of(ZipArchiveEntry.DEFLATED, ZipArchiveEntry.STORED, ZipArchiveEntry.DEFLATED), methods);
        }
    }

    @Test
    void runWithZipMergerRejectsInputAsOutput() throws Exception {
        Path inputZip = tmpDir.resolve("input.zip");
        writeZip(inputZip, List.of(new ZipTestEntry("first.txt", "first", false)));

        Assertions.assertThrows(IllegalArgumentException.class, () -> instance().run(
            args.arg("util", "zipMerger")
                .arg("path", inputZip.toString())
                .arg("out", inputZip.toString())
        ));

        try (ZipFile zip = ZipFile.builder().setPath(inputZip).get()) {
            Assertions.assertTrue(zip.getEntries().hasMoreElements());
        }
    }

    @Test
    void runWithZipMergerDoesNotReplaceExistingOutput() throws Exception {
        Path inputZip = tmpDir.resolve("input-existing-output.zip");
        Path outputZip = tmpDir.resolve("existing-output.zip");
        writeZip(inputZip, List.of(new ZipTestEntry("first.txt", "first", false)));
        Files.writeString(outputZip, "keep existing file");

        Assertions.assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> instance().run(
            args.arg("util", "zipMerger")
                .arg("path", inputZip.toString())
                .arg("out", outputZip.toString())
        ));

        Assertions.assertEquals("keep existing file", Files.readString(outputZip));
    }

    private void writeZip(Path path, List<ZipTestEntry> entries) throws Exception {
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(path.toFile())) {
            for (ZipTestEntry testEntry : entries) {
                byte[] content = testEntry.content().getBytes(StandardCharsets.UTF_8);
                ZipArchiveEntry entry = new ZipArchiveEntry(testEntry.name());
                if (testEntry.stored()) {
                    CRC32 crc = new CRC32();
                    crc.update(content);
                    entry.setMethod(ZipArchiveEntry.STORED);
                    entry.setSize(content.length);
                    entry.setCompressedSize(content.length);
                    entry.setCrc(crc.getValue());
                }
                zip.putArchiveEntry(entry);
                zip.write(content);
                zip.closeArchiveEntry();
            }
        }
    }

    private record ZipTestEntry(String name, String content, boolean stored) {
    }

}