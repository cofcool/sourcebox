package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CardsItem(

    @JsonProperty("descData")
    Object descData,

    @JsonProperty("attachments")
    List<Object> attachments,

    @JsonProperty("idLabels")
    List<String> idLabels,

    @JsonProperty("shortUrl")
    String shortUrl,

    @JsonProperty("dueComplete")
    boolean dueComplete,

    @JsonProperty("dateLastActivity")
    String dateLastActivity,

    @JsonProperty("idList")
    String idList,

    @JsonProperty("idMembersVoted")
    List<Object> idMembersVoted,

    @JsonProperty("shortLink")
    String shortLink,

    @JsonProperty("creationMethod")
    Object creationMethod,

    @JsonProperty("cover")
    Cover cover,

    @JsonProperty("dueReminder")
    Object dueReminder,

    @JsonProperty("subscribed")
    Boolean subscribed,

    @JsonProperty("pos")
    Double pos,

    @JsonProperty("staticMapUrl")
    Object staticMapUrl,

    @JsonProperty("idChecklists")
    List<String> idChecklists,

    @JsonProperty("pluginData")
    List<Object> pluginData,

    @JsonProperty("id")
    String id,

    @JsonProperty("email")
    String email,

    @JsonProperty("limits")
    Limits limits,

    @JsonProperty("customFieldItems")
    List<Object> customFieldItems,

    @JsonProperty("address")
    Object address,

    @JsonProperty("idBoard")
    String idBoard,

    @JsonProperty("locationName")
    Object locationName,

    @JsonProperty("cardRole")
    Object cardRole,

    @JsonProperty("coordinates")
    Object coordinates,

    @JsonProperty("start")
    Object start,

    @JsonProperty("checkItemStates")
    Object checkItemStates,

    @JsonProperty("url")
    String url,

    @JsonProperty("labels")
    List<LabelsItem> labels,

    @JsonProperty("badges")
    Badges badges,

    @JsonProperty("idMembers")
    List<Object> idMembers,

    @JsonProperty("idShort")
    int idShort,

    @JsonProperty("due")
    Object due,

    @JsonProperty("idAttachmentCover")
    Object idAttachmentCover,

    @JsonProperty("isTemplate")
    boolean isTemplate,

    @JsonProperty("name")
    String name,

    @JsonProperty("closed")
    boolean closed,

    @JsonProperty("manualCoverAttachment")
    boolean manualCoverAttachment,

    @JsonProperty("desc")
    String desc
) {
}