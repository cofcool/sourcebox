package net.cofcool.sourcebox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeroImageUrl(

    @JsonProperty("@2x")
    String jsonMember2x,

    @JsonProperty("@1x")
    String jsonMember1x,

    @JsonProperty("_id")
    String id
) {
}