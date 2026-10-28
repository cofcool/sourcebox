package net.cofcool.sourcebox.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RepositoryUtil {

    private static final ConcurrentMap<Class<?>, SqlRepository<?>> REPOSITORIES = new ConcurrentHashMap<>();

    private RepositoryUtil() {
    }

    public static <T> SqlRepository<T> getRepository(Class<T> entity) {
        return repository(entity);
    }

    static void clear() {
        REPOSITORIES.clear();
    }

    @SuppressWarnings("unchecked")
    private static <T> SqlRepository<T> repository(Class<T> entity) {
        return (SqlRepository<T>) REPOSITORIES.computeIfAbsent(entity, SqlRepository::create);
    }
}
