package view

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.slf4j.LoggerFactory

val viewLogger = LoggerFactory.getLogger("view")

@Composable
fun grayDivider() {
    Row(modifier = Modifier.padding(2.dp)) {
        Divider(
            color = Color.LightGray,
            modifier = Modifier.fillMaxWidth().height(1.dp)
        )
    }
}

@Composable
fun topBar(tool: String) {
    Row(
        modifier = Modifier.height(Dp(30F)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopAppBar(
            title = { Text(tool, color = Color.White) },
            backgroundColor = Color.DarkGray,
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}