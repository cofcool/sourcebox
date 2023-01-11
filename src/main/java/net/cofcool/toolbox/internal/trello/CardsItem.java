package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record CardsItem(

    @SerializedName("descData")
    Object descData,

    @SerializedName("attachments")
    List<Object> attachments,

    @SerializedName("idLabels")
    List<String> idLabels,

    @SerializedName("shortUrl")
    String shortUrl,

    @SerializedName("dueComplete")
    boolean dueComplete,

    @SerializedName("dateLastActivity")
    String dateLastActivity,

    @SerializedName("idList")
    String idList,

    @SerializedName("idMembersVoted")
    List<Object> idMembersVoted,

    @SerializedName("shortLink")
    String shortLink,

    @SerializedName("creationMethod")
    Object creationMethod,

    @SerializedName("cover")
    Cover cover,

    @SerializedName("dueReminder")
    Object dueReminder,

    @SerializedName("subscribed")
    boolean subscribed,

    @SerializedName("pos")
    double pos,

    @SerializedName("staticMapUrl")
    Object staticMapUrl,

    @SerializedName("idChecklists")
    List<String> idChecklists,

    @SerializedName("pluginData")
    List<Object> pluginData,

    @SerializedName("id")
    String id,

    @SerializedName("email")
    String email,

    @SerializedName("limits")
    Limits limits,

    @SerializedName("customFieldItems")
    List<Object> customFieldItems,

    @SerializedName("address")
    Object address,

    @SerializedName("idBoard")
    String idBoard,

    @SerializedName("locationName")
    Object locationName,

    @SerializedName("cardRole")
    Object cardRole,

    @SerializedName("coordinates")
    Object coordinates,

    @SerializedName("start")
    Object start,

    @SerializedName("checkItemStates")
    Object checkItemStates,

    @SerializedName("url")
    String url,

    @SerializedName("labels")
    List<LabelsItem> labels,

    @SerializedName("badges")
    Badges badges,

    @SerializedName("idMembers")
    List<Object> idMembers,

    @SerializedName("idShort")
    int idShort,

    @SerializedName("due")
    Object due,

    @SerializedName("idAttachmentCover")
    Object idAttachmentCover,

    @SerializedName("isTemplate")
    boolean isTemplate,

    @SerializedName("name")
    String name,

    @SerializedName("closed")
    boolean closed,

    @SerializedName("manualCoverAttachment")
    boolean manualCoverAttachment,

    @SerializedName("desc")
    String desc
) {
}