package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record CustomFields(

    @SerializedName("perBoard")
    PerBoard perBoard
) {
}