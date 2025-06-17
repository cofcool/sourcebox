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
    var todoText = remember { mutableStateOf("") }
    var todoNote = remember { mutableStateOf("") }
    var todos = remember { mutableStateListOf<TodoItem>() }

    LaunchedEffect(true) {
        refreshTodos(todos)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Column {
            TextField(
                value = todoText.value,
                onValueChange = { todoText.value = it },
                label = { Text("Task name") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = todoNote.value,
                onValueChange = { todoNote.value = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                    if (todoText.value.isNotBlank()) {
                        val item = TodoItem("", todoText.value, "todo", todoNote.value)
                        addTodo(item)
                        todos.add(item)
                        todoText.value = ""
                        todoNote.value = ""
                    }
                }) {
                    Text("Add")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        refreshTodos(todos, TodoItem("", todoText.value, "", null))
                        todoText.value = ""

                }) {
                    Text("Search")
                }
            }
        }

        grayDivider()

        LazyColumn {
            items(todos.size) {
                todoItem(todos[it])
            }
        }
    }
}

@Composable
fun todoItem(todo: TodoItem) {
    var state by remember { mutableStateOf(todo.state == "done") }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
            SelectionContainer {
                Column {
                    Text(
                        todo.name, style = MaterialTheme.typography.subtitle1,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (todo.remark != null && todo.remark.isNotBlank()) {
                        Text(
                            todo.remark,
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

fun refreshTodos(items: MutableList<TodoItem>, condition: TodoItem= TodoItem("", "", "", "")) {
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
    G_REQUEST.runTool(
        "action", mapOf(
            "name" to item.name,
            "remark" to item.remark,
            "state" to item.state,
            "type" to "todo",
            "start" to LocalDateTime.now().formatFullStyle(),
            "end" to LocalDateTime.now().formatFullStyle()
        )
    )
}