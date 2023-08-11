package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsageBrackets(

    @JsonProperty("boards")
    int boards
) {
}