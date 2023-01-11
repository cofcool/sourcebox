package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record DescData(

    @SerializedName("emoji")
    Emoji emoji
) {
}