package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;
import net.cofcool.sourcebox.util.Utils;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Entity(name = "check_list")
public record CheckList(
    @ID
    @Column(name = "id", type = JDBCType.CHAR, length = 32)
    String id,
    @Column(name = "action_id", type = JDBCType.CHAR, length = 32)
    String actionId,
    @Column(name = "content", type = JDBCType.VARCHAR, length = 1024)
    String content,
    @Column(name = "first", type = JDBCType.INTEGER)
    Integer first,
    @Column(name = "last", type = JDBCType.INTEGER)
    Integer last,
    @Column(name = "current", type = JDBCType.INTEGER)
    Integer current,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updateTime
) {

    public CheckList(String actionId) {
        this(null, actionId, null, null, null, null, null, null);
    }

    public static String toPrintStr(List<CheckList> objects) {
        if (objects == null) {
            return "";
        }
        return objects.stream()
            .map(obj ->
                String.join(" | ",
                    obj.id(), obj.content(), obj.first().toString(), obj.last().toString(), obj.current().toString(),
                    Utils.formatDatetime(obj.createTime())
                )
            )
            .map(s -> s +"\n" + "-".repeat(s.length()))
            .collect(Collectors.joining("\n"));
    }
}
