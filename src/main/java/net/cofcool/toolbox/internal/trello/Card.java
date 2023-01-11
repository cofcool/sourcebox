package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Card(

    @SerializedName("idShort")
    int idShort,

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id,

    @SerializedName("shortLink")
    String shortLink,

    @SerializedName("idList")
    String idList,

    @SerializedName("pos")
    double pos,

    @SerializedName("dueReminder")
    int dueReminder,

    @SerializedName("idLabels")
    List<String> idLabels,

    @SerializedName("desc")
    String desc
) {
}