package net.cofcool.toolbox.internal.simplenote.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.sqlclient.Row;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.UUID;
import net.cofcool.toolbox.util.TableInfoHelper.Column;
import net.cofcool.toolbox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.toolbox.util.TableInfoHelper.Entity;
import net.cofcool.toolbox.util.TableInfoHelper.ID;

@Entity(name = "note")
public record Note(
    @ID
    @Column(name = "id", type = JDBCType.VARCHAR, length = 64)
    String id,
    @Column(name = "content", type = JDBCType.VARCHAR, length = 5000)
    String content,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "date", type = JDBCType.TIMESTAMP)
    LocalDateTime date,
    @Column(name = "state", type = JDBCType.VARCHAR, length = 15)
    String state
) {

    public static Note init(String content) {
        return new Note(UUID.randomUUID().toString(), content, LocalDateTime.now(), NoteState.NORMAL.name());
    }

    @DefaultMapper
    public static Note of(Row row) {
        return new Note(row.getString("ID"), row.getString("CONTENT"), row.getLocalDateTime("DATE"), row.getString("STATE"));
    }

    public enum NoteState {
        NORMAL, DELETED, DELETE_FLAG, MEMORY
    }
}
