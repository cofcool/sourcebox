package view

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.jetbrains.skiko.ClipboardManager

@Composable
fun clipboardView() {
    Text(ClipboardManager().getText()?:"aaa")

}