package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record CheckItem(

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id,

    @SerializedName("state")
    String state,

    @SerializedName("textData")
    TextData textData
) {
}