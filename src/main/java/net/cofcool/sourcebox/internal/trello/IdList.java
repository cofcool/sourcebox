package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IdList(

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id,

    @JsonProperty("pos")
    Object pos
) {
}