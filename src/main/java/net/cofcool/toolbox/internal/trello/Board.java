package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Board(

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id,

    @SerializedName("shortLink")
    String shortLink
) {
}