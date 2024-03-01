package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NameData(

    @JsonProperty("emoji")
    Emoji emoji
) {
}