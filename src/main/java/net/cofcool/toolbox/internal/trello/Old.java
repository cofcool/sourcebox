package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Old(

    @SerializedName("name")
    String name,

    @SerializedName("idList")
    String idList,

    @SerializedName("pos")
    double pos,

    @SerializedName("dueReminder")
    Object dueReminder,

    @SerializedName("idLabels")
    List<String> idLabels,

    @SerializedName("desc")
    String desc
) {
}