package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SwitcherViewsItem(

    @JsonProperty("viewType")
    String viewType,

    @JsonProperty("_id")
    String id,

    @JsonProperty("enabled")
    boolean enabled
) {
}