package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Icon(

    @SerializedName("url")
    String url
) {
}