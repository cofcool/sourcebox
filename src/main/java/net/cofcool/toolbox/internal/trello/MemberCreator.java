package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record MemberCreator(

    @SerializedName("activityBlocked")
    boolean activityBlocked,

    @SerializedName("avatarHash")
    String avatarHash,

    @SerializedName("avatarUrl")
    String avatarUrl,

    @SerializedName("initials")
    String initials,

    @SerializedName("nonPublicAvailable")
    boolean nonPublicAvailable,

    @SerializedName("idMemberReferrer")
    Object idMemberReferrer,

    @SerializedName("fullName")
    String fullName,

    @SerializedName("id")
    String id,

    @SerializedName("nonPublic")
    NonPublic nonPublic,

    @SerializedName("username")
    String username
) {
}