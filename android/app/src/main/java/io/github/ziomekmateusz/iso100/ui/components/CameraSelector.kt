package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.ui.CameraLabel

@Composable
fun CameraSelector(camera: CameraLabel?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = colors.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.title_camera),
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )
                Text(
                    buildAnnotatedString {
                        append(camera?.title ?: stringResource(R.string.choose_camera))
                        camera?.detail?.let {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Normal,
                                    color = colors.onSurfaceVariant
                                )
                            ) { append(" " + stringResource(R.string.separator) + it) }
                        }
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Icon(
                painterResource(R.drawable.ic_forward),
                null,
                Modifier.size(20.dp),
                tint = colors.onSurfaceVariant
            )
        }
    }
}