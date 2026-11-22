package net.cofcool.sourcebox.internal.api.entity;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum RefType {
    none, unknown, note, action, schedule;

    public String refStr(String val) {
        return name() + ":" + val;
    }

    public static String appendRefStr(String... refs) {
        return String.join(";", refs);
    }

    public static Map<RefType, String> parse(String val) {
        if (StringUtils.isBlank(val)) {
            return Map.of(none, "none");
        }

        var ret = new HashMap<RefType, String>();
        for (String p : val.split(";")) {
            var s = p.split(":");
            if (s.length != 2) {
                ret.put(unknown, p);
            } else {
                ret.put(RefType.valueOf(s[0]), s[1]);
            }
        }

        return ret;
    }
}
