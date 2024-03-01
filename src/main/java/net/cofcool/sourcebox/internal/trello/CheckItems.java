package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckItems(

    @JsonProperty("perChecklist")
    PerChecklist perChecklist
) {
}