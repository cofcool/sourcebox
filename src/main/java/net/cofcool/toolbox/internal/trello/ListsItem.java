package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record ListsItem(

    @SerializedName("subscribed")
    boolean subscribed,

    @SerializedName("idBoard")
    String idBoard,

    @SerializedName("pos")
    Object pos,

    @SerializedName("name")
    String name,

    @SerializedName("closed")
    boolean closed,

    @SerializedName("id")
    String id,

    @SerializedName("limits")
    Limits limits,

    @SerializedName("softLimit")
    Object softLimit,

    @SerializedName("creationMethod")
    Object creationMethod
) {
}