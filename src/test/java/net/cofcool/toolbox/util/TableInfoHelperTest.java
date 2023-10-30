package net.cofcool.toolbox.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.sql.JDBCType;
import java.util.List;
import lombok.Data;
import net.cofcool.toolbox.util.TableInfoHelper.Column;
import net.cofcool.toolbox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.toolbox.util.TableInfoHelper.Entity;
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
        var ret = info.newInstance(new RowImpl(), List.of("name"));
        assertNotNull(ret);
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
            return 0;
        }

        @Override
        public Object getValue(int pos) {
            return "test";
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
    static class Demo2 {

        @Column(name = "name", type = JDBCType.CHAR)
        private String name;
    }
}