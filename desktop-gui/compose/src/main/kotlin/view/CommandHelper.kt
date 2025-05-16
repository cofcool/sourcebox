package view

import G_REQUEST
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import request.CommandItem
import request.Tools

@Preview
@Composable
fun commandHelper() {
    val itemList = remember { mutableStateListOf<CommandItem>() }
    val tagQuery = remember { mutableStateOf(TextFieldValue("")) }
    val idQuery = remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            TextField(
                value = idQuery.value,
                onValueChange = { query ->
                    idQuery.value = query
                },
                label = { Text("Search ID") },
                modifier = Modifier.padding(bottom = 5.dp)
            )
            Spacer(modifier = Modifier.padding(2.dp))
            TextField(
                value = tagQuery.value,
                onValueChange = { query ->
                    tagQuery.value = query
                },
                label = { Text("Search tag") },
                modifier = Modifier.padding(bottom = 5.dp)
            )
            Button(
                onClick = {
                    val id = idQuery.value.text
                    val tag = tagQuery.value.text
                    var s = ""
                    if (id.isNotEmpty()) {
                        s += "@${id} "
                    }
                    if (tag.isNotEmpty()) {
                        s += "#${tag} "
                    }
                    searchCommand(itemList, s)
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Search")
            }
            Button(
                onClick = {
                    storeCommand()
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Store")
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

fun storeCommand() {
    G_REQUEST.runTool(Tools.Helper, mapOf(
        "store" to "ALL"
    ))
    G_REQUEST.readEvents({},{ _, j ->})
}

fun editCommand(item: CommandItem) {
    G_REQUEST.runTool(Tools.Helper, mapOf(
        "add" to "${item.id} ${item.cmd} ${item.tags?.joinToString(separator = " ") }"
    ))
    G_REQUEST.readEvents({},{ _, j ->})
}

fun searchCommand(items: MutableList<CommandItem>, query: String) {
    val q = query.takeIf { it.isNotEmpty() }?.let { mapOf("find" to query) }?: mapOf()
    G_REQUEST.runTool(Tools.Helper, q)

    G_REQUEST.readEvents({ -> items.clear() }) { a, j ->
        items.addAll(j.decodeFromString<List<CommandItem>>(a.source))
    }
}

@Composable
@Preview
fun commandItem(cmd: CommandItem) {
    var isEditing by remember { mutableStateOf(false) }

    var id by remember { mutableStateOf(cmd.id) }
    var commandText by remember { mutableStateOf(cmd.cmd) }
    var tagsText by remember { mutableStateOf(cmd.tags?.joinToString(",") ?: "") }
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextField(
            value = id,
            onValueChange = { id = it },
            modifier = Modifier.weight(1f),
            enabled = isEditing,
            readOnly = !isEditing,
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                disabledTextColor = LocalContentColor.current,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Blue,
                backgroundColor = Color.Transparent
            )
        )
        TextField(
            value = commandText,
            onValueChange = { commandText = it },
            modifier = Modifier.weight(2f),
            enabled = isEditing,
            readOnly = !isEditing,
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                disabledTextColor = LocalContentColor.current,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Blue,
                backgroundColor = Color.Transparent
            )
        )

        TextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            modifier = Modifier.weight(2f),
            enabled = isEditing,
            readOnly = !isEditing,
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                disabledTextColor = LocalContentColor.current,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Blue,
                backgroundColor = Color.Transparent
            )
        )
        Button(
            onClick = {
                if (isEditing) {
                    editCommand(
                        CommandItem(
                            id = id,
                            cmd = commandText,
                            tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                    )
                }
                isEditing = !isEditing
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(if (isEditing) "Save" else "Edit")
        }
    }
    grayDivider()
}