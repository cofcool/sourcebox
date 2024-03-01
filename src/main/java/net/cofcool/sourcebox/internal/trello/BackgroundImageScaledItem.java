package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BackgroundImageScaledItem(

    @JsonProperty("width")
    int width,

    @JsonProperty("url")
    String url,

    @JsonProperty("height")
    int height
) {
}