package net.cofcool.toolbox.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class DiffAnalysisTest extends BaseTest {

    static String diff = """
        diff --git a/README.md b/README.md
        index 5432362..0dfd6cf 100644
        --- a/README.md
        +++ b/README.md
        @@ -7 +7 @@
        -使用 Java（要求 JDK 21, 推荐使用 `GraalVM`）、Python、Go 等语言实现
        +使用 Java（要求 JDK 21, 推荐使用 `GraalVM`）, Python 等语言实现
        """;

    static String unCommitDiff = """
         diff --git a/README.md b/README.md
         index 5432362..0d006d0 100644
         --- a/README.md
         +++ b/README.md
         @@ -109,0 +110,6 @@
         +## Git diff 分析
         +
         +* 根据 diff 抽取对应行的变更记录
         +
         +使用: `git diff --unified=0 --diff-filter=M . | ./mytool.sh --tool=analysisDiff --path=./ > diff.csv`
         """;

    @Override
    protected Tool instance() {
        return new DiffAnalysis();
    }

    @Test
    void run() throws Exception {
        System.setIn(new ByteArrayInputStream(diff.getBytes(StandardCharsets.UTF_8)));
        instance().run(args.arg("path", System.getProperty("user.dir")).arg("logSize", "3"));
    }
    @Test
    void runWithUnCommit() throws Exception {
        System.setIn(new ByteArrayInputStream(unCommitDiff.getBytes(StandardCharsets.UTF_8)));
        instance().run(args.arg("path", System.getProperty("user.dir")).arg("uncommit", "true"));
    }
}