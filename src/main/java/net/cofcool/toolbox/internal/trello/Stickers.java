package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Stickers(

    @JsonProperty("perCard")
    PerCard perCard
) {
}