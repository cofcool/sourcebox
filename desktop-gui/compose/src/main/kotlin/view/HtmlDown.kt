package view

import G_REQUEST
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import request.Tools

@Preview
@Composable
fun htmlDown() {
    val itemList = remember { mutableStateListOf<String>() }
    val proxy = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "proxy"))) }
    val clean = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "clean","true"))) }
    val out = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "out","/tmp/html"))) }
    val filter = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "filter"))) }
    val depth = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "depth","1"))) }
    val url = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "url"))) }
    val cleanexp = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "cleanexp"))) }
    val webDriver = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "webDriver","/usr/local/bin/chromedriver"))) }
    val hrefFilter = remember { mutableStateOf(TextFieldValue(readParams(Tools.HtmlDown, "hrefFilter"))) }

    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Row {
            TextField(
                value = url.value,
                onValueChange = { query ->
                    url.value = query
                },
                label = { Text("url") },
                modifier = Modifier.fillMaxWidth().padding(all = 5.dp)
            )
        }
        Row {
            Column {
                Row {
                    createTextField(depth, "depth")
                    createTextField(out, "out")
                }
                Row {
                    createTextField(proxy, "proxy")
                    createTextField(filter, "filter")
                }
                Row {
                    SelectionContainer {  }
                    createTextField(clean, "clean")
                    createTextField(cleanexp, "cleanexp")
                }
                Row {
                    createTextField(hrefFilter, "hrefFilter")
                    createTextField(webDriver, "webDriver")
                }
            }
        }
        Row {
            Button(
                onClick = {
                    download(
                        mapOf<String, String>(
                            "url" to url.value.text,
                            "proxy" to proxy.value.text,
                            "filter" to filter.value.text,
                            "depth" to depth.value.text,
                            "out" to out.value.text,
                            "clean" to clean.value.text,
                            "cleanexp" to cleanexp.value.text,
                            "webDriver" to webDriver.value.text,
                            "hrefFilter" to hrefFilter.value.text,
                            "debug" to "true"
                        ),
                        itemList
                    )
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Download")
            }
        }
        grayDivider()
        Row {
            LazyColumn {
                items(itemList.size) { i ->
                    Text(itemList[i])
                }
            }
        }
    }
}

@Composable
fun createTextField(textFieldValue: MutableState<TextFieldValue>, label: String) {
    TextField(
        value = textFieldValue.value,
        onValueChange = { query ->
            textFieldValue.value = query
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.padding(all = 3.dp)
    )
}

fun download(map: Map<String, String>, items: MutableList<String>) {
    addParams(Tools.HtmlDown, map)
    G_REQUEST.runTool(Tools.HtmlDown, map)
    G_REQUEST.readEvents({-> items.clear()}) { e, j ->
        items.add(e.source)
    }
}