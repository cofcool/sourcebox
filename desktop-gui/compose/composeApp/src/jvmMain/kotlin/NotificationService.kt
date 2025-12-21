import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.*
import org.jetbrains.compose.resources.painterResource
import sourcebox.composeapp.generated.resources.Res
import sourcebox.composeapp.generated.resources.tray_icon
import java.awt.Frame
import kotlin.system.exitProcess


@Composable
fun ApplicationScope.SystemTrayView(ns: NotificationService) {
    val trayState = rememberTrayState()
    val workMsg =  mutableStateOf("Work Time: 0s")
    ns.state = trayState
    ns.stateMsg = workMsg

    Tray(
        state = trayState,
        icon = painterResource(Res.drawable.tray_icon)
    ) {
        Item(
            "Show",
            onClick = {
                if (W_REF?.state == Frame.ICONIFIED) {
                    W_REF?.state = Frame.NORMAL
                }
                W_REF?.isVisible = true
                W_REF?.toFront()
                W_REF?.requestFocus()
            }
        )
        Item(
            workMsg.value,
            enabled = false,
            onClick = {}
        )
        Item(
            "Exit",
            onClick = {
                exitProcess(0)
            }
        )
    }
}

class NotificationService {

    lateinit var state: TrayState
    lateinit var stateMsg : MutableState<String>

    fun showNotification(title: String, message: String) {
        state.sendNotification(Notification(title, message, Notification.Type.Info))
    }

    fun updateMenuMsg(message: String) {
        stateMsg.value = message
    }

}