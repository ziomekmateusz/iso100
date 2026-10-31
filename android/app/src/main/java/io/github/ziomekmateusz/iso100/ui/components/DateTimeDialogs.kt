package io.github.ziomekmateusz.iso100.ui.components

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.ziomekmateusz.iso100.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** The picker works in UTC milliseconds: the date goes through midnight UTC and back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(date: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis
                if (picked == null) onDismiss() else onPick(Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(time: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(time.hour, time.minute, is24Hour = DateFormat.is24HourFormat(LocalContext.current))
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state) },
    )
}
