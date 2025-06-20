package net.cofcool.sourcebox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.sql.JDBCType;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import org.junit.jupiter.api.Test;

class TableInfoHelperTest {

    @Test
    void tableInfo1() {
        var info = TableInfoHelper.tableInfo(Demo1.class);
        var ret = info.newInstance(new RowImpl(), List.of("name"));
        assertNotNull(ret);
    }

    @Test
    void tableInfo2() {
        var info = TableInfoHelper.tableInfo(Demo2.class);
        var ret = info.newInstance(new RowImpl(), List.of("name", "habits"));
        assertNotNull(ret);
    }

    @Test
    void tableDDL() {
        var info = TableInfoHelper.tableInfo(Demo2.class);
        var ret = info.ddl();
        assertEquals(
            "CREATE TABLE IF NOT EXISTS test1 (name CHAR not null,habits CHAR(10) ARRAY not null)",
            ret
        );
    }

    @Test
    void tableInfo3() {
        var info = TableInfoHelper.tableInfo(Demo3.class);
        var ret = info.newInstance(new RowImpl(), List.of("name"));
        assertNotNull(ret);
    }

    static class RowImpl implements Row {

        @Override
        public String getColumnName(int pos) {
            return "name";
        }

        @Override
        public int getColumnIndex(String column) {
            return switch (column) {
                case "name" -> 0;
                case "habits" -> 1;
                default -> throw new IllegalStateException("Unexpected value: " + column);
            };
        }

        @Override
        public Object getValue(int pos) {
            return switch (pos) {
                case 0 -> "test";
                case 1 -> new String[]{"food", "video"};
                default -> throw new IllegalStateException("Unexpected value: " + pos);
            };
        }

        @Override
        public Tuple addValue(Object value) {
            return Tuple.of("test");
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public void clear() {

        }

        @Override
        public List<Class<?>> types() {
            return List.of(String.class);
        }
    }

    @Entity(name = "test")
    record Demo1(
        @Column(name = "name", type = JDBCType.CHAR)
        String name
    ) {}

    @Entity(name = "test")
    record Demo3(
        @Column(name = "name", type = JDBCType.CHAR)
        String name
    ) {

        @DefaultMapper
        public static Demo3 of(Row row) {
            return new Demo3(row.getString("name"));
        }
    }

    @Entity(name = "test1")
    @Data
    @Accessors(chain = true)
    static class Demo2 {

        @Column(name = "name", type = JDBCType.CHAR)
        private String name;

        @Column(name = "habits", type = JDBCType.ARRAY, arrayElemType = JDBCType.CHAR, arrayElemLength = 10)
        private List<String> habits;


        @DefaultMapper
        public static Demo2 of(Row row) {
            return new Demo2()
                .setName(row.getString("name"))
                .setHabits(Arrays.asList((row.getArrayOfStrings("habits"))));
        }
    }
}