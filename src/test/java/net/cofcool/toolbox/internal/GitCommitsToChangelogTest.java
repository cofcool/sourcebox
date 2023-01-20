package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class GitCommitsToChangelogTest {

    @Test
    void runWithTag() throws Exception {
        new GitCommitsToChangelog().run(new Tool.Args().arg("path", Utils.getTestResourcePath("/")).arg("tag", "1.0.0").arg("out", "./target/changelog-runWithTag.md"));
    }

    @Test
    void run() throws Exception {
        new GitCommitsToChangelog().run(new Tool.Args().arg("path", Utils.getTestResourcePath("/")));
    }

    @Test
    void runWithLogPath() throws Exception {
        new GitCommitsToChangelog().run(new Tool.Args().arg("log", Utils.getTestResourcePath("/gitCommitsToChangelogTest.txt")).arg("out", "./target/changelog-runWithLogPath.md"));
    }
}