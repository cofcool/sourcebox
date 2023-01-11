package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

public record SwitcherViewsItem(

    @SerializedName("viewType")
    String viewType,

    @SerializedName("_id")
    String id,

    @SerializedName("enabled")
    boolean enabled
) {
}