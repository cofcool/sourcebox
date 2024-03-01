package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Card(

    @JsonProperty("idShort")
    int idShort,

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id,

    @JsonProperty("shortLink")
    String shortLink,

    @JsonProperty("idList")
    String idList,

    @JsonProperty("pos")
    double pos,

    @JsonProperty("dueReminder")
    int dueReminder,

    @JsonProperty("idLabels")
    List<String> idLabels,

    @JsonProperty("desc")
    String desc,
    boolean closed
) {
}