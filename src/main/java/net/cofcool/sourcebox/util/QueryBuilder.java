package net.cofcool.sourcebox.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

public final class QueryBuilder {

    private final List<String> selectFields = new ArrayList<>();
    private String fromTable;
    private final List<String> whereClauses = new ArrayList<>();
    @Getter
    private final List<Object> parameters = new ArrayList<>();
    private String orderByClause;
    private Integer limit;

    private QueryBuilder() {
    }

    public static QueryBuilder builder() {
        return new QueryBuilder();
    }

    public QueryBuilder select(String... fields) {
        selectFields.addAll(Arrays.asList(fields));
        return this;
    }

    public QueryBuilder select() {
        selectFields.add("*");
        return this;
    }

    public QueryBuilder from(String table) {
        this.fromTable = table;
        return this;
    }

    public QueryBuilder from(Class<?> table) {
        this.fromTable = TableInfoHelper.tableInfo(table).name();
        return this;
    }

    public QueryBuilder and(String condition, Object... params) {
        if (!whereClauses.isEmpty()) {
            whereClauses.add("AND " + condition);
        } else {
            whereClauses.add(condition);
        }
        parameters.addAll(Arrays.asList(params));
        return this;
    }

    public QueryBuilder or(String condition, Object... params) {
        if (!whereClauses.isEmpty()) {
            whereClauses.add("OR " + condition);
        } else {
            whereClauses.add(condition);
        }
        parameters.addAll(Arrays.asList(params));
        return this;
    }

    public QueryBuilder orderBy(String orderBy) {
        this.orderByClause = orderBy;
        return this;
    }

    public QueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder count() {
        return select("count(1)");
    }

    public String build() {
        if (fromTable == null || selectFields.isEmpty()) {
            throw new IllegalStateException("SELECT and FROM must be specified.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(String.join(", ", selectFields));
        sb.append(" FROM ").append(fromTable);

        if (!whereClauses.isEmpty()) {
            sb.append(" WHERE ");
            for (int i = 0; i < whereClauses.size(); i++) {
                String clause = whereClauses.get(i);
                if (i > 0 && !clause.startsWith("AND") && !clause.startsWith("OR")) {
                    sb.append(" AND ");
                }
                sb.append(clause).append(" ");
            }
        }

        if (orderByClause != null) {
            sb.append(" ORDER BY ").append(orderByClause).append(" ");
        }

        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
        }

        return sb.toString().trim() + ";";
    }

}
