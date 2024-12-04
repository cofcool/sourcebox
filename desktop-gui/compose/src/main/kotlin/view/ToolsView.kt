package view

import G_REQUEST
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("tools")

@Composable
@Preview
fun ToolsView() {
    var tools by remember  { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(true) {
        scope.launch {
            tools = try {
                G_REQUEST.getAllTools()
            } catch (e: Exception) {
                logger.error("tools error",e)
                listOf()
            }
        }
    }
    Column {
        Row {
            Row {
                Column(modifier = Modifier.width(150.dp)) {
                    Row {
                        Text(
                            "Tools",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(Dp(30F)).fillMaxWidth()
                        )

                    }
                    grayDivider()
                    Row {
                        LazyColumn {
                            items(tools.size) {
                                val d = tools[it % tools.size]
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(onClick = { println("$it") }) {
                                        Text(d)
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