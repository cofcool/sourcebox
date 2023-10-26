package net.cofcool.toolbox.gui;

import java.util.function.Consumer;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import net.cofcool.toolbox.Tool.Args;

public class GUIController {

    protected Consumer<EventArgs> runner;

    @FXML
    protected TextField output;

    protected Args defaultArgs() {
        return new Args();
    }

    public void setOutputContent(String s) {
        output.setText(s);
    }

    public void setActionNotify(Consumer<EventArgs> runner) {
        this.runner = runner;
    }

    protected void callRunner(Event event, Args args) {
        runner.accept(new EventArgs(event, args, this));
    }
}
