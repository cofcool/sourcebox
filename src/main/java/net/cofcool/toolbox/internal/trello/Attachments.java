package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Attachments(

    @SerializedName("perCard")
    PerCard perCard,

    @SerializedName("perBoard")
    PerBoard perBoard
) {
}