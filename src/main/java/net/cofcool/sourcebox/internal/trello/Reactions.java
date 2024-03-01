package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Reactions(

    @JsonProperty("perAction")
    PerAction perAction,

    @JsonProperty("uniquePerAction")
    UniquePerAction uniquePerAction
) {
}