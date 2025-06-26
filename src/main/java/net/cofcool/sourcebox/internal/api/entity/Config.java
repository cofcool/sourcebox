package net.cofcool.sourcebox.internal.api.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;

@Entity(name = "config")
public record Config(
    @ID
    @Column(name = "name", type = JDBCType.VARCHAR, length = 50)
    String name,
    @Column(name = "data", type = JDBCType.VARCHAR, length = 1000)
    String data,
    @Column(name = "time", type = JDBCType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime time
) {

}
