package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record TextData(

    @SerializedName("emoji")
    Emoji emoji
) {
}