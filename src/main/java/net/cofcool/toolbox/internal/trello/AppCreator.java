package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record AppCreator(

    @SerializedName("id")
    String id,

    @SerializedName("name")
    String name,

    @SerializedName("icon")
    Icon icon
) {
}