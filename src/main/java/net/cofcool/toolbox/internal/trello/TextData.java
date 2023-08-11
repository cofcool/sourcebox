package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TextData(

    @JsonProperty("emoji")
    Emoji emoji
) {
}