package net.cofcool.toolbox.gui;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.InputEvent;
import net.cofcool.toolbox.internal.simplenote.entity.Note;
import net.cofcool.toolbox.runner.WebRunner;
import net.cofcool.toolbox.util.JsonUtil;

public class NoteController extends GUIController {

    private final WebClient client = WebClient.create(
        Vertx.vertx(),
        new WebClientOptions()
            .setDefaultPort(WebRunner.PORT_VAL)
            .setLocalAddress("127.0.0.1")
    );

    @FXML
    protected TableView<Note> noteTableView;

    @FXML
    protected ObservableList<Note> noteData;


    @FXML
    protected void showNote(InputEvent event) {

    }


    @FXML
    public void initialize() {
        client.get("/note/list").send().onSuccess(r -> {
            var resp = JsonUtil.toPojoList(r.bodyAsBuffer().getBytes(), Note.class);
            noteData.addAll(resp);
        } );
        noteTableView.setItems(noteData);
        noteTableView.setRowFactory(t -> {
            var r = new TableRow<Note>();
//            t.getItems()
            r.setText("sadasdas");

            return r;
        });
    }
}
