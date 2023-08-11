package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NameData(

    @JsonProperty("emoji")
    Emoji emoji
) {
}