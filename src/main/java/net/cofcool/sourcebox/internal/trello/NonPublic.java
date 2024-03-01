package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NonPublic(

    @JsonProperty("avatarHash")
    Object avatarHash,

    @JsonProperty("initials")
    String initials,

    @JsonProperty("fullName")
    String fullName
) {
}