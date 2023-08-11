package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenPerList(

    @JsonProperty("warnAt")
    int warnAt,

    @JsonProperty("disableAt")
    int disableAt,

    @JsonProperty("status")
    String status
) {
}