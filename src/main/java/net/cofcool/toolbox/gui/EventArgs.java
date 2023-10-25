package net.cofcool.toolbox.gui;

import javafx.event.Event;
import net.cofcool.toolbox.Tool.Args;

public record EventArgs(
    Event event,
    Args args,

    GUIController controller
) {

}
