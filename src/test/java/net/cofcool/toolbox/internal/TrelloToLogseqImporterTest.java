package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Utils;
import org.junit.jupiter.api.Test;

public class TrelloToLogseqImporterTest extends BaseTest {


    public static final String RESOURCE_PATH = Utils.getTestResourcePath("/trello-demo.json");

    @Test
    void run() throws Exception {
        instance().run(args.arg("path", RESOURCE_PATH).arg("out", "./target/trello"));
    }

    @Test
    void runWithTitleToPage() throws Exception {
        instance()
            .run(
                args
                    .arg("path", RESOURCE_PATH)
                    .arg("out", "./target/trello")
                    .arg("titleToPage", "true")
            );
    }

    @Override
    protected Tool instance() {
        return new TrelloToLogseqImporter();
    }
}