package net.cofcool.sourcebox.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;


public class LinkCovertTool implements Tool {

    private static final String FILE_TYPE_LINUX = ".desktop";
    private static final String FILE_TYPE_OSX = ".webloc";


    public static void covert(String inputPath, String outputPath)    {
        List<File> files = new ArrayList<>();

        File inputFile = new File(inputPath);
        if (inputFile.isDirectory()) {
            files = Arrays.asList(inputFile.listFiles());
        }else {
            files.add(inputFile);
        }

        for (File file : files) {

            if (!file.getName().startsWith(".")) {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    String content = null;
                    while ((content = bufferedReader.readLine()) != null) {
                        if (file.getName().endsWith(FILE_TYPE_LINUX)) {
                            String reg = "http.+";
                            outputFile(outputPath, content, file, reg);
                        } else if (file.getName().endsWith(FILE_TYPE_OSX)) {
                            String reg = "(http(.+)</s)";
                            outputFile(outputPath, content, file, reg);
                        }
                    }

                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void outputFile(String outputPath, String content, File file, String reg)   {
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(content);
        int line = 0;
        while (matcher.find())  {
            String result = matcher.group(line).replace("</s", "");
            line++;

            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath, true));
                String fileName = file.getName();

                bufferedWriter.write("* [" + fileName.substring(0, fileName.lastIndexOf(".")) + "]" + "(" + result + ")" + "\n");
                bufferedWriter.flush();
            }catch (IOException e)  {
                e.printStackTrace();
            }

        }
    }

    @Override
    public ToolName name() {
        return ToolName.link2Tool;
    }

    @Override
    public void run(Args args) throws Exception {
        covert(args.readArg("input").val(), args.readArg("output").val());
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("input", null, "link file path", true, "demo.desktop"))
            .arg(new Arg("output", null, "out file path", true, "demo.md"));
    }
}
