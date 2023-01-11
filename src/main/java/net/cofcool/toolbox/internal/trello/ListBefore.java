package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record ListBefore(

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id
) {
}