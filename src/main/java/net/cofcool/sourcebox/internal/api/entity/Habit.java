package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.sqlclient.Row;
import lombok.Builder;
import lombok.With;
import net.cofcool.sourcebox.util.EntityAction;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@With
@Entity(name = "habit")
public record Habit(
    @ID
    @Column(name = "id", type = JDBCType.CHAR, length = 32)
    String id,
    @Column(name = "action_id", type = JDBCType.CHAR, length = 32)
    String actionId,
    @Column(name = "category", type = JDBCType.VARCHAR, length = 20)
    String category,
    @Column(name = "note", type = JDBCType.VARCHAR, length = 1024, nullable = true)
    String note,
    @Column(name = "unit", type = JDBCType.VARCHAR, length = 50, nullable = true)
    String unit,
    @Column(name = "freq_num", type = JDBCType.INTEGER, nullable = true)
    Integer freqNum,
    @Column(name = "freq_den", type = JDBCType.INTEGER, nullable = true)
    Integer freqDen,
    @Column(name = "question", type = JDBCType.VARCHAR, length = 512, nullable = true)
    String question,
    @Column(name = "target_value", type = JDBCType.INTEGER, nullable = true)
    Integer targetValue,
    @Column(name = "target_type", type = JDBCType.VARCHAR, length = 20, nullable = true)
    String targetType,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updateTime
) implements EntityAction<Habit> {

    @DefaultMapper
    public static Habit from(Row row) {
        return new Habit(
            row.getString("ID"),
            row.getString("ACTION_ID"),
            row.getString("CATEGORY"),
            row.getString("NOTE"),
            row.getString("UNIT"),
            row.getInteger("FREQ_NUM"),
            row.getInteger("FREQ_DEN"),
            row.getString("QUESTION"),
            row.getInteger("TARGET_VALUE"),
            row.getString("TARGET_TYPE"),
            row.getLocalDateTime("CREATE_TIME"),
            row.getLocalDateTime("UPDATE_TIME")
        );
    }

    @Override
    public Habit beforeUpdate() {
        return new Habit(id, actionId, category, note, unit, freqNum, freqDen, question, targetValue, targetType,
            createTime, LocalDateTime.now());
    }

    @Override
    public Habit beforeInsert() {
        return new Habit(
            id == null ? Utils.generateShortUUID() : id,
            actionId,
            category,
            note,
            unit,
            freqNum,
            freqDen,
            question,
            targetValue,
            targetType,
            createTime == null ? LocalDateTime.now() : createTime,
            LocalDateTime.now()
        );
    }

    public String calcCheckListName(String desc) {
        return StringUtils.isBlank(desc) ? freqNum + "-" + freqDen : desc;
    }

    public enum TargetType {
        AtLeast, AtMost
    }

    public record HabitRecord(
        ActionRecord action,
        Habit habit,
        List<CheckList> checkLists
    ) {

        public HabitRecord(ActionRecord action, Habit habit) {
            this(action, habit, List.of());
        }
    }
}
