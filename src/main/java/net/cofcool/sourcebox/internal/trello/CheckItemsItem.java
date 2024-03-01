package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckItemsItem(

    @JsonProperty("dueReminder")
    Object dueReminder,

    @JsonProperty("pos")
    double pos,

    @JsonProperty("due")
    Object due,

    @JsonProperty("idMember")
    Object idMember,

    @JsonProperty("idChecklist")
    String idChecklist,

    @JsonProperty("name")
    String name,

    @JsonProperty("nameData")
    NameData nameData,

    @JsonProperty("id")
    String id,

    @JsonProperty("state")
    String state
) {
}