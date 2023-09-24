import static java.lang.StringTemplate.STR;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public record DemoTemplate() {

       
        public Map<String,String> generate(Map<String, Object> config) {
            var ret = new HashMap<String, String>();
            for (String s : (List<String>)config.get("entities")) {
                ret.put(
                    s + "Service",
                    STR. """
                    package \{ config.get("pkg") };

                    import org.springframework.web.bind.annotation.RequestMapping;
                    import org.springframework.web.bind.annotation.RestController;

                    @RestController
                    @RequestMapping(value = "/\{ s.toLowerCase() }")
                    public class \{ s }Service {

                    }
                    """
                );
            }

            return ret;
        }
    }