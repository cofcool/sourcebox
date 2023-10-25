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
    protected void runNow(MouseEvent event) {
        runner.accept(new EventArgs(event, new Args().arg("cmd", "now").arg("tool", ToolName.converts.name()), this));
    }

    @FXML
    protected void runMd5(MouseEvent event) {
        runner.accept(new EventArgs(event, new Args().arg("cmd", "md5").arg("tool", ToolName.converts.name()).arg("in", md5.getText()), this));
    }

}
