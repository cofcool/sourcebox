import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import request.Request
import view.ContentView
import view.ToolsView


val G_REQUEST = Request()

@Composable
@Preview
fun App() {

    MaterialTheme {
        Row {
            ToolsView()
            ContentView()
        }

    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
