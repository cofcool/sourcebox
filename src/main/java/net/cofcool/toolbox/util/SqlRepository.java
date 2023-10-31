package net.cofcool.toolbox.util;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.CustomLog;
import net.cofcool.toolbox.App;
import net.cofcool.toolbox.util.TableInfoHelper.TableInfo;
import net.cofcool.toolbox.util.TableInfoHelper.TableProperty;


// do not support composite entity
@CustomLog
public final class SqlRepository<T> implements AsyncCrudRepository<T> {

    public static <T> SqlRepository<T> create(Vertx vertx, Class<T> entity) {
        var dir = App.globalCfgDir("data.db");
        var url = "jdbc:hsqldb:file:" + dir;
        VertxUtils.getJDBCPool(vertx, url, "sa", "");
        var repository = new SqlRepository<>(entity);

        var ddl = repository.tableInfo.ddl();
        vertx.executeBlocking(() -> {
                try (var conn = DriverManager.getConnection(url);
                    var s = conn.createStatement()) {
                    return Future.succeededFuture(s.execute(ddl));
                } catch (SQLException e) {
                    return Future.failedFuture(e);
                }
            })
            .onSuccess(a -> log.debug("Create {0} table: {1}", dir, ddl))
            .onFailure(e -> log.error("Init table" + ddl + " error", e));

        return repository;
    }

    private static final Function<Set<String>, String> WHERE_SQL_GENERATOR = (c) -> {
        String s = c.stream().map(a -> a + "=?").collect(Collectors.joining(" and "));
        return s.isEmpty() ? "" : " where " + s;
    };

    private final TableInfo tableInfo;

    private SqlRepository(Class<T> entity) {
        this.tableInfo = TableInfoHelper.tableInfo(entity);
    }

    private JDBCPool getPool() {
        return VertxUtils.getJDBCPool();
    }

    @Override
    public Future<T> save(T entity) {
        Optional<String> id = tableInfo.getIdVal(entity);
        if (id.isEmpty()) {
            return insert(entity);
        } else {
            return find(id.get()).compose(
                n -> update(id.get(), n, entity),
                e -> insert(entity)
            );
        }
    }

    private Future<T> update(Object id, T old, T entity) {
        var columns = readProperties(entity);
        var args = new ArrayList<>(columns.values());
        args.add(id);
        return getPool()
            .preparedQuery(
                "UPDATE " + tableInfo.name()
                    + " SET "
                    + columns.keySet().stream().map(k -> " " + k + "=?").collect(Collectors.joining(","))
                + " WHERE id = ? "
            )
            .execute(Tuple.wrap(args.toArray()))
            .compose(rows -> rows.rowCount() > 0 ? Future.succeededFuture(entity) : Future.failedFuture("No data update"));
    }

    private Future<T> insert(T entity) {
        var columns = readProperties(entity);
        return getPool()
            .preparedQuery(
                "INSERT INTO " + tableInfo.name()
                    + "(" + String.join(",", columns.keySet()) + ")"
                    + " VALUES "
                    + "(" + columns.keySet().stream().map(e -> "?").collect(Collectors.joining(","))
                    + ")"
            )
            .execute(Tuple.wrap(columns.values().toArray()))
            .compose(rows -> {
                if (rows.rowCount() > 0) {
                    var lastInsertId = rows.property(JDBCPool.GENERATED_KEYS);
                    if (lastInsertId != null) {
                        var newId = lastInsertId.getString(0);
                        tableInfo.setIdVal(entity, newId);
                    }
                    return Future.succeededFuture(entity);
                } else {
                    return Future.failedFuture("No data save");
                }
            });
    }

    @Override
    public Future<Void> save(List<T> entities) {
        var columns = tableInfo.columns();
        return getPool()
            .preparedQuery(
                "INSERT INTO " + tableInfo.name()
                    + " (" + String.join(",", columns.values().stream().map(TableProperty::name).toList()) + ") "
                    + " VALUES "
                    + "(" + IntStream.range(0, columns.size()).mapToObj(i -> "?").collect(Collectors.joining(","))+ ")"
            )
            .executeBatch(
                entities.stream()
                    .map(e -> Tuple.wrap(readProperties(e, false).values().toArray()))
                    .toList()
            )
            .compose(rows -> rows.rowCount() > 0 ? Future.succeededFuture() : Future.failedFuture("No data save"));
    }

    @Override
    public Future<Void> delete(String id) {
        return getPool()
                .preparedQuery(
                    "delete from "
                        + tableInfo.name()
                        + " where "
                        + tableInfo.id().name()
                        + " = ?"
                )
                .execute(Tuple.of(id))
                .compose(rows -> rows.rowCount() > 0 ? Future.succeededFuture() : Future.failedFuture("No data delete"));
    }


    @Override
    public Future<List<T>> find(T condition) {
        var p = readProperties(condition);
        return getPool()
            .preparedQuery(
                "select * from " + tableInfo.name() + WHERE_SQL_GENERATOR.apply(p.keySet()))
            .execute(p.isEmpty() ? Tuple.tuple() : Tuple.wrap(p.values().toArray()))
            .compose(r -> Future.succeededFuture(extractRow(r)));
    }

    @Override
    public Future<List<T>> find() {
        return
            getPool()
                .preparedQuery("select * from " + tableInfo.name())
                .execute()
                .compose(r -> Future.succeededFuture(extractRow(r)));
    }

    private List<T> extractRow(RowSet<Row> ret) {
        var list = new ArrayList<T>();
        for (Row row : ret) {
            list.add(tableInfo.newInstance(row, ret.columnsNames()));
        }
        return list;
    }

    @Override
    public Future<T> find(String id) {
        return getPool()
                .preparedQuery(
                    "select * from " + tableInfo.name() + " where " + tableInfo.id().name()
                        + " = ?")
                .execute(Tuple.of(id))
                .compose(r -> {
                    List<T> list = extractRow(r);
                    if (list.isEmpty()) {
                        return Future.failedFuture("Can not find " + id);
                    } else {
                        return Future.succeededFuture(list.getFirst());
                    }
                });
    }


    private Map<String, Object> readProperties(T entity) {
        return readProperties(entity, true);
    }

    private Map<String, Object> readProperties(T entity, boolean ignoreNull) {
        Map<String, Object> columns = new LinkedHashMap<>();
        tableInfo.columns().forEach((k, v) -> {
            try {
                Object invoke = v.getVal(entity);
                if (!ignoreNull || invoke != null) {
                    columns.put(v.name(), invoke);
                }
            } catch (Exception e) {
                log.debug("Build where condition error", e);
            }
        });

        return columns;
    }

}
