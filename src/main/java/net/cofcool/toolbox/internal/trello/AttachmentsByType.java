package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record AttachmentsByType(

    @SerializedName("trello")
    Trello trello
) {
}