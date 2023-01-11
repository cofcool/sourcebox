package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record NameData(

    @SerializedName("emoji")
    Emoji emoji
) {
}