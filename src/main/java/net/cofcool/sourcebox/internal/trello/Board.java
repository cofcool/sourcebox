package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Board(

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id,

    @JsonProperty("shortLink")
    String shortLink,
    String desc
) {
}