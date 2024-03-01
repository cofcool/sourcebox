package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Badges(

    @JsonProperty("comments")
    int comments,

    @JsonProperty("attachments")
    int attachments,

    @JsonProperty("attachmentsByType")
    AttachmentsByType attachmentsByType,

    @JsonProperty("dueComplete")
    boolean dueComplete,

    @JsonProperty("start")
    Object start,

    @JsonProperty("description")
    boolean description,

    @JsonProperty("checkItemsEarliestDue")
    Object checkItemsEarliestDue,

    @JsonProperty("subscribed")
    Boolean subscribed,

    @JsonProperty("due")
    Object due,

    @JsonProperty("viewingMemberVoted")
    boolean viewingMemberVoted,

    @JsonProperty("location")
    boolean location,

    @JsonProperty("votes")
    int votes,

    @JsonProperty("fogbugz")
    String fogbugz,

    @JsonProperty("checkItems")
    int checkItems,

    @JsonProperty("checkItemsChecked")
    int checkItemsChecked
) {
}