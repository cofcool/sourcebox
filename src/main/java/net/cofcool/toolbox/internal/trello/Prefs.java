package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Prefs(

    @JsonProperty("canBeEnterprise")
    boolean canBeEnterprise,

    @JsonProperty("hideVotes")
    boolean hideVotes,

    @JsonProperty("backgroundImage")
    String backgroundImage,

    @JsonProperty("voting")
    String voting,

    @JsonProperty("hiddenPluginBoardButtons")
    List<Object> hiddenPluginBoardButtons,

    @JsonProperty("switcherViews")
    List<SwitcherViewsItem> switcherViews,

    @JsonProperty("canBePublic")
    boolean canBePublic,

    @JsonProperty("canBePrivate")
    boolean canBePrivate,

    @JsonProperty("backgroundImageScaled")
    List<BackgroundImageScaledItem> backgroundImageScaled,

    @JsonProperty("invitations")
    String invitations,

    @JsonProperty("selfJoin")
    boolean selfJoin,

    @JsonProperty("backgroundBrightness")
    String backgroundBrightness,

    @JsonProperty("backgroundColor")
    Object backgroundColor,

    @JsonProperty("comments")
    String comments,

    @JsonProperty("backgroundTopColor")
    String backgroundTopColor,

    @JsonProperty("canBeOrg")
    boolean canBeOrg,

    @JsonProperty("backgroundBottomColor")
    String backgroundBottomColor,

    @JsonProperty("calendarFeedEnabled")
    boolean calendarFeedEnabled,

    @JsonProperty("backgroundTile")
    boolean backgroundTile,

    @JsonProperty("permissionLevel")
    String permissionLevel,

    @JsonProperty("cardAging")
    String cardAging,

    @JsonProperty("canInvite")
    boolean canInvite,

    @JsonProperty("isTemplate")
    boolean isTemplate,

    @JsonProperty("background")
    String background,

    @JsonProperty("cardCovers")
    boolean cardCovers
) {
}