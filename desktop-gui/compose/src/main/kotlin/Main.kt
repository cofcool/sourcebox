import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import request.Request
import request.Tools
import view.ContentView
import view.ToolsView
import view.viewLogger
import kotlin.system.exitProcess


val G_REQUEST = Request()
var W_REF : ComposeWindow? = null

@Composable
@Preview
fun App() {
    var currentTool by remember { mutableStateOf(Tools.None) }
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
    Window(
        onCloseRequest = {
            W_REF?.isVisible = false
        },
        title = "The Source Box"
    ) {
        App()
       W_REF = this.window
    }
    G_REQUEST.checkEvent()
}

fun loadServer() {
    System.setProperty("logging.type", "net.cofcool.sourcebox.logging.JULLogger")
    try {
        val clazz = Class.forName("net.cofcool.sourcebox.App")
        val method = clazz.getDeclaredMethod("main", Array<String>::class.java)
        method.invoke(null, arrayOf<String>("--mode=GUI"))
        viewLogger.info("Start server ok")
    } catch (e: Exception) {
        viewLogger.error("Run server error", e)
        exitProcess(1)
    }
}
