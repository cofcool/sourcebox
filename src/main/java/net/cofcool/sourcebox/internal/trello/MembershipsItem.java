package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MembershipsItem(

    @JsonProperty("unconfirmed")
    boolean unconfirmed,

    @JsonProperty("idMember")
    String idMember,

    @JsonProperty("memberType")
    String memberType,

    @JsonProperty("id")
    String id,

    @JsonProperty("deactivated")
    boolean deactivated
) {
}