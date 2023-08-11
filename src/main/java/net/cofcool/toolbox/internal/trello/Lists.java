package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Lists(

    @JsonProperty("totalPerBoard")
    TotalPerBoard totalPerBoard,

    @JsonProperty("openPerBoard")
    OpenPerBoard openPerBoard
) {
}