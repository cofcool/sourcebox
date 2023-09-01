package net.cofcool.toolbox;

import java.util.Objects;
import net.cofcool.toolbox.Tool.RunnerType;
import org.apache.commons.lang3.RandomStringUtils;

public interface ToolContext {

    default ToolContext write(Object val) {
        return write(randomName(), Objects.toString(val, ""));
    }

    ToolContext write(String name, String in);

    RunnerType runnerType();

    static String randomName() {
        return RandomStringUtils.randomAlphabetic(6);
    }

}
