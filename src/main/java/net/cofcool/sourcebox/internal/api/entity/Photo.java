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
@Entity(name = "photo")
public record Photo(
    @ID @Column(name = "id", type = JDBCType.VARCHAR, length = 64) String id,
    @Column(name = "album_id", type = JDBCType.VARCHAR, length = 64) String albumId,
    @Column(name = "file_name", type = JDBCType.VARCHAR, length = 1024) String fileName,
    @Column(name = "relative_path", type = JDBCType.VARCHAR, length = 2048) String relativePath,
    @Column(name = "source_path", type = JDBCType.VARCHAR, length = 4096) String sourcePath,
    @Column(name = "mime_type", type = JDBCType.VARCHAR, length = 100, nullable = true) String mimeType,
    @Column(name = "sha256", type = JDBCType.VARCHAR, length = 64) String sha256,
    @Column(name = "size", type = JDBCType.BIGINT) Long size,
    @Column(name = "width", type = JDBCType.INTEGER, nullable = true) Integer width,
    @Column(name = "height", type = JDBCType.INTEGER, nullable = true) Integer height,
    @Column(name = "taken_at", type = JDBCType.TIMESTAMP, nullable = true) @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime takenAt,
    @Column(name = "metadata_status", type = JDBCType.VARCHAR, length = 20) String metadataStatus,
    @Column(name = "metadata_json", type = JDBCType.VARCHAR, length = 8000, nullable = true) String metadataJson,
    @Column(name = "status", type = JDBCType.VARCHAR, length = 20) String status,
    @Column(name = "trashed_at", type = JDBCType.TIMESTAMP, nullable = true) LocalDateTime trashedAt,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP) @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP) @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updateTime
) {
    @DefaultMapper
    public static Photo of(Row row) {
        return new Photo(
            row.getString("ID"), row.getString("ALBUM_ID"), row.getString("FILE_NAME"),
            row.getString("RELATIVE_PATH"), row.getString("SOURCE_PATH"), row.getString("MIME_TYPE"),
            row.getString("SHA256"), row.getLong("SIZE"), row.getInteger("WIDTH"), row.getInteger("HEIGHT"),
            row.getLocalDateTime("TAKEN_AT"), row.getString("METADATA_STATUS"),
            row.getString("METADATA_JSON"), row.getString("STATUS"), row.getLocalDateTime("TRASHED_AT"),
            row.getLocalDateTime("CREATE_TIME"), row.getLocalDateTime("UPDATE_TIME")
        );
    }
}
