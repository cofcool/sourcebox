package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.internal.api.entity.ActionState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HabitsTest {

    @Test
    void parsesHabitDefinitions() {
        var definitions = Habits.parseDefinitions("""
            Position,Name,Type,Question,Description,FrequencyNumerator,FrequencyDenominator,Color,Unit,Target Type,Target Value,Archived?
            001,Ge,YES_NO,,,1,1,#1976D2,,,,false
            002,Rd,NUMERICAL,,,1,1,#1976D2,,AT_LEAST,1.0,false
            """);

        Assertions.assertEquals(2, definitions.size());
        var check = definitions.get(0);
        Assertions.assertEquals("Ge", check.action().name());
        Assertions.assertEquals("001", check.action().index());
        Assertions.assertEquals("#1976D2", check.action().icon());
        Assertions.assertEquals(ActionState.doing.name(), check.action().state());
        Assertions.assertEquals("check", check.habit().category());
        Assertions.assertEquals(1, check.habit().freqNum());

        var value = definitions.get(1);
        Assertions.assertEquals("value", value.habit().category());
        Assertions.assertEquals("AtLeast", value.habit().targetType());
        Assertions.assertEquals(1, value.habit().targetValue());
    }

    @Test
    void parsesDailyValuesAndSkipsUnknownValues() {
        var records = Habits.parseDailyRecords("""
            Date,Ge,Rd,
            2026-10-30,YES_MANUAL,800,
            2026-10-31,YES_AUTO,UNKNOWN,
            2026-11-01,NO,SKIP,
            """);

        Assertions.assertEquals(3, records.size());
        Assertions.assertEquals(1, Habits.parseDailyValue(records.get(0).values().get("Ge")));
        Assertions.assertEquals(800, Habits.parseDailyValue(records.get(0).values().get("Rd")));
        Assertions.assertNull(Habits.parseDailyValue(records.get(1).values().get("Ge")));
        Assertions.assertNull(Habits.parseDailyValue(records.get(1).values().get("Rd")));
        Assertions.assertEquals(0, Habits.parseDailyValue(records.get(2).values().get("Ge")));
        Assertions.assertNull(Habits.parseDailyValue(records.get(2).values().get("Rd")));
    }

    @Test
    void parsesQuotedFieldsAndArchivedHabits() {
        var definitions = Habits.parseDefinitions("""
            Position,Name,Type,Question,Description,FrequencyNumerator,FrequencyDenominator,Color,Unit,Target Type,Target Value,Archived?
            003,"Walk, outside",YES_NO,"Did you walk?","At least, briefly",1,1,#1976D2,,,,true
            """);

        Assertions.assertEquals("Walk, outside", definitions.getFirst().action().name());
        Assertions.assertEquals("Did you walk?", definitions.getFirst().habit().question());
        Assertions.assertEquals("At least, briefly", definitions.getFirst().habit().note());
        Assertions.assertEquals(ActionState.cancel.name(), definitions.getFirst().action().state());
    }

    @Test
    void parsesEachDailyCellByItsValue() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Habits.parseDailyValue("not-a-value"));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Habits.parseDailyValue("1.5"));
    }
}
