package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Listing(

    @SerializedName("overview")
    String overview,

    @SerializedName("name")
    String name,

    @SerializedName("description")
    String description,

    @SerializedName("locale")
    String locale
) {
}