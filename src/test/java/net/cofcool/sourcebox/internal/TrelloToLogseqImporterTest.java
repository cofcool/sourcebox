package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.trello.CheckItemsItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TrelloToLogseqImporterTest extends BaseTest {


    public static final String RESOURCE_PATH = Utils.getTestResourcePath("/trello-demo.json");

    @Test
    void run() throws Exception {
        instance().run(args.arg("path", RESOURCE_PATH).arg("out", "./target/trello"));
    }

    @Test
    @Disabled
    void runImportRecords() throws Exception {
        instance().run(args.arg("path", RESOURCE_PATH).arg("out", "./target/trello").arg("importRecords", "http://localhost:38080"));
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

    @Test
    void mapsChecklistStateToCurrentProgress() {
        var completed = TrelloToLogseqImporter.toCheckList(
            new CheckItemsItem(null, 0, null, null, null, "completed", null, "item-1", "complete"),
            "action-1");
        var incomplete = TrelloToLogseqImporter.toCheckList(
            new CheckItemsItem(null, 0, null, null, null, "incomplete", null, "item-2", "incomplete"),
            "action-1");

        Assertions.assertEquals(0, completed.first());
        Assertions.assertEquals(1, completed.last());
        Assertions.assertEquals(1, completed.current());
        Assertions.assertEquals(0, incomplete.first());
        Assertions.assertEquals(1, incomplete.last());
        Assertions.assertEquals(0, incomplete.current());
    }

    @Override
    protected Tool instance() {
        return new TrelloToLogseqImporter();
    }
}