package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.sqlclient.Row;
import lombok.Builder;
import lombok.With;
import net.cofcool.sourcebox.util.EntityAction;
import net.cofcool.sourcebox.util.LogseqOutStr;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.DefaultMapper;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;
import net.cofcool.sourcebox.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.sql.JDBCType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.cofcool.sourcebox.util.TableInfoHelper.TableInfo.getIfPresent;

@Builder
@With
@Entity(name = "action_record")
public record ActionRecord(
    @ID
    @Column(name = "id", type = JDBCType.CHAR, length = 32)
    String id,
    @Column(name = "name", type = JDBCType.VARCHAR, length = 512)
    String name,
    @Column(name = "icon", type = JDBCType.VARCHAR, length = 256, nullable = true)
    String icon,
    @Column(name = "index", type = JDBCType.VARCHAR, length = 100, nullable = true)
    String index,
    @Column(name = "device", type = JDBCType.VARCHAR, length = 30, nullable = true)
    String device,
    @Column(name = "type", type = JDBCType.VARCHAR, length = 20)
    String type,
    @Column(name = "category", type = JDBCType.VARCHAR, length = 50, nullable = true)
    String category,
    @Column(name = "state", type = JDBCType.VARCHAR, length = 20)
    String state,
    @Column(name = "start", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime start,
    @Column(name = "end", type = JDBCType.TIMESTAMP, nullable = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
    @Column(name = "remark", type = JDBCType.VARCHAR, length = 512, nullable = true)
    String remark,
    @Column(name = "create_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createTime,
    @Column(name = "update_time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updateTime
) implements EntityAction<ActionRecord> {

    public static final String GENERATED = "GENERATED_ID";

    public ActionRecord(String id, String name, String icon, String index, String device,
        String type, String category,
        String state, LocalDateTime start, LocalDateTime end, Integer duration, Integer rating,
        List<String> comments, String labels, String refs, String remark, LocalDateTime createTime,
        LocalDateTime updateTime) {
        this.id = StringUtils.isBlank(id) && !StringUtils.isBlank(name) && !StringUtils.isBlank(type) ?
            Utils.generateShortUUID() : id;
        this.name = name;
        this.icon = icon;
        this.index = index;
        this.device = device;
        this.type = type;
        this.category = category;
        this.state = state;
        this.start = start;
        this.end = end;
        this.duration = (duration == null && start != null && end != null) ? Integer.valueOf(
            Math.toIntExact(Duration.between(start, end).toSeconds())) : duration;
        this.rating = rating;
        this.comments = comments;
        this.labels = labels;
        this.refs = refs;
        this.remark = remark;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public ActionRecord(String name, String icon, String index, String device, String type, String category,
        String state, LocalDateTime start, LocalDateTime end, Integer duration, Integer rating,
        List<String> commentIds, String labels, String refs, LocalDateTime createTime) {
        this(null, name, icon, index, device, type, category, state, start, end, duration,
            rating, commentIds, labels, refs, null, createTime, LocalDateTime.now());
    }

    public ActionRecord(String refs) {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, refs,
            null,null,
            null);
    }

    public boolean checkId() {
        return !Objects.equals(id, GENERATED);
    }

    public static String toMarkdown(RecordRet recordRet) {
        var out = new LogseqOutStr()
            .block(LogseqOutStr.cardTask(recordRet.record.state), 0)
            .blockRef(recordRet.record.name)
            .blockRef(LogseqOutStr.date(recordRet.record.createTime));
        if (recordRet.record.labels != null) {
            for (String s : recordRet.record.labels.split(",")) {
                out.tag(s);
            }
        }
        out.breakLine();
        for (Comment comment : recordRet.comments) {
            out
                .block(comment.content(), 1)
                .blockRef(LogseqOutStr.date(comment.createTime()))
                .breakLine();
        };

        return out.toString();
    }

    public static String toPrintStr(List<ActionRecord> objects) {
        if (objects == null) {
            return "";
        }
        return objects.stream()
            .map(obj ->
                String.join(" | ",
                    obj.id(), obj.state(), obj.name(), Objects.toString(obj.remark(), ""),
                    Utils.formatDatetime(obj.createTime())
                )
            )
            .map(s -> s +"\n" + "-".repeat(s.length()))
            .collect(Collectors.joining("\n"));
    }

    @DefaultMapper
    public static ActionRecord from(Row row) {
        return new ActionRecord(
            getIfPresent(row, "ID", row::getString),
            getIfPresent(row, "NAME", row::getString),
            getIfPresent(row, "ICON", row::getString),
            getIfPresent(row, "INDEX", row::getString),
            getIfPresent(row, "DEVICE", row::getString),
            getIfPresent(row, "TYPE", row::getString),
            getIfPresent(row, "CATEGORY", row::getString),
            getIfPresent(row, "STATE", row::getString),
            getIfPresent(row, "START", row::getLocalDateTime),
            getIfPresent(row, "END", row::getLocalDateTime),
            getIfPresent(row, "DURATION", row::getInteger),
            getIfPresent(row, "RATING", row::getInteger),
            List.of(),
            getIfPresent(row, "LABELS", row::getString),
            getIfPresent(row, "REFS", row::getString),
            getIfPresent(row, "REMARK", row::getString),
            getIfPresent(row, "CREATE_TIME", row::getLocalDateTime),
            getIfPresent(row, "UPDATE_TIME", row::getLocalDateTime)
        );
    }

    @Override
    public ActionRecord beforeUpdate() {
        return new ActionRecord(id, name, icon,
            index, device, type, category, state, start, end,
            duration, rating, comments, labels, refs, remark,
            createTime, LocalDateTime.now());
    }

    @Override
    public ActionRecord beforeInsert() {
        return new ActionRecord(id, name, icon,
            index, device, type, category, state, start, end,
            duration, rating, comments, labels, refs, remark,
            createTime != null ? createTime : LocalDateTime.now() , LocalDateTime.now());
    }

    public static final String SYS_TEMPLATE = """
                    sys: actionRecord 变更记录
                    name: %s;
                    label: %s;
                    remark: %s;
                    state: %s;
                    category: %s;
                    start-end: %s - %s
                    """;
    public String generateSysMsg() {
        return String.format(SYS_TEMPLATE,
            name, labels, Objects.toString(remark, ""), state, category,
            Utils.formatDatetime(createTime), Utils.formatDatetime(updateTime)
        );
    }

    public record Records(
        ActionRecord record,
        List<ActionRecord> refs
    ) {}

    public record RecordRet(
        ActionRecord record,
        List<Comment> comments,
        List<CheckList> checklists,
        Object other
    ){

        public RecordRet(ActionRecord record, List<Comment> comments,  List<CheckList> checklists) {
            this(record, comments, checklists, null);
        }
    }
}
