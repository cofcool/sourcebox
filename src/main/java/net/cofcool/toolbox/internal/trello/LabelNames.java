package net.cofcool.toolbox.internal.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LabelNames(

    @JsonProperty("pink")
    String pink,

    @JsonProperty("orange_light")
    String orangeLight,

    @JsonProperty("yellow")
    String yellow,

    @JsonProperty("blue_light")
    String blueLight,

    @JsonProperty("red_dark")
    String redDark,

    @JsonProperty("red")
    String red,

    @JsonProperty("sky_dark")
    String skyDark,

    @JsonProperty("black_light")
    String blackLight,

    @JsonProperty("orange_dark")
    String orangeDark,

    @JsonProperty("sky_light")
    String skyLight,

    @JsonProperty("blue_dark")
    String blueDark,

    @JsonProperty("red_light")
    String redLight,

    @JsonProperty("purple_dark")
    String purpleDark,

    @JsonProperty("yellow_dark")
    String yellowDark,

    @JsonProperty("sky")
    String sky,

    @JsonProperty("black_dark")
    String blackDark,

    @JsonProperty("green_light")
    String greenLight,

    @JsonProperty("green_dark")
    String greenDark,

    @JsonProperty("green")
    String green,

    @JsonProperty("pink_light")
    String pinkLight,

    @JsonProperty("lime_dark")
    String limeDark,

    @JsonProperty("purple_light")
    String purpleLight,

    @JsonProperty("pink_dark")
    String pinkDark,

    @JsonProperty("lime")
    String lime,

    @JsonProperty("black")
    String black,

    @JsonProperty("lime_light")
    String limeLight,

    @JsonProperty("orange")
    String orange,

    @JsonProperty("blue")
    String blue,

    @JsonProperty("yellow_light")
    String yellowLight,

    @JsonProperty("purple")
    String purple
) {
}