package net.cofcool.sourcebox.internal;

import java.io.ByteArrayInputStream;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import org.junit.jupiter.api.Test;

public class JsonToPojoTest extends BaseTest {

    public static final String JSON_STR = """
        {
            "str": "strVal",
            "obj": {
                "objKey0": 12313,
                "objKey1": "objVal1"
            },
            "arrObj": [
                {"arrObjKey0": "arrObjVal0", "arrObjKey01": 573, "arrObjKey02": false, "arrObjKey03": "asadasd"},
                {"arrObjKey1": "arrObjVal1", "arrObjKey11": 141, "arrObjKey12": true}
            ],
            "boolStr": true,
            "arrStr": ["arrStr1", "arrStr2", "arrStr3", ""],
            "arrNum": [123, 321, 111]
        }
        """;

    protected void init() {
        System.setProperty("logging.debug", "true");
    }

    @Override
    protected Tool instance() {
        return new JsonToPojo();
    }

    @Test
    void run() throws Exception {
        instance().run(
            args
                .arg("json", JSON_STR)
                .arg("out", "./target/pojo/run")
                .arg("pkg", "json.demo")
        );
    }

    @Test
    void runWithInput() throws Exception {
        System.setIn(new ByteArrayInputStream(JSON_STR.getBytes()));
        instance().run(
            args
                .arg("out", "./target/pojo/runWithInput")
                .arg("pkg", "json.demo")
        );
    }

    @Test
    void runToClass() throws Exception {
        instance().run(
            args
                .arg("json", JSON_STR)
                .arg("ver", JsonToPojo.Lang.JAVA_CLASS.getVer())
                .arg("out", "./target/pojo/runToClass")
                .arg("pkg", "json.demo")
        );
    }
}