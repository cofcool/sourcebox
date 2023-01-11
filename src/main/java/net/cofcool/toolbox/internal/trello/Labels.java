package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Labels(

    @SerializedName("perBoard")
    PerBoard perBoard
) {
}