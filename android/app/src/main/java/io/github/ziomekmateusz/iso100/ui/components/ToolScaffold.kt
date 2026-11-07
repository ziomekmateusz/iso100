package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ziomekmateusz.iso100.R

/**
 * A tool screen: round back button and title fixed at the top, the optional [bottomBar] fixed at the
 * bottom, and between them the [content] and the [footer], which scroll and fade under the bars.
 *
 * The [footer] (the controls) sits at the bottom when everything fits and scrolls with the content
 * when it does not (large text, keyboard open). It is drawn edge to edge and gets the bottom padding
 * to apply inside its background. The scaffold handles every inset: neither slot adds its own.
 */
@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    footer: (@Composable (PaddingValues) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Scaffold(
        modifier.fillMaxSize(),
        topBar = { ToolTopBar(title, onBack, actions) },
        bottomBar = { Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) { bottomBar?.invoke() } },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { bars ->
        val bottom = bars.calculateBottomPadding()
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                .fadeUnder(
                    top = bars.calculateTopPadding(),
                    bottom = if (bottomBar != null) bottom else 0.dp
                ),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = bars.calculateTopPadding() + 14.dp,
                            bottom = 16.dp + if (footer == null) bottom else 0.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
                footer?.invoke(PaddingValues(bottom = bottom))
            }
        }
    }
}

@Composable
private fun ToolTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(R.drawable.ic_back, stringResource(R.string.back), onBack)
        Text(
            title,
            Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge
        )
        actions()
    }
}

/** 48 dp round button on the surface colour, used in the screen headers. */
@Composable
fun RoundIconButton(icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(painterResource(icon), contentDescription, Modifier.size(22.dp))
    }
}
