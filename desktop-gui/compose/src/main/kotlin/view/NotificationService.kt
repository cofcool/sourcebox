package view

import java.awt.*
import javax.swing.ImageIcon

class NotificationService {

    private var systemTray: SystemTray? = null
    private var trayIcon: TrayIcon? = null

    init {
        if (SystemTray.isSupported()) {
            systemTray = SystemTray.getSystemTray()
            val trayIconImage = ImageIcon(javaClass.getResource("/icons/icon.png")).image
            trayIcon = TrayIcon(trayIconImage, "Tomato Clock")
            trayIcon?.toolTip = "Tomato Clock"

            val popupMenu = PopupMenu()
            val exitItem = MenuItem("Exit")
            exitItem.addActionListener {
                System.exit(0)
            }
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

    fun removeNotification() {
        if (systemTray != null) {
            systemTray?.remove(trayIcon)
        }
    }
}