package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Checklist(

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id
) {
}