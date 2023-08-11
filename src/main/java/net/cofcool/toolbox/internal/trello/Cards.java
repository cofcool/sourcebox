package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Cards(

    @JsonProperty("openPerList")
    OpenPerList openPerList,

    @JsonProperty("totalPerList")
    TotalPerList totalPerList,

    @JsonProperty("totalPerBoard")
    TotalPerBoard totalPerBoard,

    @JsonProperty("openPerBoard")
    OpenPerBoard openPerBoard
) {
}