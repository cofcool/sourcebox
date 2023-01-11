package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Cards(

    @SerializedName("openPerList")
    OpenPerList openPerList,

    @SerializedName("totalPerList")
    TotalPerList totalPerList,

    @SerializedName("totalPerBoard")
    TotalPerBoard totalPerBoard,

    @SerializedName("openPerBoard")
    OpenPerBoard openPerBoard
) {
}