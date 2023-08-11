package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppCreator(

    @JsonProperty("id")
    String id,

    @JsonProperty("name")
    String name,

    @JsonProperty("icon")
    Icon icon
) {
}