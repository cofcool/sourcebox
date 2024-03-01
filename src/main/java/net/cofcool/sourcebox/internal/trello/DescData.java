package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DescData(

    @JsonProperty("emoji")
    Emoji emoji
) {
}