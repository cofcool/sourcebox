package net.cofcool.toolbox.internal.commandhelper;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record Command(
    String id,
    String cmd,
    String alias,
    List<String> tags,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime cTime
) {

    public static final String NO_ALIAS = "NA_";

    public Command(String cmd, String alias, List<String> tags) {
        this(alias != null ? alias : NO_ALIAS + Math.abs(Objects.hash(cmd)), cmd, alias, tags, LocalDateTime.now());
    }

    public boolean tagContains(String tag) {
        return tags.contains(tag);
    }

    public boolean hasAlias() {
        return !id.startsWith(NO_ALIAS);
    }

    public String toAlias() {
        return "alias "
            + alias.substring(1)
            + "='"
            + cmd
            + "'";
    }

    @Override
    public String toString() {
        return String.join(" ", id, cmd, Objects.toString(tags.toString(), "[]"));
    }
}
