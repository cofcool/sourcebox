package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record BackgroundImageScaledItem(

    @SerializedName("width")
    int width,

    @SerializedName("url")
    String url,

    @SerializedName("height")
    int height
) {
}