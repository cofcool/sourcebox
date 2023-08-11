package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Data(

    @JsonProperty("list")
    IdList list,

    @JsonProperty("card")
    Card card,

    @JsonProperty("board")
    Board board,

    @JsonProperty("text")
    String text,

    @JsonProperty("textData")
    TextData textData,

    @JsonProperty("dateLastEdited")
    String dateLastEdited,

    @JsonProperty("checklist")
    Checklist checklist,

    @JsonProperty("checkItem")
    CheckItem checkItem,

    @JsonProperty("old")
    Old old,

    @JsonProperty("listAfter")
    ListAfter listAfter,

    @JsonProperty("listBefore")
    ListBefore listBefore,

    @JsonProperty("reason")
    String reason,

    @JsonProperty("organization")
    Organization organization,

    @JsonProperty("plugin")
    Plugin plugin,

    @JsonProperty("cardSource")
    CardSource cardSource,

    @JsonProperty("idMember")
    String idMember,

    @JsonProperty("member")
    Member member,

    @JsonProperty("deactivated")
    boolean deactivated,

    @JsonProperty("boardSource")
    BoardSource boardSource
) {
}