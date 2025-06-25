package net.cofcool.sourcebox.internal.api.entity;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public enum RefType {
    none, unknown, note, action;

    public String refStr(String val) {
        return name() + ":" + val;
    }

    public static Pair<RefType, String> parse(String val) {
        if (StringUtils.isBlank(val)) {
            return Pair.of(none, null);
        }

        var s = val.split(":");
        if (s.length != 2) {
            return Pair.of(unknown, val);
        }

        return Pair.of(RefType.valueOf(s[0]), s[1]);
    }
}
