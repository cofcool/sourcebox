package net.cofcool.toolbox.gui;

import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class GUIController {

    protected Consumer<EventArgs> runner;

    @FXML
    protected TextField output;


    public void setOutputContent(String s) {
        output.setText(s);
    }

    public void setActionNotify(Consumer<EventArgs> runner) {
        this.runner = runner;
    }
}
