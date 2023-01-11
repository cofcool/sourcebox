package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record Limits(

    @SerializedName("checklists")
    Checklists checklists,

    @SerializedName("attachments")
    Attachments attachments,

    @SerializedName("cards")
    Cards cards,

    @SerializedName("customFieldOptions")
    CustomFieldOptions customFieldOptions,

    @SerializedName("customFields")
    CustomFields customFields,

    @SerializedName("lists")
    Lists lists,

    @SerializedName("boards")
    Boards boards,

    @SerializedName("stickers")
    Stickers stickers,

    @SerializedName("reactions")
    Reactions reactions,

    @SerializedName("checkItems")
    CheckItems checkItems,

    @SerializedName("labels")
    Labels labels
) {
}