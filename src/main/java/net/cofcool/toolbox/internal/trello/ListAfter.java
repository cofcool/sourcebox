package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ListAfter(

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id
) {
}