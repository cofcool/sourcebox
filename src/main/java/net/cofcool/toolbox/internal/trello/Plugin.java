package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Plugin(

    @JsonProperty("privacyUrl")
    String privacyUrl,

    @JsonProperty("capabilities")
    List<Object> capabilities,

    @JsonProperty("author")
    String author,

    @JsonProperty("idOrganizationOwner")
    String idOrganizationOwner,

    @JsonProperty("icon")
    Icon icon,

    @JsonProperty("capabilitiesOptions")
    List<Object> capabilitiesOptions,

    @JsonProperty("heroImageUrl")
    HeroImageUrl heroImageUrl,

    @JsonProperty("url")
    String url,

    @JsonProperty("usageBrackets")
    UsageBrackets usageBrackets,

    @JsonProperty("tags")
    List<String> tags,

    @JsonProperty("supportEmail")
    String supportEmail,

    @JsonProperty("public")
    boolean jsonMemberPublic,

    @JsonProperty("claimedDomains")
    List<Object> claimedDomains,

    @JsonProperty("name")
    String name,

    @JsonProperty("moderatedState")
    Object moderatedState,

    @JsonProperty("id")
    String id,

    @JsonProperty("categories")
    List<String> categories,

    @JsonProperty("iframeConnectorUrl")
    String iframeConnectorUrl,

    @JsonProperty("isCompliantWithPrivacyStandards")
    Object isCompliantWithPrivacyStandards,

    @JsonProperty("listing")
    Listing listing
) {
}