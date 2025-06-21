package net.cofcool.sourcebox.internal.simplenote;

import java.util.function.Function;
import net.cofcool.sourcebox.internal.simplenote.entity.ActionRecord;

public interface ActionInterceptor extends Function<ActionRecord, ActionRecord> {
}
