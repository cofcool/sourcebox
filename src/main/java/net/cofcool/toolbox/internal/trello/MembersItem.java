package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record MembersItem(

    @SerializedName("idEnterprisesDeactivated")
    List<Object> idEnterprisesDeactivated,

    @SerializedName("avatarUrl")
    String avatarUrl,

    @SerializedName("initials")
    String initials,

    @SerializedName("nonPublicAvailable")
    boolean nonPublicAvailable,

    @SerializedName("idMemberReferrer")
    Object idMemberReferrer,

    @SerializedName("bio")
    String bio,

    @SerializedName("fullName")
    String fullName,

    @SerializedName("nonPublic")
    NonPublic nonPublic,

    @SerializedName("confirmed")
    boolean confirmed,

    @SerializedName("idPremOrgsAdmin")
    List<Object> idPremOrgsAdmin,

    @SerializedName("url")
    String url,

    @SerializedName("products")
    List<Object> products,

    @SerializedName("activityBlocked")
    boolean activityBlocked,

    @SerializedName("avatarHash")
    String avatarHash,

    @SerializedName("aaId")
    String aaId,

    @SerializedName("idEnterprise")
    Object idEnterprise,

    @SerializedName("id")
    String id,

    @SerializedName("memberType")
    String memberType,

    @SerializedName("bioData")
    Object bioData,

    @SerializedName("username")
    String username,

    @SerializedName("status")
    String status
) {
}