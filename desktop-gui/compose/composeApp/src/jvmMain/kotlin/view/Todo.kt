package view

import G_REQUEST
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import formatFullStyle
import kotlinx.datetime.LocalDateTime
import now
import request.TodoItem


@Composable
@Preview
fun Todo() {
    val todos = remember { mutableStateListOf<TodoItem>() }
    var current by remember {
        mutableStateOf<TodoItem>(
            TodoItem(
                id = "",
                name = "",
                state = "todo",
                remark = null
            )
        )
    }

    LaunchedEffect(true) {
        refreshTodos(todos)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Column {
            TextField(
                value = current.name,
                onValueChange = {
                   current = current.copy(name = it)
                },
                label = { Text("Task name") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = current.remark ?: "",
                onValueChange = { current = current.copy(remark = it) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        if (current.name.isNotBlank()) {
                            addTodo(current.copy(state = "todo"))
                            current.name = ""
                            current.remark = ""
                        }
                    }) {
                    Text("Add")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        refreshTodos(todos, TodoItem("", current.name, "", null))
                        current.name = ""

                    }) {
                    Text("Search")
                }
            }
        }

        grayDivider()

        LazyColumn {
            items(todos.size) {
                todoItem(todos[it]) {
                    current = it.copy()
                }
            }
        }
    }
}

@Composable
fun todoItem(todo: TodoItem, action: (TodoItem) -> Unit) {
    var state by remember { mutableStateOf(todo.state == "done") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row {
            Column {
                Checkbox(
                    checked = state,
                    enabled = todo.state == "todo" || state,
                    onCheckedChange = {
                        doneTodo(todo)
                        state = true
                    }
                )
            }
            Column {
                Button(
                    modifier = Modifier.padding(4.dp),
                    onClick = {
                        action(todo)
                    }) {
                    Text("Edit")
                }
            }
            SelectionContainer {
                Column {
                    Text(
                        todo.name, style = MaterialTheme.typography.subtitle1,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (todo.remark?.isNotBlank() == true) {
                        Text(
                            todo.remark ?: "",
                            style = MaterialTheme.typography.body2.copy(
                                color = MaterialTheme.colors.onSurface.copy(
                                    alpha = 0.6f
                                )
                            )
                        )
                    }
                }
            }
        }
    }
    grayDivider()
}

fun refreshTodos(items: MutableList<TodoItem>, condition: TodoItem = TodoItem("", "", "", "")) {
    items.clear()
    items.addAll(G_REQUEST.listTodo(condition))
}

fun doneTodo(item: TodoItem) {
    G_REQUEST.runTool(
        "action", mapOf(
            "id" to item.id,
            "state" to "done"
        )
    )
}

fun addTodo(item: TodoItem) {
    val id = item.id.ifBlank {
        null
    }
    G_REQUEST.runTool(
        "action", mapOf(
            "id" to id,
            "name" to item.name,
            "remark" to item.remark,
            "state" to item.state,
            "type" to "todo",
            "start" to LocalDateTime.now().formatFullStyle(),
            "end" to LocalDateTime.now().formatFullStyle()
        )
    )
}