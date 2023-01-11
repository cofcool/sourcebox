package net.cofcool.toolbox.internal.trello;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Prefs(

    @SerializedName("canBeEnterprise")
    boolean canBeEnterprise,

    @SerializedName("hideVotes")
    boolean hideVotes,

    @SerializedName("backgroundImage")
    String backgroundImage,

    @SerializedName("voting")
    String voting,

    @SerializedName("hiddenPluginBoardButtons")
    List<Object> hiddenPluginBoardButtons,

    @SerializedName("switcherViews")
    List<SwitcherViewsItem> switcherViews,

    @SerializedName("canBePublic")
    boolean canBePublic,

    @SerializedName("canBePrivate")
    boolean canBePrivate,

    @SerializedName("backgroundImageScaled")
    List<BackgroundImageScaledItem> backgroundImageScaled,

    @SerializedName("invitations")
    String invitations,

    @SerializedName("selfJoin")
    boolean selfJoin,

    @SerializedName("backgroundBrightness")
    String backgroundBrightness,

    @SerializedName("backgroundColor")
    Object backgroundColor,

    @SerializedName("comments")
    String comments,

    @SerializedName("backgroundTopColor")
    String backgroundTopColor,

    @SerializedName("canBeOrg")
    boolean canBeOrg,

    @SerializedName("backgroundBottomColor")
    String backgroundBottomColor,

    @SerializedName("calendarFeedEnabled")
    boolean calendarFeedEnabled,

    @SerializedName("backgroundTile")
    boolean backgroundTile,

    @SerializedName("permissionLevel")
    String permissionLevel,

    @SerializedName("cardAging")
    String cardAging,

    @SerializedName("canInvite")
    boolean canInvite,

    @SerializedName("isTemplate")
    boolean isTemplate,

    @SerializedName("background")
    String background,

    @SerializedName("cardCovers")
    boolean cardCovers
) {
}