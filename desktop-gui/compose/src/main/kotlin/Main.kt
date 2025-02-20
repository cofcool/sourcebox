import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import request.Request
import view.ContentView
import view.ToolsView
import view.viewLogger
import kotlin.system.exitProcess


val G_REQUEST = Request()

@Composable
@Preview
fun App() {
    var currentTool by remember { mutableStateOf("") }
    MaterialTheme {
        Row {
            ToolsView {
                currentTool = it
            }
            ContentView(currentTool)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "The Source Box") {
        App()
    }
    loadServer()
    G_REQUEST.checkEvent()
}

fun loadServer() {
    try {
        val clazz = Class.forName("net.cofcool.sourcebox.App")
        val method = clazz.getDeclaredMethod("main", Array<String>::class.java)
        method.invoke(null, arrayOf<String>("--mode=GUI"))
        viewLogger.info("Start server ok")
    } catch (e: Exception) {
        System.err.println(e)
        exitProcess(1)
    }
}
