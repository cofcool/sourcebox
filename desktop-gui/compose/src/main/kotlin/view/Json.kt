package view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import globalJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Composable
fun JsonView() {
    var jsonString by remember { mutableStateOf("") }
    var jsonErrorMessage by remember { mutableStateOf<String?>(null) }
    var jsonObject by remember { mutableStateOf<JsonObject?>(null) }
    val isExpandedAll  = mutableStateOf(false)


    Column(modifier = Modifier.padding(16.dp)) {
        Text("Enter JSON String:")
        TextField(
            value = jsonString,
            onValueChange = {
                jsonString = it
                
                try {
                    jsonObject = globalJson.decodeFromString(jsonString)
                    jsonErrorMessage = null
                } catch (e: Exception) {
                    jsonObject = null
                    jsonErrorMessage = "Invalid JSON format: ${e.localizedMessage}"
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).height(200.dp),
            placeholder = { Text("Enter JSON here...") }
        )

        jsonErrorMessage?.let {
            Text(text = it, color = MaterialTheme.colors.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { isExpandedAll.value = true }) {
                Text("Expand All")
            }
            Button(onClick = { isExpandedAll.value = false }) {
                Text("Collapse All")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        
        jsonObject?.let {
            ExpandableJsonTreeView(json = it, isExpandedAll)
        }
    }
}

@Composable
fun ExpandableJsonTreeView(json: JsonObject, isExpandedAll: MutableState<Boolean>) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        RenderJsonTree(json, level = 0, isExpandedAll)
    }
}

@Composable
fun RenderJsonTree(json: JsonObject, level: Int, isExpandedAll: MutableState<Boolean>) {
    json.keys.forEach {
        JsonTreeItem(it, json[it]!!, level, isExpandedAll)
    }
}

@Composable
fun JsonTreeItem(key: String, value: Any, level: Int, isExpandedAll: MutableState<Boolean>) {
    var isExpanded by remember { mutableStateOf(isExpandedAll.value)}
    val indentation = "  ".repeat(level)

    Column(modifier = Modifier.padding(start = 16.dp * level)) {
        if (value is JsonObject || value is JsonArray) {
            Row {
                Button(onClick = { isExpanded = !isExpanded }) {
                    Text(text = if (isExpanded) "[-]" else "[+]")
                }
                Text(text = "$indentation$key:")
            }
            
            if (isExpanded) {
                when (value) {
                    is JsonObject -> RenderJsonTree(
                        value,
                        level + 1,
                        isExpandedAll
                    ) 
                    is JsonArray -> RenderJsonArray(value, level + 1, isExpandedAll)
                }
            }
        } else {
            Row {
                Text(text = "$indentation$key: $value")
            }
        }
    }
}

@Composable
fun RenderJsonArray(array: JsonArray, level: Int, isExpandedAll: MutableState<Boolean>) {
    array.forEachIndexed { index, item ->
        val indentation = "  ".repeat(level)
        Column(modifier = Modifier.padding(start = 16.dp * level)) {
            Row {
                Text(text = "$indentation[Index $index]: ")
            }

            
            when (item) {
                is JsonObject -> RenderJsonTree(item, level + 1, isExpandedAll) 
                is JsonArray -> RenderJsonArray(item, level + 1, isExpandedAll) 
                else -> Row { Text(text = "$indentation$item") } 
            }
        }
    }
}