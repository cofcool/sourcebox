package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Limits(

    @JsonProperty("checklists")
    Checklists checklists,

    @JsonProperty("attachments")
    Attachments attachments,

    @JsonProperty("cards")
    Cards cards,

    @JsonProperty("customFieldOptions")
    CustomFieldOptions customFieldOptions,

    @JsonProperty("customFields")
    CustomFields customFields,

    @JsonProperty("lists")
    Lists lists,

    @JsonProperty("boards")
    Boards boards,

    @JsonProperty("stickers")
    Stickers stickers,

    @JsonProperty("reactions")
    Reactions reactions,

    @JsonProperty("checkItems")
    CheckItems checkItems,

    @JsonProperty("labels")
    Labels labels
) {
}