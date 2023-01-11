package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record HeroImageUrl(

    @SerializedName("@2x")
    String jsonMember2x,

    @SerializedName("@1x")
    String jsonMember1x,

    @SerializedName("_id")
    String id
) {
}