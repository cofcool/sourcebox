package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Icon(

    @JsonProperty("url")
    String url
) {
}