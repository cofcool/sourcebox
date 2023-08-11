package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BoardSource(

    @JsonProperty("id")
    String id
) {
}