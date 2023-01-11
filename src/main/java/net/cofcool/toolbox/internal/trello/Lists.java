package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Lists(

    @SerializedName("totalPerBoard")
    TotalPerBoard totalPerBoard,

    @SerializedName("openPerBoard")
    OpenPerBoard openPerBoard
) {
}