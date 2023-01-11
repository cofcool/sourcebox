package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Checklists(

    @SerializedName("perCard")
    PerCard perCard,

    @SerializedName("perBoard")
    PerBoard perBoard
) {
}