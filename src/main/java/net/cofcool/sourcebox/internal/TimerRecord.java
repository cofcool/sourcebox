package net.cofcool.sourcebox.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.LinkCategory;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public class TimerRecord extends BaseRecord {

    @Override
    public ToolName name() {
        return ToolName.timer;
    }

    @Override
    protected boolean callOthers(Args args) throws Exception {
        return super.callOthers(args);
    }

    @Override
    protected void callAdd(Arg a) {
        saveRecord(null, a.val(), null, LinkCategory.bookmark.name());
    }

    @Override
    public Args config() {
        return super.config()
            .runnerTypes(EnumSet.allOf(RunnerType.class));
    }

    @Override
    protected Type currentType() {
        return Type.timer;
    }
}
