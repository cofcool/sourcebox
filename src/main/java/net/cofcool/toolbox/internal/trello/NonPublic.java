package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record NonPublic(

    @SerializedName("avatarHash")
    Object avatarHash,

    @SerializedName("initials")
    String initials,

    @SerializedName("fullName")
    String fullName
) {
}