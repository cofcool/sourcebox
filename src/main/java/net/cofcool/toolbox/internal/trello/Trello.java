package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Trello(

    @JsonProperty("descData")
    Object descData,

    @JsonProperty("checklists")
    List<ChecklistsItem> checklists,

    @JsonProperty("idTags")
    List<Object> idTags,

    @JsonProperty("pinned")
    Boolean pinned,

    @JsonProperty("labelNames")
    LabelNames labelNames,

    @JsonProperty("cards")
    List<CardsItem> cards,

    @JsonProperty("shortUrl")
    String shortUrl,

    @JsonProperty("customFields")
    List<Object> customFields,

    @JsonProperty("dateLastActivity")
    String dateLastActivity,

    @JsonProperty("datePluginDisable")
    Object datePluginDisable,

    @JsonProperty("dateClosed")
    Object dateClosed,

    @JsonProperty("shortLink")
    String shortLink,

    @JsonProperty("idBoardSource")
    Object idBoardSource,

    @JsonProperty("memberships")
    List<MembershipsItem> memberships,

    @JsonProperty("creationMethod")
    Object creationMethod,

    @JsonProperty("subscribed")
    Boolean subscribed,

    @JsonProperty("starred")
    Boolean starred,

    @JsonProperty("members")
    List<MembersItem> members,

    @JsonProperty("idOrganization")
    String idOrganization,

    @JsonProperty("dateLastView")
    String dateLastView,

    @JsonProperty("pluginData")
    List<Object> pluginData,

    @JsonProperty("id")
    String id,

    @JsonProperty("limits")
    Limits limits,

    @JsonProperty("powerUps")
    List<Object> powerUps,

    @JsonProperty("templateGallery")
    Object templateGallery,

    @JsonProperty("premiumFeatures")
    List<String> premiumFeatures,

    @JsonProperty("url")
    String url,

    @JsonProperty("prefs")
    Prefs prefs,

    @JsonProperty("labels")
    List<LabelsItem> labels,

    @JsonProperty("enterpriseOwned")
    Boolean enterpriseOwned,

    @JsonProperty("ixUpdate")
    String ixUpdate,

    @JsonProperty("lists")
    List<ListsItem> lists,

    @JsonProperty("idEnterprise")
    Object idEnterprise,

    @JsonProperty("name")
    String name,

    @JsonProperty("closed")
    Boolean closed,

    @JsonProperty("nodeId")
    String nodeId,

    @JsonProperty("idMemberCreator")
    String idMemberCreator,

    @JsonProperty("actions")
    List<ActionsItem> actions,

    @JsonProperty("desc")
    String desc,

    @JsonProperty("board")
    int board,

    @JsonProperty("card")
    int card
) {
}