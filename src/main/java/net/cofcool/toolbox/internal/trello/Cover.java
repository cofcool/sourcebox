package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Cover(

    @SerializedName("idUploadedBackground")
    Object idUploadedBackground,

    @SerializedName("brightness")
    String brightness,

    @SerializedName("color")
    Object color,

    @SerializedName("size")
    String size,

    @SerializedName("idAttachment")
    Object idAttachment,

    @SerializedName("idPlugin")
    Object idPlugin
) {
}