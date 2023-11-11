package net.cofcool.toolbox.runner;

import java.util.function.Consumer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.CustomLog;
import net.cofcool.toolbox.App;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import net.cofcool.toolbox.ToolContext;
import net.cofcool.toolbox.ToolRunner;
import net.cofcool.toolbox.WebTool;
import net.cofcool.toolbox.gui.EventArgs;
import net.cofcool.toolbox.gui.GUIController;
import net.cofcool.toolbox.util.Utils;

@CustomLog
public class GUIRunner extends Application implements ToolRunner {

    private static Args GLOBAL_ARGS = new Args();


    @Override
    public boolean run(Args args) throws Exception {
        GUIRunner.GLOBAL_ARGS = args;
        App.getRunner(RunnerType.WEB).run(args);
        launch();
        return true;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(c -> {
            var cs = Utils.instance(c);
            if (GUIController.class.isAssignableFrom(c)) {
                var controller = (GUIController) cs;
                controller.setActionNotify(new ActionNotify());
            }

            return cs;
        });

        VBox main = loader.load();

        Scene scene = new Scene(new StackPane(main), 640, 480);
        primaryStage.setScene(scene);
        primaryStage.setTitle(App.ABOUT);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void runTool(EventArgs event) {
        var name = event.args().readArg("tool").val();
        log.debug("Run {0}", name);
        App.supportTools(RunnerType.GUI)
            .stream()
            .filter(t -> t.name().name().equals(name))
            .filter(t -> !(t instanceof WebTool))
            .forEach(t -> {
                try {
                    t.run(new Args().copyConfigFrom(event.args()).copyConfigFrom(GLOBAL_ARGS)
                        .copyConfigFrom(t.config()).context(new GUIContext(event.controller())));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private class ActionNotify implements Consumer<EventArgs> {

        @Override
        public void accept(EventArgs eventArgs) {
            runTool(eventArgs);
        }
    }

    private record GUIContext(GUIController controller) implements ToolContext {

        @Override
            public ToolContext write(String name, String in) {
                controller.setOutputContent(in);
                return this;
            }

            @Override
            public RunnerType runnerType() {
                return RunnerType.GUI;
            }
        }

}
