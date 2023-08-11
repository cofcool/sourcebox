package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ChecklistsItem(

    @JsonProperty("idBoard")
    String idBoard,

    @JsonProperty("pos")
    double pos,

    @JsonProperty("idCard")
    String idCard,

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id,

    @JsonProperty("checkItems")
    List<CheckItemsItem> checkItems,

    @JsonProperty("limits")
    Limits limits,

    @JsonProperty("creationMethod")
    Object creationMethod
) {
}