package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record IdList(

    @SerializedName("name")
    String name,

    @SerializedName("id")
    String id,

    @SerializedName("pos")
    Object pos
) {
}