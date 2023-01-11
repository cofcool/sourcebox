package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record ChecklistsItem(

    @SerializedName("idBoard")
    String idBoard,

    @SerializedName("pos")
    double pos,

    @SerializedName("idCard")
    String idCard,

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id,

    @SerializedName("checkItems")
    List<CheckItemsItem> checkItems,

    @SerializedName("limits")
    Limits limits,

    @SerializedName("creationMethod")
    Object creationMethod
) {
}