package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ListsItem(

    @JsonProperty("subscribed")
    Boolean subscribed,

    @JsonProperty("idBoard")
    String idBoard,

    @JsonProperty("pos")
    Object pos,

    @JsonProperty("name")
    String name,

    @JsonProperty("closed")
    boolean closed,

    @JsonProperty("id")
    String id,

    @JsonProperty("limits")
    Limits limits,

    @JsonProperty("softLimit")
    Object softLimit,

    @JsonProperty("creationMethod")
    Object creationMethod
) {
}