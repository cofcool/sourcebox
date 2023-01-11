package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Data(

    @SerializedName("list")
    IdList list,

    @SerializedName("card")
    Card card,

    @SerializedName("board")
    Board board,

    @SerializedName("text")
    String text,

    @SerializedName("textData")
    TextData textData,

    @SerializedName("dateLastEdited")
    String dateLastEdited,

    @SerializedName("checklist")
    Checklist checklist,

    @SerializedName("checkItem")
    CheckItem checkItem,

    @SerializedName("old")
    Old old,

    @SerializedName("listAfter")
    ListAfter listAfter,

    @SerializedName("listBefore")
    ListBefore listBefore,

    @SerializedName("reason")
    String reason,

    @SerializedName("organization")
    Organization organization,

    @SerializedName("plugin")
    Plugin plugin,

    @SerializedName("cardSource")
    CardSource cardSource,

    @SerializedName("idMember")
    String idMember,

    @SerializedName("member")
    Member member,

    @SerializedName("deactivated")
    boolean deactivated,

    @SerializedName("boardSource")
    BoardSource boardSource
) {
}