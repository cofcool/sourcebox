package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record ActionsItem(

    @SerializedName("date")
    String date,

    @SerializedName("data")
    Data data,

    @SerializedName("appCreator")
    AppCreator appCreator,

    @SerializedName("memberCreator")
    MemberCreator memberCreator,

    @SerializedName("id")
    String id,

    @SerializedName("type")
    String type,

    @SerializedName("idMemberCreator")
    String idMemberCreator,

    @SerializedName("limits")
    Object limits,

    @SerializedName("member")
    Member member
) {
}