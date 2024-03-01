package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Member(

    @JsonProperty("activityBlocked")
    boolean activityBlocked,

    @JsonProperty("avatarHash")
    String avatarHash,

    @JsonProperty("avatarUrl")
    String avatarUrl,

    @JsonProperty("initials")
    String initials,

    @JsonProperty("nonPublicAvailable")
    boolean nonPublicAvailable,

    @JsonProperty("idMemberReferrer")
    Object idMemberReferrer,

    @JsonProperty("fullName")
    String fullName,

    @JsonProperty("id")
    String id,

    @JsonProperty("nonPublic")
    NonPublic nonPublic,

    @JsonProperty("username")
    String username,

    @JsonProperty("name")
    String name
) {
}