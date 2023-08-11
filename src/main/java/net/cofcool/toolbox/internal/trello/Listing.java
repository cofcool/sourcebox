package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Listing(

    @JsonProperty("overview")
    String overview,

    @JsonProperty("name")
    String name,

    @JsonProperty("description")
    String description,

    @JsonProperty("locale")
    String locale
) {
}