package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.shortNumber

/** The focal length in mm: typed, or stepped through the usual lenses with [onStep]. */
@Composable
fun FocalLengthField(
    valueMm: Double,
    error: String?,
    onValueChange: (Double) -> Unit,
    onStep: (up: Boolean) -> Unit
) {
    NumberField(
        label = stringResource(R.string.focal_length),
        value = valueMm,
        unit = stringResource(R.string.symbol_mm),
        onValueChange = onValueChange,
        onDecrease = { onStep(false) },
        onIncrease = { onStep(true) },
        decreaseDescription = stringResource(R.string.focal_length_shorter),
        increaseDescription = stringResource(R.string.focal_length_longer),
        error = error,
    )
}

/** The aperture dial, "f/5.6" above and "5,6" under the long ticks; [onSelect] gets the exact aperture. */
@Composable
fun ApertureDial(dial: Dial, trailing: String, onSelect: (Double) -> Unit) {
    val locale = currentLocale()
    StopDial(
        label = stringResource(R.string.aperture),
        dial = dial,
        onSelect = { onSelect(it.actual) },
        valueText = { stringResource(R.string.aperture_value, shortNumber(it.nominal, locale)) },
        tickLabel = { shortNumber(it.nominal, locale) },
        trailing = trailing,
    )
}
