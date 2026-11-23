package net.cofcool.sourcebox.internal.api.entity;

import io.vertx.sqlclient.Row;
import lombok.Builder;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;

import java.sql.JDBCType;
import java.time.LocalDateTime;

@Builder
@Entity(name = "smart_album")
public record SmartAlbum(
    @ID @Column(name = "id", type = JDBCType.VARCHAR, length = 64) String id,
    @Column(name = "name", type = JDBCType.VARCHAR, length = 255) String name,
    @Column(name = "match_type", type = JDBCType.VARCHAR, length = 10) String matchType,
    @Column(name = "rules_json", type = JDBCType.VARCHAR, length = 8000) String rulesJson,
    @Column(name = "enabled", type = JDBCType.BOOLEAN) Boolean enabled,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP) LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP) LocalDateTime updateTime
) {
    @DefaultMapper
    public static SmartAlbum of(Row row) {
        return new SmartAlbum(
            row.getString("ID"), row.getString("NAME"), row.getString("MATCH_TYPE"),
            row.getString("RULES_JSON"), row.getBoolean("ENABLED"),
            row.getLocalDateTime("CREATE_TIME"), row.getLocalDateTime("UPDATE_TIME")
        );
    }
}
