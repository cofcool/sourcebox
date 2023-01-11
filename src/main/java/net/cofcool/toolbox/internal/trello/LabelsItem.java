package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record LabelsItem(

    @SerializedName("idBoard")
    String idBoard,

    @SerializedName("color")
    String color,

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id
) {
}