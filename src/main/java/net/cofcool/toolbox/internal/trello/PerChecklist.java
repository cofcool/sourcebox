package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PerChecklist(

    @JsonProperty("warnAt")
    int warnAt,

    @JsonProperty("disableAt")
    int disableAt,

    @JsonProperty("status")
    String status
) {
}