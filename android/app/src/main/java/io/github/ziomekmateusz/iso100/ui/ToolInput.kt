package io.github.ziomekmateusz.iso100.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.data.ToolValues
import io.github.ziomekmateusz.iso100.data.ToolValuesStore
import kotlinx.serialization.serializer

/**
 * The input of a tool, kept across rotation like any `rememberSaveable`: it starts from the values the
 * user left last time under [key], or [default], and saves them as they change.
 */
@Composable
inline fun <reified T : Any> rememberToolInput(key: String, default: T): MutableState<T> {
    val serializer = remember { serializer<T>() }
    return rememberToolInput(remember(key) { ToolValues(key, serializer, default) })
}

@Composable
fun <T : Any> rememberToolInput(
    tool: ToolValues<T>,
    store: ToolValuesStore = LocalAppContainer.current.toolValues
): MutableState<T> {
    val input = rememberSaveable { mutableStateOf(store.saved(tool)) }
    LaunchedEffect(input.value) { store.save(tool, input.value) }
    return input
}
