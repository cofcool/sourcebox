package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record OpenPerBoard(

    @SerializedName("warnAt")
    int warnAt,

    @SerializedName("disableAt")
    int disableAt,

    @SerializedName("status")
    String status
) {
}