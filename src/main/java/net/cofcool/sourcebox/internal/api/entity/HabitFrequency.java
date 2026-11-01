package net.cofcool.sourcebox.internal.api.entity;

import java.util.Objects;

public record HabitFrequency(
     Integer numerator,
     Integer denominator
) {

    public static final HabitFrequency DAILY = new HabitFrequency(1, 1);
    public static final HabitFrequency THREE_TIMES_PER_WEEK = new HabitFrequency(3, 7);
    public static final HabitFrequency TWO_TIMES_PER_WEEK = new HabitFrequency(2, 7);
    public static final HabitFrequency WEEKLY = new HabitFrequency(1, 7);

    public HabitFrequency(Integer numerator, Integer denominator) {
        if (Objects.equals(numerator, denominator)) {
            this.denominator = 1;
            this.numerator = 1;
        } else {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }

    public float toDouble() {
        return numerator.floatValue() / denominator;
    }

}
