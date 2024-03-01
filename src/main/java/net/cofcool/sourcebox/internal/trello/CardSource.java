package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardSource(

    @JsonProperty("idShort")
    int idShort,

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id,

    @JsonProperty("shortLink")
    String shortLink
) {
}