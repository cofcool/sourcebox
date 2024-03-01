package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Labels(

    @JsonProperty("perBoard")
    PerBoard perBoard
) {
}