package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Boards(

    @JsonProperty("totalMembersPerBoard")
    TotalMembersPerBoard totalMembersPerBoard,

    @JsonProperty("totalAccessRequestsPerBoard")
    TotalAccessRequestsPerBoard totalAccessRequestsPerBoard
) {
}