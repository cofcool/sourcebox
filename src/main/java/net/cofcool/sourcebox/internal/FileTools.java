package net.cofcool.sourcebox.internal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class FileTools implements Tool {

    private final Map<String, Util> utilMap = Map.of(
        "split", new Split(),
        "count", new FileCounter()
    );

    @Override
    public ToolName name() {
        return ToolName.fileTools;
    }

    @Override
    public void run(Args args) throws Exception {
        var util = args.readArg("util").val();
        var outPath = args.readArg("out");

        var outStr = utilMap.get(util).run(args);
        if (outPath.isPresent()) {
            FileUtils.writeStringToFile(new File(outPath.val()), outStr, StandardCharsets.UTF_8);
        } else {
            args.getContext().write("Output:");
            args.getContext().write(outStr);
        }
    }

    private interface Util {

        String run(Args args) throws Exception;
    }

    private record Split() implements Util {

        @Override
        public String run(Args args) throws Exception {
            var path = args.readArg("path").val();
            var splitIdx = args.readArg("splitIdx");
            var splitChar = args.readArg("splitChar");
            var splitDirection = args.readArg("splitDirection");
            if (!splitChar.isPresent() && !splitIdx.isPresent()) {
                throw new IllegalArgumentException("splitIdx or splitChar must be specified");
            }

            return FileUtils.readLines(new File(path), StandardCharsets.UTF_8)
                .parallelStream()
                .map(s -> {
                    var i = splitIdx.isPresent() ?
                        Integer.parseInt(splitIdx.val()) : s.indexOf(splitChar.val());
                    if (i < 0 || i >= s.length()) {
                        return s;
                    } else {
                        var f = splitDirection.test(t -> t.equalsIgnoreCase("forward"));
                        return f ? s.substring(0, i) : s.substring(i);
                    }
                })
                .collect(Collectors.joining("\n"));
        }
    }

    private class FileCounter implements Util {

        @Override
        public String run(Args args) throws Exception {
            var filePath = args.readArg("path").val();
            var samplePath = args.readArg("samplePath").val();
            var threadSize = Integer.parseInt(args.readArg("threadSize").val());

            var strs = FileUtils.readLines(new File(samplePath), StandardCharsets.UTF_8);
            var countMap = new LinkedHashMap<String, AtomicInteger>(strs.size());
            for (String s : strs) {
                countMap.put(s, new AtomicInteger());
            }

            try (ExecutorService executor = Executors.newFixedThreadPool(threadSize)) {
                Path path = Paths.get(filePath);
                FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
                long fileSize = fileChannel.size();
                long chunkSize = fileSize / threadSize;
                for (int i = 0; i < threadSize; i++) {
                    long position = i * chunkSize;
                    if (i == threadSize - 1) {
                        chunkSize = fileSize - position;
                    }
                    executor.execute(new FileReadTask(fileChannel, position, chunkSize, countMap));
                    getLogger().info("Submit task with position {0} and chunkSize {1}", position, chunkSize);
                }
                executor.shutdown();
                if (executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    getLogger().debug("Close thread poll ok");
                }

                return countMap.entrySet().parallelStream()
                    .map(e -> e.getKey() + ": " + e.getValue().longValue())
                    .collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new IllegalStateException("Count file error", e);
            }
        }
    }

    private record FileReadTask(FileChannel fileChannel, long position, long chunkSize,
                                Map<String, AtomicInteger> counter) implements Runnable {

        @Override
        public void run() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate((int) chunkSize);
                fileChannel.read(buffer, position);
                buffer.flip();
                var str = new String(buffer.array(), StandardCharsets.UTF_8);
                counter.forEach((k, v) -> {
                    v.getAndAdd(StringUtils.countMatches(str, k));
                });
            } catch (IOException e) {
                throw new IllegalStateException("Read file error from " + position + " error", e);
            }
        }
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("util", null, "util name, support: " + utilMap.keySet(), true, "count"))
            .arg(new Arg("path", null, "input file path", true, "./demo.csv"))
            .arg(new Arg("out", null, "output file path, if none will use stdout", false, "./output.csv"))
            .arg(new Arg("samplePath", null, "sample file path, when using count, this parameter should be set", false, "./sample.csv"))
            .arg(new Arg("threadSize", "1", "count thread size, when using count, this parameter can be set", false, null))
            .arg(new Arg("splitIdx", null, "split by index, when using split, this parameter can be set", false, "2"))
            .arg(new Arg("splitChar", null, "split by character, when using split, this parameter can be set", false, "foo"))
            .arg(new Arg("splitDirection", "forward", "split direction, when using split, this parameter can be set, forward or back", false, "forward"));
    }
}
