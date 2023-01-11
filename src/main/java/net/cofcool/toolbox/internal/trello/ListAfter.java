package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record ListAfter(

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id
) {
}