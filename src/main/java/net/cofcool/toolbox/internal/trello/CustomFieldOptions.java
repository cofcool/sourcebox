package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record CustomFieldOptions(

    @SerializedName("perField")
    PerField perField
) {
}