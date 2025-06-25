package net.cofcool.sourcebox.util;

public interface EntityAction<T> {

    T beforeUpdate();
    T beforeInsert();

}
