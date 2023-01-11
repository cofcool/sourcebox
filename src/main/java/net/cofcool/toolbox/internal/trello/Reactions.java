package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Reactions(

    @SerializedName("perAction")
    PerAction perAction,

    @SerializedName("uniquePerAction")
    UniquePerAction uniquePerAction
) {
}