package view

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

@Composable
fun timerView() {
    val notificationService = NotificationService()
    TomatoClockApp(
        timerService = TimerService(2400L, 300L),
        notificationService = notificationService
    )
}

@Composable
fun TomatoClockApp(timerService: TimerService, notificationService: NotificationService) {
    var remainingTime by remember { mutableStateOf(timerService.workDuration) }
    var isWorkingTime by remember { mutableStateOf(true) }
    var notificationMessage by remember { mutableStateOf("Start your work!") }

    var isFullScreen by remember { mutableStateOf(false) }

    var workDurationInput by remember { mutableStateOf(TextFieldValue("40")) }
    var breakDurationInput by remember { mutableStateOf(TextFieldValue("5")) }

    fun updateDurations() {
        val workMinutes = workDurationInput.text.toLongOrNull() ?: 40
        val breakMinutes = breakDurationInput.text.toLongOrNull() ?: 5
        timerService.updateDurations(workMinutes, breakMinutes)
        remainingTime = workMinutes * 60
    }

    timerService.onTimeUpdated = { time ->
        remainingTime = time
    }

    timerService.onSessionCompleted = {
        notificationMessage = if (isWorkingTime) {
            "Break time! Please relax."
        } else {
            "Time to work again!"
        }
        notificationService.showNotification("Pomodoro Timer", notificationMessage)
    }

    timerService.onBreakTimeCompleted = {
        isWorkingTime = true
    }
    timerService.onBreakTimeStarted = {
        isWorkingTime = false
        isFullScreen = true
        notificationMessage = "Break time started!"
        notificationService.showNotification("Pomodoro Timer", notificationMessage)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Timer", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isWorkingTime) {
                "Work Time: ${secondsDisplay(remainingTime)}"
            } else {
                "Break Time: ${secondsDisplay(remainingTime)}"
            },
            style = MaterialTheme.typography.h5
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
        transparent = true
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
    var workDuration: Long,
    private var breakDuration: Long
) {
    private var timeRemaining: Long = workDuration
    private var isWorkingTime: Boolean = true
    private var isReset = false

    var onTimeUpdated: (Long) -> Unit = {}
    var onSessionCompleted: () -> Unit = {}
    var onBreakTimeStarted: () -> Unit = {}
    var onBreakTimeCompleted: () -> Unit = {}

    private val timerScope = CoroutineScope(Dispatchers.Default)

    fun startTimer() {
        timerScope.launch {
            while (timeRemaining > 0 && !isReset) {
                delay(1000L)
                timeRemaining--

                onTimeUpdated(timeRemaining)

            }
            if (isReset) {
                isWorkingTime = true
                isReset = false
                timeRemaining = workDuration
                onTimeUpdated(timeRemaining)
            } else {
                onSessionCompleted()
                switchSession()
            }
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

