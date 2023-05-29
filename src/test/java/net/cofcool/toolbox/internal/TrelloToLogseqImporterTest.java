package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class TrelloToLogseqImporterTest extends BaseTest {


    @Test
    void run() throws Exception {
        instance().run(args.arg("path", Utils.getTestResourcePath("/trello-demo.json")).arg("out", "./target/trello"));
    }

    @Test
    void runWithTitleToPage() throws Exception {
        instance()
            .run(
                args
                    .arg("path", Utils.getTestResourcePath("/trello-demo.json"))
                    .arg("out", "./target/trello")
                    .arg("titleToPage", "true")
            );
    }

    @Override
    protected Tool instance() {
        return new TrelloToLogseqImporter();
    }
}