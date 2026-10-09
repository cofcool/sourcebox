package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileDuplicateParser implements Tool.SimpleTool {

    private boolean outjson;
    private boolean onlySameName;

    @Override
    public String run(Tool.Args args) throws Exception {
        Path root = Paths.get(args.readArg("path").optVal().orElse("."));
        long minSize = Long.parseLong(args.readArg("dupMinSize").val());
        Set<String> ignoreDirs = new HashSet<>();
        args.readArg("dupIgnore").accept(a -> ignoreDirs.addAll(List.of(a.val().split(","))));
        outjson = args.readArg("dupOutjson").test(Boolean::parseBoolean);
        onlySameName = args.readArg("dupOnlySameName").test(Boolean::parseBoolean);


        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("Root not found or not a directory: " + root);
        }

        List<ParsedBook> files = new LinkedList<>();
        EnumSet<FileVisitOption> opts = EnumSet.noneOf(FileVisitOption.class);

        Files.walkFileTree(root, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path name = dir.getFileName();
                if (name != null && ignoreDirs.contains(name.toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                if (attrs.size() < minSize) return FileVisitResult.CONTINUE;

                files.add(BookNameParser.parse(file));
                return FileVisitResult.CONTINUE;
            }
        });

        return readGroups(files);
    }

    private String readGroups(List<ParsedBook> files) {

        Map<String, List<Path>> groups = new HashMap<>();

        for (int i = 0; i < files.size(); i++) {
            var book = files.get(i);

            if (onlySameName) {
                groups.compute(book.original, (k,v) -> {
                    var nv = (v == null) ? new ArrayList<Path>() : v;
                    nv.add(book.file);
                    return nv;
                });
            } else {
                List<Path> list = groups.computeIfAbsent(book.original, k -> {
                    var l = new ArrayList<Path>();
                    l.add(book.file);
                    return l;
                });
                for (int j = i + 1; j < files.size(); j++) {
                    var file = files.get(j);
                    var r = BookSimilarity.compare(book, file);
                    if (r.isProbablySameBook()) {
                        list.add(file.file);
                    }
                }
            }
        }

        Stream<Map.Entry<String, List<Path>>> stream = groups.entrySet().stream()
                .filter(e -> e.getValue().size() >= 2)
                .sorted(Map.Entry.comparingByKey());
        if (outjson) {
            return JsonUtil.toJson(
                    stream
                            .map(e ->
                                    e.getValue()
                                            .stream()
                                            .map(s -> new GroupItem(getFull(s), false, e.getKey()))
                                            .collect(Collectors.toList())
                            )
                            .reduce(new ArrayList<>(), (o1, o2) -> {
                                o1.addAll(o2);
                                return o1;
                            })
            );
        } else {
            return stream
                    .map(e -> {
                        var str = new StringBuilder().append("---").append(e.getKey()).append("---").append("\n");
                        for (Path p : e.getValue()) {
                            str.append(getFull(p)).append("\n");
                        }
                        return str.toString();
                    })
                    .collect(Collectors.joining("\n\n"));
        }
    }

    private static String getFull(Path p) {
        String full;
        try {
            full = p.toRealPath().toString();
        } catch (IOException ex) {
            full = p.toAbsolutePath().normalize().toString();
        }
        return full;
    }

    public record GroupItem (
            String file,
            boolean delete,
            String group
    ){}

    static class BookNameParser {

        private static final Pattern YEAR =
                Pattern.compile(
                        "(?<!\\d)(19\\d{2}|20\\d{2})(?!\\d)"
                );

        private static final Pattern VOLUME =
                Pattern.compile(
                        "(?i)(?:第\\s*(\\d+|[一二三四五六七八九十百]+)\\s*" +
                                "(部|册|卷|篇|章)|" +
                                "(上册|下册|上卷|下卷|上部|下部)|" +
                                "([ivxlcdm]+))"
                );


        private static final Pattern AUTHOR =
                Pattern.compile(
                        "(?i)(?:作者|author)\\s*(?:[:：=]|\\p{Pd})\\s*([^\\[\\]【】(){}《》,，;；|]+)"
                );

        private static final Pattern BRACKET =
                Pattern.compile(
                        "\\[([^\\]]+)]|【([^】]+)】|\\(([^()]+)\\)|\\{([^{}]+)}|《([^》]+)》"
                );


        private static final Pattern TOKEN =
                Pattern.compile(
                        "[\\p{L}\\p{N}]+"
                );

        public static ParsedBook parse(Path file) {

            String original = file.getFileName().toString();

            String name = FileNameParser.normalize(original);

            Set<String> bracketInfo =
                    extractBracketInfo(original);

            Integer year =
                    extractYear(name);

            VolumeResult volume =
                    extractVolume(name);

            String withoutVolume =
                    removeVolume(name);

            withoutVolume =
                    YEAR.matcher(withoutVolume)
                            .replaceAll(" ");


            String author =
                    extractAuthor(original, bracketInfo);


            String title =
                    removeAuthor(withoutVolume, author);


            title =
                    cleanTitle(title);

            Set<String> tokens =
                    tokenize(title);

            return new ParsedBook(
                    original,
                    name,
                    title,
                    author,
                    year,
                    volume.number,
                    volume.text,
                    tokens,
                    bracketInfo,
                    file
            );
        }

        private static Set<String> extractBracketInfo(String text) {
            Set<String> result =
                    new LinkedHashSet<>();

            Matcher matcher =
                    BRACKET.matcher(text);

            while (matcher.find()) {

                                String value = null;
                                for (int group = 1; group <= matcher.groupCount(); group++) {
                                        if (matcher.group(group) != null) {
                                                value = matcher.group(group).trim();
                                                break;
                                        }
                                }

                                if (value != null && !value.isEmpty()) {
                    result.add(value);
                }
            }

            return result;
        }

        private static Integer extractYear(String text) {

            Matcher matcher =
                    YEAR.matcher(text);

            if (matcher.find()) {
                return Integer.parseInt(
                        matcher.group(1)
                );
            }

            return null;
        }

        private static VolumeResult extractVolume(
                String text
        ) {

            Matcher matcher =
                    VOLUME.matcher(text);

            if (!matcher.find()) {
                return new VolumeResult(
                        null,
                        null
                );
            }

            String number =
                    matcher.group(1);

            String special =
                    matcher.group(3);

            if (special != null) {

                return new VolumeResult(
                        switch (special) {
                            case "上册", "上卷", "上部" -> 1;
                            case "下册", "下卷", "下部" -> 2;
                            default -> null;
                        },
                        special
                );
            }

            if (number == null) {
                return new VolumeResult(
                        null,
                        matcher.group()
                );
            }

            Integer volume =
                    parseChineseNumber(number);

            return new VolumeResult(
                    volume,
                    matcher.group()
            );
        }

        private static String removeVolume(
                String text
        ) {

            return VOLUME.matcher(text)
                    .replaceAll(" ");
        }

        private static String extractAuthor(
                String text,
                Set<String> bracketInfo
        ) {

            Matcher matcher =
                    AUTHOR.matcher(text);

            if (matcher.find()) {
                return clean(matcher.group(1));
            }

                        for (String feature : bracketInfo) {
                                matcher = AUTHOR.matcher(feature);
                                if (matcher.find()) {
                                        return clean(matcher.group(1));
                                }
                        }


            if (bracketInfo.size() == 1) {

                String value =
                        bracketInfo.iterator().next();

                if (looksLikeAuthor(value)) {
                    return clean(value);
                }
            }

            return "";
        }

        private static boolean looksLikeAuthor(
                String value
        ) {

            String lower =
                    value.toLowerCase(Locale.ROOT);


            String[] ignored = {
                    "pdf",
                    "epub",
                    "mobi",
                    "高清",
                    "完整版",
                    "全集",
                    "电子书",
                    "精校",
                    "扫描版",
                    "译者"
            };

            for (String item : ignored) {
                if (lower.contains(item)) {
                    return false;
                }
            }

            return true;
        }

        private static String removeAuthor(
                String title,
                String author
        ) {

            if (author == null ||
                    author.isBlank()) {

                return title;
            }

            return title
                    .replace(
                            author,
                            " "
                    );
        }

        private static String cleanTitle(String title) {
            title = title.replaceAll(
                    "\\[[^\\]]*]|【[^】]*】|《[^》]*》",
                    " "
            );

            title =
                    title.replaceAll(
                            "\\([^)]*\\)",
                            " "
                    );

            title =
                    title.replaceAll(
                            "\\{[^}]*}",
                            " "
                    );


            title =
                    title.replaceAll(
                            "(?i)\\b(ebook|epub|pdf|mobi)\\b",
                            " "
                    );


            title =
                    title.replaceAll(
                            "[^\\p{L}\\p{N}\\s]+",
                            " "
                    );

            return clean(title);
        }

        private static String clean(
                String text
        ) {

            return text
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toLowerCase(Locale.ROOT);
        }

        private static Set<String> tokenize(
                String text
        ) {

            Set<String> result =
                    new LinkedHashSet<>();

            Matcher matcher =
                    TOKEN.matcher(text);

            while (matcher.find()) {

                result.add(
                        matcher.group()
                                .toLowerCase(Locale.ROOT)
                );
            }

            return result;
        }

        private static Integer parseChineseNumber(
                String text
        ) {

            if (text.matches("\\d+")) {
                return Integer.parseInt(text);
            }

            Map<Character, Integer> map =
                    Map.of(
                            '一', 1,
                            '二', 2,
                            '三', 3,
                            '四', 4,
                            '五', 5,
                            '六', 6,
                            '七', 7,
                            '八', 8,
                            '九', 9,
                            '十', 10
                    );

            if (text.equals("十")) {
                return 10;
            }

            if (text.length() == 2 &&
                    text.charAt(0) == '十') {

                return 10 +
                        map.getOrDefault(
                                text.charAt(1),
                                0
                        );
            }

            if (text.length() == 2 &&
                    text.charAt(1) == '十') {

                return map.get(
                        text.charAt(0)
                ) * 10;
            }

            if (text.length() == 3 &&
                    text.charAt(1) == '十') {

                return map.get(
                        text.charAt(0)
                ) * 10 +
                        map.get(
                                text.charAt(2)
                        );
            }

            return null;
        }

        private record VolumeResult(
                Integer number,
                String text
        ) {
        }
    }

    public record ParsedBook(
            String original,


            String normalized,


            String title,


            String author,


            Integer year,


            Integer volume,


            String volumeText,


            Set<String> tokens,


            Set<String> bracketInfo,
            Path file
    ) {
    }

    static class BookSimilarity {

        public static SimilarityResult compare(
                ParsedBook a,
                ParsedBook b
        ) {


            double titleScore =
                    SimilarityUtils.combined(
                            a.title(),
                            b.title(),
                            a.tokens(),
                            b.tokens()
                    );


            double authorScore;

            if (a.author().isEmpty() ||
                    b.author().isEmpty()) {

                authorScore = 0.5;

            } else {

                authorScore =
                        SimilarityUtils.combined(
                                a.author(),
                                b.author(),
                                java.util.Set.of(a.author()),
                                java.util.Set.of(b.author())
                        );
            }


            double yearScore;

            if (a.year() == null ||
                    b.year() == null) {

                yearScore = 0.5;

            } else {

                yearScore =
                        a.year().equals(b.year())
                                ? 1.0
                                : 0.0;
            }


            double volumeScore;

            if (a.volume() == null ||
                    b.volume() == null) {

                volumeScore = 0.5;

            } else {

                volumeScore =
                        a.volume().equals(b.volume())
                                ? 1.0
                                : 0.0;
            }


            double levenshtein =
                    SimilarityUtils.levenshtein(
                            a.normalized(),
                            b.normalized()
                    );

            double jaro =
                    SimilarityUtils.jaro(
                            a.normalized(),
                            b.normalized()
                    );

            double jaccard =
                    SimilarityUtils.jaccard(
                            a.tokens(),
                            b.tokens()
                    );

            boolean hasFeatures = hasComparableFeatures(a, b);

            double score;
            if (hasFeatures) {
                score =
                        titleScore * 0.70 +
                                authorScore * 0.1 +
                                yearScore * 0.05 +
                                volumeScore * 0.05 +
                                levenshtein * 0.05 +
                                jaro * 0.025 +
                                jaccard * 0.025;
            } else {
                score = (
                        titleScore * 0.70 +
                                levenshtein * 0.05 +
                                jaro * 0.025 +
                                jaccard * 0.025
                ) / 0.8;
            }


            if (a.volume() != null &&
                    b.volume() != null &&
                    !a.volume().equals(b.volume())) {

                score *= 0.65;
            }


            if (a.year() != null &&
                    b.year() != null &&
                    !a.year().equals(b.year())) {

                score *= 0.90;
            }

            score =
                    Math.round(score * 10000)
                            / 10000.0;

            List<String> reasons =
                    buildReasons(
                            titleScore,
                            authorScore,
                            yearScore,
                            volumeScore,
                            score
                    );

            return new SimilarityResult(
                    score,
                    titleScore,
                    authorScore,
                    yearScore,
                    volumeScore,
                    levenshtein,
                    jaccard,
                    jaro,
                    reasons
            );
        }

                private static boolean hasComparableFeatures(ParsedBook a, ParsedBook b) {
                    return (!a.author().isBlank() && !b.author().isBlank())
                            || (a.year() != null && b.year() != null)
                            || (a.volume() != null && b.volume() != null);
                }

        private static List<String> buildReasons(
                double title,
                double author,
                double year,
                double volume,
                double score
        ) {

            List<String> result =
                    new ArrayList<>();

            result.add(
                    String.format(
                            "书名相似度: %.2f",
                            title
                    )
            );

            result.add(
                    String.format(
                            "作者相似度: %.2f",
                            author
                    )
            );

            result.add(
                    String.format(
                            "年份匹配度: %.2f",
                            year
                    )
            );

            result.add(
                    String.format(
                            "卷号匹配度: %.2f",
                            volume
                    )
            );

            if (score >= 0.90) {
                result.add("高度相似");
            } else if (score >= 0.75) {
                result.add("很可能是同一本书");
            } else if (score >= 0.60) {
                result.add("存在一定相似性");
            } else {
                result.add("相似度较低");
            }

            return result;
        }
    }

    public record SimilarityResult(
            double score,

            double titleScore,

            double authorScore,

            double yearScore,

            double volumeScore,

            double overallLevenshtein,

            double jaccard,

            double jaroWinkler,

            List<String> reasons
    ) {

        public boolean isSameBook() {
            return score >= 0.85;
        }

        public boolean isProbablySameBook() {
            return score >= 0.70;
        }
    }

    static class SimilarityUtils {
        private static final LevenshteinDistance LEVENSHTEIN =
                LevenshteinDistance.getDefaultInstance();

        private static final JaroWinklerSimilarity JARO =
                new JaroWinklerSimilarity();


        public static double levenshtein(
                String a,
                String b
        ) {

            if (a.equals(b)) {
                return 1;
            }

            if (a.isEmpty() || b.isEmpty()) {
                return 0;
            }

            int distance =
                    LEVENSHTEIN.apply(a, b);

            return 1.0 -
                    (double) distance /
                            Math.max(
                                    a.length(),
                                    b.length()
                            );
        }


        public static double jaro(
                String a,
                String b
        ) {

            if (a.isEmpty() || b.isEmpty()) {
                return 0;
            }

            return JARO.apply(a, b);
        }


        public static double jaccard(
                Set<String> a,
                Set<String> b
        ) {

            if (a.isEmpty() && b.isEmpty()) {
                return 1;
            }

            if (a.isEmpty() || b.isEmpty()) {
                return 0;
            }

            Set<String> intersection =
                    new HashSet<>(a);

            intersection.retainAll(b);

            Set<String> union =
                    new HashSet<>(a);

            union.addAll(b);

            return (double) intersection.size()
                    / union.size();
        }


        public static double contains(
                String a,
                String b
        ) {

            if (a.equals(b)) {
                return 1;
            }

            if (a.contains(b) ||
                    b.contains(a)) {

                return 0.9;
            }

            return 0;
        }


        public static double combined(
                String a,
                String b,
                Set<String> tokensA,
                Set<String> tokensB
        ) {

            double levenshtein =
                    levenshtein(a, b);

            double jaro =
                    jaro(a, b);

            double jaccard =
                    jaccard(tokensA, tokensB);

            double contains =
                    contains(a, b);

            double character =
                    characterSimilarity(a, b);

            return
                    levenshtein * 0.30 +
                            jaro * 0.20 +
                            jaccard * 0.15 +
                            contains * 0.10 +
                            character * 0.25;
        }

        private static double characterSimilarity(
                String a,
                String b
        ) {
            String first = a.replaceAll("\\s+", "");
            String second = b.replaceAll("\\s+", "");

            if (first.equals(second)) {
                return 1;
            }

            int firstLength = first.codePointCount(0, first.length());
            int secondLength = second.codePointCount(0, second.length());
            if (firstLength == 0 || secondLength == 0) {
                return 0;
            }

            double unigram = characterNgramDice(first, second, 1);
            if (Math.min(firstLength, secondLength) <= 2) {
                return unigram;
            }

            double bigram = characterNgramDice(first, second, 2);
            return unigram * 0.4 + bigram * 0.6;
        }

        private static double characterNgramDice(
                String a,
                String b,
                int n
        ) {
            Map<String, Integer> first = characterNgrams(a, n);
            Map<String, Integer> second = characterNgrams(b, n);
            int shared = first.entrySet().stream()
                    .mapToInt(entry -> Math.min(entry.getValue(), second.getOrDefault(entry.getKey(), 0)))
                    .sum();
            int total = first.values().stream().mapToInt(Integer::intValue).sum()
                    + second.values().stream().mapToInt(Integer::intValue).sum();
            return total == 0 ? 0 : 2.0 * shared / total;
        }

        private static Map<String, Integer> characterNgrams(
                String text,
                int n
        ) {
            int[] codePoints = text.codePoints().toArray();
            Map<String, Integer> result = new HashMap<>();
            for (int i = 0; i <= codePoints.length - n; i++) {
                String gram = new String(codePoints, i, n);
                result.merge(gram, 1, Integer::sum);
            }
            return result;
        }
    }

    private static class FileNameParser {

        public static String removePath(String name) {

            int slash = Math.max(
                    name.lastIndexOf('/'),
                    name.lastIndexOf('\\')
            );

            if (slash >= 0) {
                return name.substring(slash + 1);
            }

            return name;
        }

        public static String removeExtension(String name) {
            int index = name.lastIndexOf('.');

            if (index <= 0) {
                return name;
            }

            return name.substring(0, index);
        }

        public static String normalize(String name) {

            if (name == null) {
                return "";
            }

            name = removePath(name);

            name = removeExtension(name);


            name = Normalizer.normalize(
                    name,
                    Normalizer.Form.NFKC
            );


            name = name.replaceAll(
                    "[._\\-]+",
                    " "
            );


            name = name.replaceAll(
                    "\\s+",
                    " "
            );

            return name.trim()
                    .toLowerCase(Locale.ROOT);
        }
    }
}
