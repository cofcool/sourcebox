package view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun grayDivider() {
    Row(modifier = Modifier.padding(2.dp)) {
        Divider(
            color = Color.LightGray,
            modifier = Modifier.fillMaxWidth().height(1.dp)
        )
    }
}