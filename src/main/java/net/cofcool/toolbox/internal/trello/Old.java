package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Old(

    @JsonProperty("name")
    String name,

    @JsonProperty("idList")
    String idList,

    @JsonProperty("pos")
    double pos,

    @JsonProperty("dueReminder")
    Object dueReminder,

    @JsonProperty("idLabels")
    List<String> idLabels,

    @JsonProperty("desc")
    String desc,
    boolean closed
) {
}