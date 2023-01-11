package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record CheckItems(

    @SerializedName("perChecklist")
    PerChecklist perChecklist
) {
}