package view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import request.Tools

@Preview
@Composable
fun ContentView(currentTool: Tools) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topBar(currentTool.toolName(), true)
        grayDivider()
        Row {
            when (currentTool) {
                Tools.Helper -> commandHelper()
                Tools.HtmlDown  -> htmlDown()
                Tools.Note -> NoteView()
                Tools.C -> converts()
                Tools.Json -> JsonView()
                else -> {}
            }
        }
    }
}