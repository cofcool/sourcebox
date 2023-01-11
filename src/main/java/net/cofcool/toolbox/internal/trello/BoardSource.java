package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record BoardSource(

    @SerializedName("id")
    String id
) {
}