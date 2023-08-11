package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Cover(

    @JsonProperty("idUploadedBackground")
    Object idUploadedBackground,

    @JsonProperty("brightness")
    String brightness,

    @JsonProperty("color")
    Object color,

    @JsonProperty("size")
    String size,

    @JsonProperty("idAttachment")
    Object idAttachment,

    @JsonProperty("idPlugin")
    Object idPlugin
) {
}