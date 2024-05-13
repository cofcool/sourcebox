package net.cofcool.sourcebox.internal;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class FileNameFormatter implements Tool {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @Override
    public ToolName name() {
        return ToolName.rename;
    }

    @Override
    public void run(Args args) throws Exception {
        var root = new File(args.readArg("path").val());

        var nameGenerator = Formatter.valueOf(args.readArg("formatter").val()).getGenerator();
        if (FileUtils.isDirectory(root)) {
            Files.walkFileTree(
                    root.toPath(),
                    Collections.emptySet(),
                    Integer.MAX_VALUE,
                    new DirEnterVisitor(nameGenerator, (n, f) -> rename(f, args, n))
            );
        } else {
            rename(root, args, nameGenerator);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void rename(File file, Args args, NameGenerator nameGenerator) {
        var fullPath = FilenameUtils.getFullPath(file.getAbsolutePath());
        var baseName = args.readArg("base").getVal().orElse(FilenameUtils.getBaseName(file.getName()));
        var ignore = args.readArg("ignore").val();
        if (Pattern.compile(ignore).matcher(file.getName()).matches()) {
            getLogger().debug("Ignore " + file);
            return;
        }

        var ext = FilenameUtils.getExtension(file.getName());
        var newFileName = nameGenerator.name(baseName, StringUtils.isEmpty(ext) ? "" : "." + ext, args);
        var newName = new File(fullPath + newFileName);

        Object ret;
        var dest = args.readArg("dest").getVal();
        if (dest.isPresent()) {
            var target = Path.of(dest.get(), newFileName);
            try {
                new File(dest.get()).mkdirs();
                ret = Files.move(file.toPath(), target);
            } catch (IOException e) {
                throw new IllegalStateException("Move " + file + " to " + target +  " error", e);
            }
        } else {
            ret = file.renameTo(newName);
        }
        getLogger().info(String.format("rename file %s to %s: %s", file, newName, ret));
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("path", "./", "file path", false, null))
            .arg(new Arg("dest", null, "dest path", false, "/tmp/"))
            .arg(new Arg("base", null, "file base name, default is old name", false, "demo"))
            .arg(new Arg("ignore", "^\\..*", "ignore files with regular expression", false, null))
            .arg(new Arg("formatter", null, "new name formatter, like " + Arrays.toString(Formatter.values()), true, Formatter.order.name()))
            .alias("rename", name(), "path",  null);
    }

    private static class DirEnterVisitor extends SimpleFileVisitor<Path> {

        private final NameGenerator nameGenerator;
        private final BiConsumer<NameGenerator, File> consumer;

        private DirEnterVisitor(NameGenerator nameGenerator, BiConsumer<NameGenerator, File> consumer) {
            this.nameGenerator = nameGenerator;
            this.consumer = consumer;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            nameGenerator.enterDir();
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            consumer.accept(nameGenerator, file.toFile());
            return super.visitFile(file, attrs);
        }

    }

    enum Formatter {
        order(OrderGenerator::new),
        date(() -> new DateGenerator(DATE_FORMATTER)),
        datetime(() -> new DateGenerator(DATE_TIME_FORMATTER)),
        urlencoded(UrlNameDecoder::new),
        replace(Replace::new),
        delete(Delete::new),
        expression(ExpressionGenerator::new);

        private final Supplier<NameGenerator> supplier;

        Formatter(Supplier<NameGenerator> supplier) {
            this.supplier = supplier;
        }

        NameGenerator getGenerator() {
            return supplier.get();
        }

        @Override
        public String toString() {
            var help = supplier.get().help();
            return super.toString() + (help.isEmpty() ? "" : "(support args: " + help + ")");
        }
    }

    interface NameGenerator {

        String name(String old, String ext, Args args);

        default String help() {
            return "";
        }

        default void enterDir() {

        }
    }

    private static class OrderGenerator implements NameGenerator {

        static final int DEFAULT = 1;

        private int i = DEFAULT;
        protected Integer start;

        @Override
        public String name(String old, String ext, Args args) {
            if (start == null) {
                start = Integer.valueOf(args.getArgVal("start").orElse(DEFAULT + ""));
                i = start;
            }
            return String.format("%s-%03d%s", old, i++, ext);
        }

        @Override
        public void enterDir() {
            i = DEFAULT;
        }

        @Override
        public String help() {
            return "--start=" + DEFAULT;
        }
    }

    private static class UrlNameDecoder implements NameGenerator {

        @Override
        public String name(String old, String ext, Args args) {
            try {
                return URLDecoder.decode(old, StandardCharsets.UTF_8) + ext;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Decode " + old + " error: " + e.getMessage());
            }
        }
    }

    private static class ExpressionGenerator extends OrderGenerator {

        @Override
        public String name(String old, String ext, Args args) {
            if (start == null) {
                start = DEFAULT;
            }
            Map<String, Supplier<String>> exps = Map.of(
                "{old}", () -> old,
                "{suffix}",() -> ext,
                "{idx}",() -> String.format("%03d", start),
                "{padding}", () -> String.format("%03d", Integer.parseInt(old))
            );

            var expression = args.readArg("expression").val();
            Pattern pattern = Pattern.compile("\\{([^}]*)}");
            Matcher matcher = pattern.matcher(expression);

            while (matcher.find()) {
                var m = matcher.group(1);
                var key = STR."{\{m}}";
                System.out.println(key);
                var f = exps.get(key);
                if (f != null) {
                    expression = expression.replace(key, String.valueOf(f.get()));
                }

            }

            start++;
            return expression;
        }

        @Override
        public String help() {
            return "--expression='{old}'-'{idx}{suffix}'";
        }
    }

    private static class Replace implements NameGenerator {

        @Override
        public String name(String old, String ext, Args args) {
            return (old + ext).replace(args.readArg("old").val(), args.readArg("new").val());
        }

        @Override
        public String help() {
            return "--old=demo --new=test";
        }
    }

    private static class Delete implements NameGenerator {

        @Override
        public String name(String old, String ext, Args args) {
            return (old + ext).replace(args.readArg("old").val(), "");
        }

        @Override
        public String help() {
            return "--old=demo";
        }
    }

    private static class DateGenerator extends OrderGenerator {

        private final DateTimeFormatter dateTimeFormatter;

        public DateGenerator(DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public String name(String old, String ext, Args args) {
            return super.name(old + "-" + dateTimeFormatter.format(LocalDate.now()), ext, args);
        }
    }
}
