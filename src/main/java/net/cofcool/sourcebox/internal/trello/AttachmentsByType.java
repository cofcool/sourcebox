package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AttachmentsByType(

    @JsonProperty("trello")
    Trello trello
) {
}