package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;
import net.cofcool.sourcebox.util.Utils;

@Entity(name = "action_type")
public record ActionType(

    @ID
    @Column(name = "id", type = JDBCType.VARCHAR, length = 32)
    String id,
    @Column(name = "name", type = JDBCType.VARCHAR, length = 20)
    String name,
    @Column(name = "type", type = JDBCType.VARCHAR, length = 20)
    String type,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createTime
) {

    public ActionType(String name, Type type) {
        this(Utils.md5(name+type), name, type.name(), LocalDateTime.now());
    }

    public enum Type {
        category, device, state, label, todo, link, record
    }
}
