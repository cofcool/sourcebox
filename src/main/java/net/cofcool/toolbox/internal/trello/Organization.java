package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Organization(

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id
) {
}