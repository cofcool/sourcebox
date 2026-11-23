package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.sqlclient.Row;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import lombok.Builder;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;

@Builder
@Entity(name = "photo_album")
public record PhotoAlbum(
    @ID @Column(name = "id", type = JDBCType.VARCHAR, length = 64) String id,
    @Column(name = "name", type = JDBCType.VARCHAR, length = 255) String name,
    @Column(name = "type", type = JDBCType.VARCHAR, length = 20) String type,
    @Column(name = "source_roots", type = JDBCType.VARCHAR, length = 4000) String sourceRoots,
    @Column(name = "recursive", type = JDBCType.BOOLEAN) Boolean recursive,
    @Column(name = "cover_photo_id", type = JDBCType.VARCHAR, length = 64, nullable = true) String coverPhotoId,
    @Column(name = "status", type = JDBCType.VARCHAR, length = 20) String status,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP) @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP) @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updateTime
) {

    @DefaultMapper
    public static PhotoAlbum of(Row row) {
        return new PhotoAlbum(
            row.getString("ID"), row.getString("NAME"), row.getString("TYPE"),
            row.getString("SOURCE_ROOTS"), row.getBoolean("RECURSIVE"),
            row.getString("COVER_PHOTO_ID"), row.getString("STATUS"),
            row.getLocalDateTime("CREATE_TIME"), row.getLocalDateTime("UPDATE_TIME")
        );
    }
}
