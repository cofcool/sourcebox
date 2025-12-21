package view

import G_REQUEST
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import request.Tools

@Preview
@Composable
fun converts() {
    val inputText = remember { mutableStateOf("") }
    val outputText = remember { mutableStateOf("") }
    val selectedOperations = remember { mutableStateListOf<Tools>() }

    val replaceInputN = remember { mutableStateOf("") }
    val replaceInputO = remember { mutableStateOf("") }

    val urlInput = remember { mutableStateOf("") }

    val base64Input = remember { mutableStateOf("") }

    val extendParams = remember { mutableMapOf<String, String>() }

    fun reset() {
        inputText.value = ""
        outputText.value = ""
        replaceInputO.value = ""
        replaceInputN.value = ""
        urlInput.value = ""
        base64Input.value = ""
        selectedOperations.clear()
        extendParams.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = inputText.value,
            onValueChange = { inputText.value = it },
            label = { Text("Input") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                Row {
                    checkboxView(selectedOperations, Tools.C_MD5)
                    checkboxView(selectedOperations, Tools.C_NOW)
                    checkboxView(selectedOperations, Tools.C_HDATE)
                    checkboxView(selectedOperations, Tools.C_TIMESP)
                    checkboxView(selectedOperations, Tools.C_UPPER)
                    checkboxView(selectedOperations, Tools.C_LOWER)
                }
                Row {
                    checkboxView(selectedOperations, Tools.C_REPLACE)
                    TextField(
                        value = replaceInputN.value,
                        onValueChange = {
                            replaceInputN.value = it
                            extendParams["new"] = it
                        },
                        label = { Text("replace: new") },
                        modifier = Modifier.padding(all = 3.dp)
                    )
                    TextField(
                        value = replaceInputO.value,
                        onValueChange = {
                            replaceInputO.value = it
                            extendParams["old"] = it
                        },
                        label = { Text("replace: old") },
                        modifier = Modifier.padding(all = 3.dp)
                    )
                }
                Row {
                    checkboxView(selectedOperations, Tools.C_URLENCODE)
                    TextField(
                        value = urlInput.value,
                        onValueChange = {
                            urlInput.value = it
                            extendParams["utype"] = it
                        },
                        label = { Text("url encode: en/de") },
                        modifier = Modifier.fillMaxWidth().padding(all = 3.dp)
                    )
                }
                Row {
                    checkboxView(selectedOperations, Tools.C_BASE64)
                    TextField(
                        value = base64Input.value,
                        onValueChange = {
                            base64Input.value = it
                            extendParams["btype"] = it
                        },
                        label = { Text("base64 encode: en/de") },
                        modifier = Modifier.fillMaxWidth().padding(all = 3.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        grayDivider()
        Row {
            Button(
                onClick = {
                    transformText(
                        selectedOperations,
                        extendParams,
                        inputText.value,
                        outputText
                    )
                },
                modifier = Modifier.padding(8.dp),
                enabled = !selectedOperations.isEmpty()
            ) {
                Text("Convert")
            }
            Button(onClick = { reset() }, modifier = Modifier.padding(8.dp)) {
                Text("Reset")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        grayDivider()
        BasicTextField(
            value = outputText.value,
            onValueChange = {},
            readOnly = true,
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 18.sp)
        )
    }
}

@Composable
fun checkboxView(
    selectedOperations: SnapshotStateList<Tools>,
    operation: Tools,
    onCheckedChange: ((Boolean) -> Unit) = {}
) {
    Column {
        Row {
            Checkbox(
                checked = selectedOperations.contains(operation),
                onCheckedChange = { checked ->
                    if (checked) {
                        selectedOperations.add(operation)
                    } else {
                        selectedOperations.remove(operation)
                    }
                    onCheckedChange(checked)
                }
            )
            Text(operation.alias, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

fun transformText(
    ops: List<Tools>,
    ep: Map<String, String>,
    input: String,
    out: MutableState<String>
) {
    G_REQUEST.runSubTool(
        ops[0],
        mutableMapOf(
            "in" to input,
            "pipeline" to if (ops.size == 1) "" else ops.subList(1, ops.size)
                .joinToString(" | ") { it.cmd },
        ).apply { putAll(ep) }
    )
    G_REQUEST.readEvents { e, j ->
        out.value = e.source
    }
}