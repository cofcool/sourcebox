package net.cofcool.toolbox.gui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.ToolName;

public class ConvertsController extends GUIController {

    @FXML
    protected TextField md5;
    @FXML
    protected TextField hdate;
    @FXML
    protected TextField timesp;

    @FXML
    protected void runNow(MouseEvent event) {
        callRunner(event, defaultArgs("now"));
    }

    @FXML
    protected void runHdate(MouseEvent event) {
        callRunner(event, defaultArgs("hdate").arg("in", hdate.getText()));
    }

    @FXML
    protected void runMd5(MouseEvent event) {
        callRunner(event, defaultArgs("md5").arg("in", md5.getText()));
    }

    @FXML
    protected void runTimesp(MouseEvent event) {
        callRunner(event, defaultArgs("timesp").arg("in", timesp.getText()));
    }

    protected Args defaultArgs(String cmd) {
        return super.defaultArgs().arg("tool", ToolName.converts.name()).arg("cmd", cmd);
    }
}
