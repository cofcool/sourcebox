package net.cofcool.toolbox.internal.simplenote.entity;

import io.vertx.sqlclient.Row;
import java.sql.JDBCType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import net.cofcool.toolbox.util.TableInfoHelper.Column;
import net.cofcool.toolbox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.toolbox.util.TableInfoHelper.Entity;
import net.cofcool.toolbox.util.TableInfoHelper.ID;
import net.cofcool.toolbox.util.Utils;

@Entity(name = "action_record")
public record ActionRecord(
    @ID
     @Column(name = "id", type = JDBCType.CHAR, length = 32)
    String id,
     @Column(name = "name", type = JDBCType.VARCHAR, length = 50)
    String name,
     @Column(name = "icon", type = JDBCType.VARCHAR, length = 256, nullable = true)
    String icon,
     @Column(name = "index", type = JDBCType.VARCHAR, length = 100, nullable = true)
    String index,
     @Column(name = "device", type = JDBCType.VARCHAR, length = 30)
    String device,
     @Column(name = "type", type = JDBCType.VARCHAR, length = 20)
    String type,
     @Column(name = "state", type = JDBCType.VARCHAR, length = 20)
    String state,
     @Column(name = "start", type = JDBCType.TIMESTAMP)
    LocalDateTime start,
     @Column(name = "end", type = JDBCType.TIMESTAMP, nullable = true)
    LocalDateTime end,
     @Column(name = "duration", type = JDBCType.INTEGER, nullable = true)
    Integer duration,
     @Column(name = "rating", type = JDBCType.INTEGER, nullable = true)
    Integer rating,
    List<String> comments,
     @Column(name = "labels", type = JDBCType.VARCHAR, length = 120, nullable = true)
    String labels,
     @Column(name = "refs", type = JDBCType.VARCHAR, length = 512, nullable = true)
    String refs,
     @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    LocalDateTime createTime,
     @Column(name = "update_time", type = JDBCType.TIMESTAMP)
    LocalDateTime updateTime
) {

    public ActionRecord(String id, String name, String icon, String index, String device,
        String type,
        String state, LocalDateTime start, LocalDateTime end, Integer duration, Integer rating,
        List<String> comments, String labels, String refs, LocalDateTime createTime,
        LocalDateTime updateTime) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.index = index;
        this.device = device;
        this.type = type;
        this.state = state;
        this.start = start;
        this.end = end;
        this.duration = (duration == null && start != null && end != null) ? Integer.valueOf(Math.toIntExact(Duration.between(start, end).toSeconds())) : duration;
        this.rating = rating;
        this.comments = comments;
        this.labels = labels;
        this.refs = refs;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public ActionRecord(String name, String icon, String index, String device, String type,
        String state, LocalDateTime start, LocalDateTime end, Integer duration, Integer rating,
        List<String> commentIds, String labels, String refs, LocalDateTime createTime) {
        this(Utils.md5(name + type), name, icon, index, device, type, state, start, end, duration, rating, commentIds, labels, refs, createTime, LocalDateTime.now());
    }

    public static ActionRecord copy(ActionRecord record) {
        return new ActionRecord(record.id, record.name, record.icon,
            record.index, record.device, record.type, record.state, record.start, record.end,
            record.duration, record.rating, record.comments, record.labels, record.refs,
            record.createTime == null ? LocalDateTime.now() : record.createTime,
            LocalDateTime.now());
    }

    @DefaultMapper
    public static ActionRecord from(Row row) {
        return new ActionRecord(
            row.getString("ID"),
            row.getString("NAME"),
            row.getString("ICON"),
            row.getString("INDEX"),
            row.getString("DEVICE"),
            row.getString("TYPE"),
            row.getString("STATE"),
            row.getLocalDateTime("START"),
            row.getLocalDateTime("END"),
            row.getInteger("DURATION"),
            row.getInteger("RATING"),
            List.of(),
            row.getString("LABELS"),
            row.getString("REFS"),
            row.getLocalDateTime("CREATE_TIME"),
            row.getLocalDateTime("UPDATE_TIME")

        );
    }
}
