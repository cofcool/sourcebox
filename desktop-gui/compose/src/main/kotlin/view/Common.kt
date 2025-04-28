package view

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import globalJson
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import request.Tools
import java.io.File

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

val currentParams = mutableStateMapOf<Tools, Map<String, String>>()

fun addParams(tools: Tools, map: Map<String, String>) {
    currentParams[tools] = map
}

fun readParams(tools: Tools, key: String, default: String = "") :String {
    val m = currentParams[tools]
    if (m != null) {
        return m.getOrDefault(key, default)
    }
    return ""
}

@Composable
fun topBar(tool: String, save: Boolean = false) {
    Row(
        modifier = Modifier.height(Dp(35F)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopAppBar(
            title = { Text(tool, color = Color.White) },
            backgroundColor = Color.DarkGray,
            actions = {
                if (save) {
                    Button(
                        onClick = { saveJsonToFile() },
                        modifier = Modifier.padding(2.dp, 1.dp)
                    ) {
                        Text("Save")
                    }
                    Button(
                        onClick = { loadJsonFromFile() },
                        modifier = Modifier.padding(2.dp, 1.dp)
                    ) {
                        Text("Load")
                    }
                }

            }
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

fun showMessage(msg: String) {
    viewLogger.info(msg)
}

private const val paramCfgPath = "/tmp/user.json"

fun saveJsonToFile() {
    try {
        val file = File(paramCfgPath)
        file.writeText(globalJson.encodeToString(currentParams.toMap()))
        showMessage("Save ok")
    } catch (e: Exception) {
        showMessage("Save error: ${e.message}")
    }
}

fun loadJsonFromFile() {
    try {
        val file = File(paramCfgPath)
        if (file.exists()) {
            val params: Map<Tools, Map<String, String>> = globalJson.decodeFromString(file.readText())
            currentParams.putAll(params)
        } else {
            showMessage("File not found!")
        }
    } catch (e: Exception) {
        showMessage("Failed to load file: ${e.message}")
    }
}