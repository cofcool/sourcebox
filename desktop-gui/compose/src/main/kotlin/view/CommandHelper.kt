package view

import G_REQUEST
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import request.CommandItem

@Preview
@Composable
fun commandHelper() {
    val itemList = remember { mutableStateListOf<CommandItem>() }
    val query = remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            TextField(
                value = query.value,
                onValueChange = {
                    query.value = it
                },
                label = { Text("Search") },
                modifier = Modifier.padding(bottom = 5.dp).weight(1f)
            )

        }
        Row {
            Button(
                onClick = {
                    searchCommand(itemList, query.value.text)
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
                commandItem(itemList[i]) {
                    if (it) {
                        itemList.removeAt(i)
                    }
                }
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
    G_REQUEST.storeCmd()
}

fun delCommand(id: String) {
    G_REQUEST.deleteCmd(id)
}

fun editCommand(item: CommandItem) {
    G_REQUEST.runTool("cmd", item)
}

fun searchCommand(items: MutableList<CommandItem>, query: String) {
    items.clear()
    items.addAll(G_REQUEST.listCmd(query))
}

@Composable
@Preview
fun commandItem(cmd: CommandItem, action: (Boolean) -> Unit) {
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
        Button(
            onClick = {
                delCommand(cmd.id)
                action(true)
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Delete")
        }
    }
    grayDivider()
}