package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Badges(

    @SerializedName("comments")
    int comments,

    @SerializedName("attachments")
    int attachments,

    @SerializedName("attachmentsByType")
    AttachmentsByType attachmentsByType,

    @SerializedName("dueComplete")
    boolean dueComplete,

    @SerializedName("start")
    Object start,

    @SerializedName("description")
    boolean description,

    @SerializedName("checkItemsEarliestDue")
    Object checkItemsEarliestDue,

    @SerializedName("subscribed")
    boolean subscribed,

    @SerializedName("due")
    Object due,

    @SerializedName("viewingMemberVoted")
    boolean viewingMemberVoted,

    @SerializedName("location")
    boolean location,

    @SerializedName("votes")
    int votes,

    @SerializedName("fogbugz")
    String fogbugz,

    @SerializedName("checkItems")
    int checkItems,

    @SerializedName("checkItemsChecked")
    int checkItemsChecked
) {
}