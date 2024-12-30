package net.cofcool.sourcebox.runner;

public record ActionEvent(Object source, String tool, String action, boolean success) {

    public ActionEvent(Object source, String tool, String action) {
        this(source, tool, action, true);
    }
}
