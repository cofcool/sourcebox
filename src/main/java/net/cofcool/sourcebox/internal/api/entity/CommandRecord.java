package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.sqlclient.Row;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import net.cofcool.sourcebox.util.EntityAction;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;

@Builder
@Entity(name = "command_record")
public record CommandRecord(
    @ID
    @Column(name = "id", type = JDBCType.VARCHAR, length = 32)
    String id,
    @Column(name = "cmd", type = JDBCType.VARCHAR, length = 2048)
    String cmd,
    @Column(name = "alias", type = JDBCType.VARCHAR, length = 50, nullable = true)
    String alias,
    @Column(name = "tags", type = JDBCType.ARRAY, arrayElemType = JDBCType.VARCHAR, arrayElemLength = 30)
    List<String> tags,
    @Column(name = "remark", type = JDBCType.VARCHAR, length = 512, nullable = true)
    String remark,
    @Column(name = "frequency", type = JDBCType.INTEGER)
    Integer frequency,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updateTime
) implements EntityAction<CommandRecord> {

    public CommandRecord(
        String id,
        String cmd,
        String alias,
        List<String> tags,
        String remark,
        Integer frequency,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updateTime) {
        this.id = id != null ? id : (StringUtils.isNotBlank(alias) ? alias : (StringUtils.isNotBlank(cmd) ? Utils.md5(cmd) : null));
        this.cmd = cmd;
        this.alias = alias;
        this.tags = tags;
        this.remark = remark;
        this.frequency = frequency;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public String makeAlias() {
        return "alias "
            + alias.substring(1)
            + "='"
            + cmd
            + "'";
    }

    public boolean hasAlias() {
        return StringUtils.isNotBlank(alias);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    @Override
    public String toString() {
        return String.join(" ", id, cmd, Objects.toString(tags.toString(), "[]"));
    }

    @DefaultMapper
    public static CommandRecord from(Row row) {
        return new CommandRecord(
            row.getString("ID"),
            row.getString("CMD"),
            row.getString("ALIAS"),
            List.of(row.getArrayOfStrings("TAGS")),
            row.getString("REMARK"),
            row.getInteger("FREQUENCY"),
            row.getLocalDateTime("CREATE_TIME"),
            row.getLocalDateTime("UPDATE_TIME")
        );
    }

    public CommandRecord incrementFrequency() {
        return new CommandRecord(id, null, null, null, null, frequency+1, null, LocalDateTime.now());
    }

    @Override
    public CommandRecord beforeUpdate() {
        return new CommandRecord(id, cmd, alias, tags, remark, frequency, createTime, LocalDateTime.now());
    }

    @Override
    public CommandRecord beforeInsert() {
        return new CommandRecord(id, cmd, alias, tags, remark, frequency == null ? 0 : frequency, createTime != null ? createTime : LocalDateTime.now(), LocalDateTime.now());
    }
}
