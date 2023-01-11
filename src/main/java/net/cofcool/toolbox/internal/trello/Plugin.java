package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Plugin(

    @SerializedName("privacyUrl")
    String privacyUrl,

    @SerializedName("capabilities")
    List<Object> capabilities,

    @SerializedName("author")
    String author,

    @SerializedName("idOrganizationOwner")
    String idOrganizationOwner,

    @SerializedName("icon")
    Icon icon,

    @SerializedName("capabilitiesOptions")
    List<Object> capabilitiesOptions,

    @SerializedName("heroImageUrl")
    HeroImageUrl heroImageUrl,

    @SerializedName("url")
    String url,

    @SerializedName("usageBrackets")
    UsageBrackets usageBrackets,

    @SerializedName("tags")
    List<String> tags,

    @SerializedName("supportEmail")
    String supportEmail,

    @SerializedName("public")
    boolean jsonMemberPublic,

    @SerializedName("claimedDomains")
    List<Object> claimedDomains,

    @SerializedName("name")
    String name,

    @SerializedName("moderatedState")
    Object moderatedState,

    @SerializedName("id")
    String id,

    @SerializedName("categories")
    List<String> categories,

    @SerializedName("iframeConnectorUrl")
    String iframeConnectorUrl,

    @SerializedName("isCompliantWithPrivacyStandards")
    Object isCompliantWithPrivacyStandards,

    @SerializedName("listing")
    Listing listing
) {
}