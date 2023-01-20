package net.cofcool.toolbox;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;


@SuppressWarnings("unchecked")
public class App {

    public static final boolean isWindows = System.getProperty("os.name").contains("Windows");

    private static final Set<Tool> ALL_TOOLS = new HashSet<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    static {
        try {
            var resource = App.class.getResource("/version");
            String versionPath = resource.getPath();
            var path  = new URL(resource.getProtocol() + (resource.getProtocol().equals("jar") ? ":" : "://") + versionPath.substring(0, versionPath.length() - 7));
            var type = path.getProtocol();
            String root = path.getFile();
            if (type.equals("file")) {
                var connection = path.openConnection();
                connection.connect();
                var dirs = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
                for (String dir : dirs.split("\n")) {
                    File file = new File(root + dir);
                    if (file.isDirectory()) {
                        for (File subFile : FileUtils.listFiles(file, new String[] {"class"}, true)) {
                            cacheClass(subFile.getPath().substring(root.length() - (isWindows ? 1: 0)).replace(File.separator, "."));
                        }
                    }
                }
            } else if (type.equals("jar")) {
                try (var file = new JarFile(new File(root.substring(5, root.length() - 2)))) {
                    file.stream()
                        .filter(a -> a.getRealName().endsWith(".class"))
                        .forEach(j -> cacheClass(j.getRealName().replace("/", ".")));
                }
            } else {
                throw new IllegalStateException("Do not support file type " + path);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void cacheClass(String name) {
        if (name.contains("module-info")) {
            return;
        }
        try {
            Class<?> aClass = App.class.getClassLoader().loadClass(name.substring(0, name.length() - 6));
            if (aClass != Tool.class && Tool.class.isAssignableFrom(aClass)) {
                Class<Tool>  type = (Class<Tool>) aClass;
                Constructor<Tool> constructor = type.getConstructor();
                ALL_TOOLS.add(constructor.newInstance());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {
        var pArgs = new Tool.Args(args);
        LoggerFactory.setDebug(pArgs.readArg("debug").filter(d -> "true".equalsIgnoreCase(d.val())).isPresent());

        LOGGER.info("Example: --name=demo --path=tmp");
        LOGGER.info("Tools: " + ALL_TOOLS.stream().map(Tool::name).toList());
        LOGGER.info("Args: ");
        LOGGER.info(pArgs);
        LOGGER.info("----------");
        pArgs.readArg("name").ifPresent(a -> {
            for (Tool tool : ALL_TOOLS) {
                if (tool.name() == ToolName.valueOf(a.val())) {
                    LOGGER.info("Start run " + tool.name());
                    try {
                        tool.run(pArgs);
                    } catch (Exception e) {
                        LOGGER.error(e);
                        LOGGER.info("Help:");
                        LOGGER.info(tool.help());
                    }
                }
            }
        });
    }
}
