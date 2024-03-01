package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomFieldOptions(

    @JsonProperty("perField")
    PerField perField
) {
}