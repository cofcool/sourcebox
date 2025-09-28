package view

import ConfigManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import secondsDisplay
import java.awt.Toolkit

private val helper = TimerHelper()

class TimerHelper {
    val notificationService = NotificationService()
    val timerService = TimerService()
    var remainingTime = timerService.timeMsg()

    var isWorkingTime = true
    var isFullScreen = false

    var action:(r: String, isW: Boolean, isF: Boolean) -> Unit = fun(_,_,_){}

    init {
        timerService.onTimeUpdated = fun(i1: Long, i2: String) {
            remainingTime = i2
            notificationService.updateMenuMsg(i2)
            if (i1 == 59L && isWorkingTime) {
                notificationService.showNotification("Timer", "Break time! Please relax.")
            }
            updateNotify()
        }
        timerService.onBreakTimeCompleted = {
            isWorkingTime = true
            updateNotify()
        }
        timerService.onBreakTimeStarted = {
            isWorkingTime = false
            isFullScreen = true
            updateNotify()
        }
    }

    fun updateNotify() {
        action(remainingTime, isWorkingTime, isFullScreen)
    }
}

@Composable
fun timerView() {
    TomatoClockApp(timerService = helper.timerService)
}

@Composable
fun TomatoClockApp(timerService: TimerService) {
    var remainingTime by remember { mutableStateOf(helper.remainingTime) }
    var isWorkingTime by remember { mutableStateOf(helper.isWorkingTime) }
    var isFullScreen by remember { mutableStateOf(helper.isFullScreen) }

    var workDurationInput by remember { mutableStateOf(TextFieldValue("${timerService.workMin()}")) }
    var breakDurationInput by remember { mutableStateOf(TextFieldValue("${timerService.breakMin()}")) }

    helper.action = { r: String, isW: Boolean, isF: Boolean ->
        remainingTime = r
        isWorkingTime = isW
        isFullScreen = isF
    }

    fun updateDurations() {
        val workMinutes = workDurationInput.text.toLong()
        val breakMinutes = breakDurationInput.text.toLong()
        helper.timerService.updateDurations(workMinutes, breakMinutes)
        remainingTime = helper.timerService.timeMsg(workMinutes * 60)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = remainingTime,
            style = MaterialTheme.typography.h4
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row {
            Text("Work Time (minutes):")
            BasicTextField(
                value = workDurationInput,
                onValueChange = { workDurationInput = it },
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row {
            Text("Break Time (minutes):")
            BasicTextField(
                value = breakDurationInput,
                onValueChange = { breakDurationInput = it },
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            updateDurations()
            ConfigManager.saveConfig {
                it.timerWorkDuration = workDurationInput.text.toLong()
                it.timerBreakDuration = breakDurationInput.text.toLong()

                it
            }
            timerService.startTimer()
        }) {
            Text("Start")
        }

        Button(onClick = {
            timerService.resetTimer()
        }) {
            Text("Reset")
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isFullScreen) {
            fullScreenWindow(isWorkingTime) {
                isFullScreen = false
                helper.isFullScreen = false
                timerService.startTimer()
            }
        }
    }
}

@Composable
fun fullScreenWindow(isButtonEnabled: Boolean, onClose: () -> Unit) {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val screenWidth = screenSize.width
    val screenHeight = screenSize.height

    val state = rememberWindowState(
        width = screenWidth.dp,
        height = screenHeight.dp,
        position = WindowPosition.Absolute(0.dp, 0.dp)
    )

    val backgroundColor = Color.Black.copy(alpha = 0.7f)

    Window(
        onCloseRequest = onClose,
        state = state,
        title = "Break Time",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClose,
                enabled = isButtonEnabled,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Break is over")
            }
        }
    }
}

class TimerService(
    var workDuration: Long = ConfigManager.config().timerWorkDurationSec(),
    var breakDuration: Long = ConfigManager.config().timerBreakDurationSec()
) {
    private var timeRemaining: Long = workDuration
    private var isWorkingTime: Boolean = true
    private var isReset = false

    var onTimeUpdated: (Long, String) -> Unit = fun(_, _) {}
    var onBreakTimeStarted: () -> Unit = {}
    var onBreakTimeCompleted: () -> Unit = {}

    private val timerScope = CoroutineScope(Dispatchers.Default)


    fun workMin() = workDuration / 60
    fun breakMin() = breakDuration / 60

    fun startTimer() {
        timerScope.launch {
            while (timeRemaining > 0 && !isReset) {
                delay(1000L)
                timeRemaining--

                onTimeUpdated(timeRemaining, timeMsg())

            }
            if (isReset) {
                isWorkingTime = true
                isReset = false
                timeRemaining = workDuration
                onTimeUpdated(timeRemaining, timeMsg())
            } else {
                switchSession()
            }
        }
    }

    fun timeMsg(t: Long = timeRemaining): String {
        return if (isWorkingTime) {
            "Work Time: ${secondsDisplay(t)}"
        } else {
            "Break Time: ${secondsDisplay(t)}"
        }
    }

    private fun switchSession() {
        if (isWorkingTime) {
            isWorkingTime = false
            timeRemaining = breakDuration
            onBreakTimeStarted()
            startTimer()
        } else {
            isWorkingTime = true
            timeRemaining = workDuration
            onBreakTimeCompleted()
        }
    }

    fun updateDurations(workMinutes: Long, breakMinutes: Long) {
        breakDuration = breakMinutes * 60
        workDuration = workMinutes * 60
        timeRemaining = workDuration
    }

    fun resetTimer() {
        isReset = true
    }
}

