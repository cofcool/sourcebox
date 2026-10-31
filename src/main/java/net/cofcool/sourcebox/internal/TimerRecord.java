package net.cofcool.sourcebox.internal;

import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;

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
    protected String callAdd(Arg a) {
        return saveRecord(null, a.val(), null, null).id();
    }

    @Override
    protected Type currentType() {
        return Type.timer;
    }
}
