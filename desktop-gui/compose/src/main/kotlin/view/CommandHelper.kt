package view

import G_REQUEST
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import request.CommandItem
import request.Tools

@Preview
@Composable
fun commandHelper() {
    val itemList = remember { mutableStateListOf<CommandItem>() }
    val searchQuery = remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            TextField(
                value = searchQuery.value,
                onValueChange = { query ->
                    searchQuery.value = query
                    searchCommand(itemList, query.text, null)
                },
                label = { Text("Search ID") },
                modifier = Modifier.padding(bottom = 5.dp)
            )
            TextField(
                value = searchQuery.value,
                onValueChange = { query ->
                    searchQuery.value = query
                    searchCommand(itemList, null, query.text)
                },
                label = { Text("Search tag") },
                modifier = Modifier.padding(bottom = 5.dp)
            )
            Button(
                onClick = { searchCommand(itemList, null, null) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Search")
            }
        }

        LazyColumn {
            item { cmdHeader() }

            items(itemList.size) { i ->
                commandItem(itemList[i])
            }
        }

    }
}

@Composable
fun cmdHeader() {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("ID", style = typography.subtitle1, modifier = Modifier.weight(1f))
        Text("Command", style = typography.subtitle1, modifier = Modifier.weight(2f))
        Text("Tags", style = typography.subtitle1, modifier = Modifier.weight(2f))
    }
}

fun searchCommand(items: MutableList<CommandItem>, id: String?, tag: String?) {
    G_REQUEST.runTool(Tools.Helper, mapOf<String, String>())

    G_REQUEST.readEvents({ -> items.clear() }) { a, j ->
        items.addAll(j.decodeFromString<List<CommandItem>>(a.source))
    }
}

@Composable
@Preview
fun commandItem(cmd: CommandItem) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(cmd.id, modifier = Modifier.weight(1f))
        Text(cmd.cmd, modifier = Modifier.weight(2f))
        cmd.tags?.joinToString(",")?.let { Text(it, modifier = Modifier.weight(2f)) }
    }
}