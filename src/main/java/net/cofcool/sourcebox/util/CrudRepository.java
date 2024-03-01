package net.cofcool.sourcebox.util;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T> {

    String STARTED = "STARTED";
    String LIST = "LIST";
    String SAVE = "SAVE";
    String DELETE = "DELETE";
    String DIRTY = "DIRTY";

    T save(T entity);

    void save(List<T> entities);

    void delete(String id);

    List<T> find(T condition);

    List<T> find();

    Optional<T> find(String id);

}
