package net.cofcool.sourcebox.util;

import io.vertx.sqlclient.Row;
import java.beans.Transient;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.JDBCType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TableInfoHelper {

    private static final Map<Class<?>, TableInfo> CACHED_META = new ConcurrentHashMap<>();

    public static TableInfo tableInfo(Class<?> type) {
        return CACHED_META.computeIfAbsent(type, TableInfoHelper::parsingEntity);
    }

    private static TableInfo parsingEntity(Class<?> type) {
        Entity entity = type.getAnnotation(Entity.class);

        return new TableInfo(entity.name(), type, parsingColumn(type));
    }

    private static Map<String, TableProperty> parsingColumn(Class<?> type) {
        Map<String, TableProperty> columns = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            var name = field.getName();
            if (field.getAnnotation(Transient.class) != null
                || Modifier.isStatic(field.getModifiers())
                || name.equals("serialVersionUID")) {
                continue;
            }

            var column = field.getAnnotation(Column.class);
            if (column != null) {
                field.setAccessible(true);
                columns.put(name, new TableProperty(column.name(), column.type(), column.length(),
                    column.nullable(), field, field.getAnnotation(ID.class) != null,
                    column.arrayElemType(), column.arrayElemLength()
                ));
            }
        }

        return columns;
    }

    public record TableInfo(
        String name,
        Class<?> type,
        TableProperty id,
        Map<String, TableProperty> columns,
        Method defaultMapper
    ) {

        public TableInfo(String name, Class<?> type, Map<String, TableProperty> columns) {
            this(
                name,
                type,
                columns.values().stream().filter(TableProperty::isId).findAny().orElse(null),
                columns,
                Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getAnnotation(DefaultMapper.class) != null).findAny().orElse(null)
            );
        }

        @SuppressWarnings("unchecked")
        public <T> T newInstance(Row row, List<String> columnNames) {
            if (defaultMapper == null) {
                if (Record.class.isAssignableFrom(type)) {
                    return  (T) Utils.instance(type, columnNames.stream().map(row::getValue).toArray());
                } else {
                    var a = (T) Utils.instance(type);
                    columns.forEach((k, v) -> v.setVal(a, row.getValue(v.name())));
                    return a;
                }
            } else {
                try {
                    return (T) defaultMapper.invoke(null, row);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @SuppressWarnings("unchecked")
        public <T> Optional<T> getIdVal(Object obj) {
            if (id != null) {
                return (Optional<T>) Optional.ofNullable(id.getVal(obj));
            }
            return Optional.empty();
        }

        public void setIdVal(Object obj, Object val) {
            if (id != null) {
                id.setVal(obj, val);
            }
        }

        public String ddl() {
            return "CREATE TABLE IF NOT EXISTS " + name
                + " ("
                + columns.values().stream()
                .map(a ->
                    a.name()
                        + " "
                        + (a.arrayElemType == JDBCType.NULL ? "" : a.arrayElemType.getName() + (a.arrayElemLength() > 0 ? "(" + a.arrayElemLength() + ") " : " "))
                        + a.type().getName()
                        + (a.length() > 0 ? "(" + a.length() + ")" : "")
                        + (a.nullable ? " null" : " not null")
                        + (a.isId() ? " PRIMARY KEY " : "")
                )
                .collect(Collectors.joining(","))
                + ")";
        }

    }

    public record TableProperty(
        String name,
        JDBCType type,
        int length,
        boolean nullable,
        Field field,
        boolean isId,
        JDBCType arrayElemType,
        int arrayElemLength
    ) {

        @Override
        public int length() {
            if (type == JDBCType.VARCHAR && length <= 0) {
                throw new IllegalArgumentException("length must be specified in type definition: VARCHAR");
            }
            return length;
        }

        @SuppressWarnings("rawtypes")
        public <T> Object getVal(T entity) {
            try {
                var v = field.get(entity);
                if (v instanceof List v1) {
                    v = v1.toArray();
                }
                return v;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> void setVal(T entity, Object v) {
            try {
                field.set(entity, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Entity {

        String name();
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Column {
        String name();
        JDBCType type();
        int length() default -1;
        boolean nullable() default false;
        JDBCType arrayElemType() default JDBCType.NULL;
        int arrayElemLength() default -1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ID {
        boolean generated() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DefaultMapper {
    }

}
