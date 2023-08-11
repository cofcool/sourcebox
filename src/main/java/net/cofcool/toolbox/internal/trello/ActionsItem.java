package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActionsItem(

    @JsonProperty("date")
    String date,

    @JsonProperty("data")
    Data data,

    @JsonProperty("appCreator")
    AppCreator appCreator,

    @JsonProperty("memberCreator")
    MemberCreator memberCreator,

    @JsonProperty("id")
    String id,

    @JsonProperty("type")
    String type,

    @JsonProperty("idMemberCreator")
    String idMemberCreator,

    @JsonProperty("limits")
    Object limits,

    @JsonProperty("member")
    Member member
) {
}