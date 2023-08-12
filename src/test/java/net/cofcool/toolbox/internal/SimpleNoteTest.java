package net.cofcool.toolbox.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Duration;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.internal.simplenote.NoteConfig;
import org.junit.jupiter.api.Test;

class SimpleNoteTest extends BaseTest {

    @Override
    protected Tool instance() {
        return new SimpleNote();
    }

    @Test
    void run() throws Exception {
        var s = "./target";
        instance().run(new Args().arg(NoteConfig.PATH_KEY, s).arg(NoteConfig.PORT_KEY, "18888"));
        Thread.sleep(Duration.ofSeconds(5));
        assertTrue(new File(s + "/" + NoteConfig.FILE_NAME).exists());
    }
}