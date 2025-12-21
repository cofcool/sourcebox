package view

import G_REQUEST
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import now
import request.Note

val defaultNote = Note("", "", LocalDateTime.now(), "")

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NoteView() {
    val currentNote = remember { mutableStateOf<Note>(defaultNote) }
    val notesList = remember { mutableStateListOf<Note>() }
    LaunchedEffect(true) {
        refreshNoteList(notesList)
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        BasicTextField(
            value = currentNote.value.content,
            onValueChange = { currentNote.value = Note("", it, LocalDateTime.now(), "") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(8.dp)
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.5f)),
            singleLine = false,
            textStyle = TextStyle(fontSize = 20.sp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    G_REQUEST.saveNote(currentNote.value)
                    refreshNoteList(notesList)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Note")
            }
            Button(
                onClick = { currentNote.value = defaultNote },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
            Button(
                onClick = {
                    G_REQUEST.deleteNote(currentNote.value)
                    refreshNoteList(notesList)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Saved Notes:", style = MaterialTheme.typography.h6)
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            items(notesList.size) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = 4.dp,
                    onClick = { currentNote.value = notesList[index] }
                ) {
                    Text(
                        text = notesList[index].content,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

fun refreshNoteList(items: MutableList<Note>) {
    items.clear()
    items.addAll(G_REQUEST.listNote(Note("", "", null, "NORMAL")))
}