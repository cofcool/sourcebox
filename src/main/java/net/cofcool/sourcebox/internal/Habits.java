package net.cofcool.sourcebox.internal;

import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;

@CustomLog
public class Habits extends BaseRecord {

    @Override
    public ToolName name() {
        return ToolName.habits;
    }


    @Override
    protected Type currentType() {
        return Type.habit;
    }

}
