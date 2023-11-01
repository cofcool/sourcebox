package net.cofcool.toolbox.internal.simplenote.entity;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import net.cofcool.toolbox.util.TableInfoHelper.Column;
import net.cofcool.toolbox.util.TableInfoHelper.Entity;
import net.cofcool.toolbox.util.TableInfoHelper.ID;

@Entity(name = "action_type")
public record ActionType(

    @ID
    @Column(name = "name", type = JDBCType.CHAR, length = 20)
    String name,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    LocalDateTime createTime

) {

    public ActionType(String name) {
        this(name, LocalDateTime.now());
    }
}
