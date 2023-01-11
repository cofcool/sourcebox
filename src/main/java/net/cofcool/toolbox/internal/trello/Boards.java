package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Boards(

    @SerializedName("totalMembersPerBoard")
    TotalMembersPerBoard totalMembersPerBoard,

    @SerializedName("totalAccessRequestsPerBoard")
    TotalAccessRequestsPerBoard totalAccessRequestsPerBoard
) {
}