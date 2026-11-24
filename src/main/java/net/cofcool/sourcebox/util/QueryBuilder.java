package net.cofcool.sourcebox.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import lombok.Getter;

public final class QueryBuilder {

    private final List<String> selectFields = new ArrayList<>();
    private String fromTable;
    private final List<String> joinClauses = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    @Getter
    private final List<Object> parameters = new ArrayList<>();
    private String orderByClause;
    private String groupByClause;
    private Integer limit;
    private Long offset;

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

    public QueryBuilder join(String joinClause) {
        this.joinClauses.add("JOIN " + joinClause);
        return this;
    }

    public QueryBuilder leftJoin(String joinClause) {
        this.joinClauses.add("LEFT JOIN " + joinClause);
        return this;
    }

    public QueryBuilder and(boolean predicate, String condition, Supplier<Object[]> params) {
        if (predicate) {
            and(condition, params.get());
        }

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

    public QueryBuilder groupBy(String groupBy) {
        this.groupByClause = groupBy;
        return this;
    }

    public QueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder offset(long offset) {
        this.offset = offset;
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
        if (!joinClauses.isEmpty()) {
            sb.append(" ").append(String.join(" ", joinClauses));
        }

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

        if (groupByClause != null) {
            sb.append(" GROUP BY ").append(groupByClause).append(" ");
        }

        if (orderByClause != null) {
            sb.append(" ORDER BY ").append(orderByClause).append(" ");
        }

        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
        }

        if (offset != null) {
            sb.append(" OFFSET ").append(offset);
        }

        return sb.toString().trim() + ";";
    }

}
