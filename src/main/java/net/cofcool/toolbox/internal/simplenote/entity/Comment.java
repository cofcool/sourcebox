package net.cofcool.toolbox.internal.simplenote.entity;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import net.cofcool.toolbox.util.TableInfoHelper.Column;
import net.cofcool.toolbox.util.TableInfoHelper.Entity;
import net.cofcool.toolbox.util.TableInfoHelper.ID;

@Entity(name = "comment")
public record Comment(
    @ID
    @Column(name = "id", type = JDBCType.CHAR, length = 32)
    String id,
    @Column(name = "action_id", type = JDBCType.CHAR, length = 32)
    String actionId,
    @Column(name = "content", type = JDBCType.VARCHAR, length = 1024)
    String content,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP)
    LocalDateTime updateTime
) {

    public Comment(String actionId) {
        this(null, actionId, null, null, null);
    }
}
