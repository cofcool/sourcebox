package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class GitCommitsToChangelogTest extends BaseTest {

    @Override
    protected Tool instance() {
        return new GitCommitsToChangelog();
    }

    @Test
    void runWithTag() throws Exception {
        new GitCommitsToChangelog().run(args.arg("path", Utils.getTestResourcePath("/")).arg("tag", "1.0.0").arg("out", "./target/changelog-runWithTag.md"));
    }

    @Test
    void run() throws Exception {
        new GitCommitsToChangelog().run(args.arg("path", Utils.getTestResourcePath("/")));
    }

    @Test
    void runWithLogPath() throws Exception {
        new GitCommitsToChangelog().run(args.arg("log", Utils.getTestResourcePath("/gitCommitsToChangelogTest.txt")).arg("out", "./target/changelog-runWithLogPath.md"));
    }

    @Test
    void runWithNoTag() throws Exception {
        new GitCommitsToChangelog().run(args.arg("path", Utils.getTestResourcePath("/"))
                .arg("out", "./target/changelog-runWithNoTag.md")
                .arg("no-tag", "true")
        );
    }
}