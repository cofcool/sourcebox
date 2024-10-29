import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import view.ContentView
import view.ToolsView

@Composable
@Preview
fun App() {
    var tool by remember { mutableStateOf("tool") }

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
