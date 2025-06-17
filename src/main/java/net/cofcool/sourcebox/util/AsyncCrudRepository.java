package net.cofcool.sourcebox.util;

import io.vertx.core.Future;
import java.util.List;

public interface AsyncCrudRepository<T> {

    Future<T> save(T entity);

    Future<Void> save(List<T> entities);

    Future<Void> delete(String id);

    Future<List<T>> find(T condition);

    Future<List<T>> find(QueryBuilder condition);

    Future<List<T>> find();

    Future<T> find(String id);

}
