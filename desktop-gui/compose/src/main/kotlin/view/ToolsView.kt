package view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun ToolsView() {
    Column {
        Row {
            Row {
                Column(modifier = Modifier.width(120.dp)) {
                    Row {
                        Text(
                            "Tools",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                    }
                    Row {
                        LazyColumn {
                            items(5) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(onClick = { println("$it") }) {
                                        Text("Hello, $it")
                                    }
                                }
                            }
                        }
                    }
                }
                Column(modifier = Modifier.width(2.dp)) {
                    Divider(
                        color = Color.LightGray,
                        modifier = Modifier.fillMaxHeight().width(1.dp)
                    )
                }
            }
        }
    }
}