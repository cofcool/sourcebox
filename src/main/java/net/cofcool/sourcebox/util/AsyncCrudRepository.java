package net.cofcool.sourcebox.util;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.List;

public interface AsyncCrudRepository<T> {

    Future<T> save(T entity);

    Future<Void> save(List<T> entities);

    Future<Boolean> delete(String id);

    Future<List<T>> find(T condition);

    Future<List<T>> find(QueryBuilder condition);

    Future<RowSet<Row>> execute(QueryBuilder condition);

    Future<List<T>> find();

    Future<T> find(String id);

    Future<Integer> count(QueryBuilder condition);
}
