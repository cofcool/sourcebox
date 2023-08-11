package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomFieldOptions(

    @JsonProperty("perField")
    PerField perField
) {
}