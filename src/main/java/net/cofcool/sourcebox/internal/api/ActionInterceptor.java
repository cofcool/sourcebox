package net.cofcool.sourcebox.internal.api;

import java.util.function.Function;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;

public interface ActionInterceptor extends Function<ActionRecord, ActionRecord> {
}
