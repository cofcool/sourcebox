package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Stickers(

    @SerializedName("perCard")
    PerCard perCard
) {
}