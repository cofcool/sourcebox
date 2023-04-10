package net.cofcool.toolbox.internal;

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
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

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
        var newFileName = nameGenerator.name(baseName) + "." + ext;
        var newName = new File(fullPath + newFileName);

        Object ret;
        var dest = args.readArg("dest").getVal();
        if (dest.isPresent()) {
            var target = Path.of(dest.get(), newFileName);
            try {
                new File(dest.get()).mkdirs();
                ret = Files.move(file.toPath(), target);
            } catch (IOException e) {
                throw new IllegalStateException("Move to " + target +  " error", e);
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
                .arg(new Arg("formatter", Formatter.order.name(), "new name formatter, like " + Arrays.toString(Formatter.values()), false, null));
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
        urlencoded(UrlNameDecoder::new);

        private final Supplier<NameGenerator> supplier;

        Formatter(Supplier<NameGenerator> supplier) {
            this.supplier = supplier;
        }

        NameGenerator getGenerator() {
            return supplier.get();
        }
    }

    private interface NameGenerator {

        String name(String old);

        default void enterDir() {

        }
    }

    private static class OrderGenerator implements NameGenerator {

        private int i = 0;

        @Override
        public String name(String old) {
            i++;
            return String.format("%s-%03d", old, i);
        }

        @Override
        public void enterDir() {
            i = 0;
        }
    }

    private static class UrlNameDecoder implements NameGenerator {

        @Override
        public String name(String old) {
           return URLDecoder.decode(old, StandardCharsets.UTF_8);
        }
    }

    private static class DateGenerator extends OrderGenerator {

        private final DateTimeFormatter dateTimeFormatter;

        public DateGenerator(DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public String name(String old) {
            return super.name(old + "-" + dateTimeFormatter.format(LocalDate.now()));
        }
    }
}
