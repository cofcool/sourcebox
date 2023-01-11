package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record MembershipsItem(

    @SerializedName("unconfirmed")
    boolean unconfirmed,

    @SerializedName("idMember")
    String idMember,

    @SerializedName("memberType")
    String memberType,

    @SerializedName("id")
    String id,

    @SerializedName("deactivated")
    boolean deactivated
) {
}