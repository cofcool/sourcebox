package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record Trello(

    @SerializedName("descData")
    Object descData,

    @SerializedName("checklists")
    List<ChecklistsItem> checklists,

    @SerializedName("idTags")
    List<Object> idTags,

    @SerializedName("pinned")
    Boolean pinned,

    @SerializedName("labelNames")
    LabelNames labelNames,

    @SerializedName("cards")
    List<CardsItem> cards,

    @SerializedName("shortUrl")
    String shortUrl,

    @SerializedName("customFields")
    List<Object> customFields,

    @SerializedName("dateLastActivity")
    String dateLastActivity,

    @SerializedName("datePluginDisable")
    Object datePluginDisable,

    @SerializedName("dateClosed")
    Object dateClosed,

    @SerializedName("shortLink")
    String shortLink,

    @SerializedName("idBoardSource")
    Object idBoardSource,

    @SerializedName("memberships")
    List<MembershipsItem> memberships,

    @SerializedName("creationMethod")
    Object creationMethod,

    @SerializedName("subscribed")
    Boolean subscribed,

    @SerializedName("starred")
    Boolean starred,

    @SerializedName("members")
    List<MembersItem> members,

    @SerializedName("idOrganization")
    String idOrganization,

    @SerializedName("dateLastView")
    String dateLastView,

    @SerializedName("pluginData")
    List<Object> pluginData,

    @SerializedName("id")
    String id,

    @SerializedName("limits")
    Limits limits,

    @SerializedName("powerUps")
    List<Object> powerUps,

    @SerializedName("templateGallery")
    Object templateGallery,

    @SerializedName("premiumFeatures")
    List<String> premiumFeatures,

    @SerializedName("url")
    String url,

    @SerializedName("prefs")
    Prefs prefs,

    @SerializedName("labels")
    List<LabelsItem> labels,

    @SerializedName("enterpriseOwned")
    Boolean enterpriseOwned,

    @SerializedName("ixUpdate")
    String ixUpdate,

    @SerializedName("lists")
    List<ListsItem> lists,

    @SerializedName("idEnterprise")
    Object idEnterprise,

    @SerializedName("name")
    String name,

    @SerializedName("closed")
    Boolean closed,

    @SerializedName("nodeId")
    String nodeId,

    @SerializedName("idMemberCreator")
    String idMemberCreator,

    @SerializedName("actions")
    List<ActionsItem> actions,

    @SerializedName("desc")
    String desc,

    @SerializedName("board")
    int board,

    @SerializedName("card")
    int card
) {
}