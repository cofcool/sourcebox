package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LabelsItem(

    @JsonProperty("idBoard")
    String idBoard,

    @JsonProperty("color")
    String color,

    @JsonProperty("name")
    String name,

    @JsonProperty("id")
    String id
) {
}