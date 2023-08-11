package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomFields(

    @JsonProperty("perBoard")
    PerBoard perBoard
) {
}