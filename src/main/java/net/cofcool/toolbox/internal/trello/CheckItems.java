package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckItems(

    @JsonProperty("perChecklist")
    PerChecklist perChecklist
) {
}