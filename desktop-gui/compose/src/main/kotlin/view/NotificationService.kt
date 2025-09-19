package view

import java.awt.*
import javax.swing.ImageIcon
import kotlin.system.exitProcess

class NotificationService {

    private var systemTray: SystemTray? = null
    private var trayIcon: TrayIcon? = null

    init {
        if (SystemTray.isSupported()) {
            systemTray = SystemTray.getSystemTray()
            val trayIconImage = ImageIcon(javaClass.getResource("/icons/icon.png")).image
            trayIcon = TrayIcon(trayIconImage, "Timer")
            trayIcon?.toolTip = "Timer"

            val popupMenu = PopupMenu()
            val exitItem = MenuItem("Exit")
            exitItem.addActionListener {
                exitProcess(0)
            }
            val msgItem = MenuItem("Work Time: 0s")
            msgItem.isEnabled = false

            popupMenu.add(msgItem)
            popupMenu.add(exitItem)

            trayIcon?.popupMenu = popupMenu

            try {
                systemTray?.add(trayIcon)
            } catch (e: AWTException) {
                e.printStackTrace()
            }
        } else {
            println("SystemTray is not supported on this system.")
        }
    }

    fun showNotification(title: String, message: String) {
        trayIcon?.displayMessage(title, message, TrayIcon.MessageType.INFO)
    }

    fun updateMenuMsg(message: String) {
        trayIcon?.popupMenu?.getItem(0)?.label = message
    }

    fun removeNotification() {
        systemTray?.remove(trayIcon)
    }

}