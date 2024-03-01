package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MembersItem(

    @JsonProperty("idEnterprisesDeactivated")
    List<Object> idEnterprisesDeactivated,

    @JsonProperty("avatarUrl")
    String avatarUrl,

    @JsonProperty("initials")
    String initials,

    @JsonProperty("nonPublicAvailable")
    boolean nonPublicAvailable,

    @JsonProperty("idMemberReferrer")
    Object idMemberReferrer,

    @JsonProperty("bio")
    String bio,

    @JsonProperty("fullName")
    String fullName,

    @JsonProperty("nonPublic")
    NonPublic nonPublic,

    @JsonProperty("confirmed")
    boolean confirmed,

    @JsonProperty("idPremOrgsAdmin")
    List<Object> idPremOrgsAdmin,

    @JsonProperty("url")
    String url,

    @JsonProperty("products")
    List<Object> products,

    @JsonProperty("activityBlocked")
    boolean activityBlocked,

    @JsonProperty("avatarHash")
    String avatarHash,

    @JsonProperty("aaId")
    String aaId,

    @JsonProperty("idEnterprise")
    Object idEnterprise,

    @JsonProperty("id")
    String id,

    @JsonProperty("memberType")
    String memberType,

    @JsonProperty("bioData")
    Object bioData,

    @JsonProperty("username")
    String username,

    @JsonProperty("status")
    String status
) {
}