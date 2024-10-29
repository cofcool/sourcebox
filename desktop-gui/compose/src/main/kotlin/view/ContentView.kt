package view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun ContentView() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.height(Dp(30F)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contents")
        }
        Row(modifier = Modifier.padding(2.dp)) {
            Divider(
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth().height(1.dp)
            )
        }
        Row {
            Text("Content")
        }
    }
}