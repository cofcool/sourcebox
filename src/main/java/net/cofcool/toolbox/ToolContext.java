package net.cofcool.toolbox;

import java.util.Objects;
import net.cofcool.toolbox.Tool.RunnerType;
import org.apache.commons.lang3.RandomStringUtils;

public interface ToolContext {

    default ToolContext write(Object val) {
        return write(RandomStringUtils.randomAlphabetic(6), Objects.toString(val, ""));
    }

    ToolContext write(String name, String in);

    RunnerType runnerType();

}
