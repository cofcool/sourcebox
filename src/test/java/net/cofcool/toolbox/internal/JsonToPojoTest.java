package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.Tool;
import org.junit.jupiter.api.Test;

class JsonToPojoTest {

    @Test
    void run() throws Exception {
        new JsonToPojo().run(
                new Tool.Args()
                        .arg("json", """
                                {
                                    "str": "strVal",
                                    "obj": {
                                        "objKey0": "objVal0",
                                        "objKey1": "objVal1"
                                    },
                                    "arrObj": [
                                        {"arrObjKey0": "arrObjVal0"},
                                        {"arrObjKey1": "arrObjVal1"}
                                    ],
                                    "arrStr": ["arrStr1", "arrStr2", "arrStr2"]
                                }
                                """)
                        .arg("out", "./target/pojo")
                        .arg("pkg", "json.demo")
        );
    }
}