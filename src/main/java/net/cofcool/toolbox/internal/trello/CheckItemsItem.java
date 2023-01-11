package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record CheckItemsItem(

    @SerializedName("dueReminder")
    Object dueReminder,

    @SerializedName("pos")
    double pos,

    @SerializedName("due")
    Object due,

    @SerializedName("idMember")
    Object idMember,

    @SerializedName("idChecklist")
    String idChecklist,

    @SerializedName("name")
    String name,

    @SerializedName("nameData")
    NameData nameData,

    @SerializedName("id")
    String id,

    @SerializedName("state")
    String state
) {
}