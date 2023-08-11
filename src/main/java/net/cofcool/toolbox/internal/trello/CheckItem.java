package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckItem(

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id,

    @JsonProperty("state")
    String state,

    @JsonProperty("textData")
    TextData textData
) {
}