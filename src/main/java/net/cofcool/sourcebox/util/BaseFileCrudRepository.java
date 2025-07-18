package net.cofcool.sourcebox.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import net.cofcool.sourcebox.logging.Logger;
import net.cofcool.sourcebox.logging.LoggerFactory;
import org.apache.commons.io.FileUtils;

public abstract class BaseFileCrudRepository<T> implements CrudRepository<T> {


    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Getter
    private final File file;
    protected final Map<String, T> dataCache = new ConcurrentHashMap<>();

    public BaseFileCrudRepository(String path) {
        this.file = new File(path);
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException("Init file error", e);
        }
    }

    private void init() throws IOException {
        if (!file.exists()) {
            var data = initContent();
            if (data != null) {
                FileUtils.write(file, data, StandardCharsets.UTF_8);
                log.info("Init {0} success", file);
            }
        } else {
            loadData(FileUtils.readFileToByteArray(file));
        }
    }

    protected String initContent() {
        return "[]";
    }

    protected abstract void loadData(byte[] data);
    protected abstract T saveData(T entity);

    @Override
    public void delete(String id) {
        dataCache.remove(id);
        refreshDirtyData(id);
    }

    @Override
    public void save(List<T> entities) {
        for (T data : entities) {
            saveData(data);
        }
        refreshDirtyData(entities.size());
    }

    @Override
    public T save(T entity) {
        var ret = saveData(entity);
        refreshDirtyData(1);
        return ret;
    }

    @Override
    public Optional<T> find(String id) {
        return Optional.ofNullable(dataCache.get(id));
    }

    @Override
    public List<T> find() {
        return List.copyOf(dataCache.values());
    }

    @Override
    public List<T> find(QueryBuilder condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer count(QueryBuilder condition) {
        return find(condition).size();
    }

    protected void refreshDirtyData(Object data) {
        try {
            FileUtils.write(file, JsonUtil.toJson(dataCache.values()), StandardCharsets.UTF_8);
            log.debug("Update {0} data cache", dataCache.size());
        } catch (IOException e) {
            throw new RuntimeException("Write data error", e);
        }
    }
}
