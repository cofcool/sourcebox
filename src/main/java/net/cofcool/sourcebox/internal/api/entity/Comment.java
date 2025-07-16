package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import lombok.Builder;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;

@Builder
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updateTime
) {

    public Comment(String actionId) {
        this(null, actionId, null, null, null);
    }
}
