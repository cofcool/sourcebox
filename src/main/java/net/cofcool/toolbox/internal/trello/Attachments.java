package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Attachments(

    @JsonProperty("perCard")
    PerCard perCard,

    @JsonProperty("perBoard")
    PerBoard perBoard
) {
}