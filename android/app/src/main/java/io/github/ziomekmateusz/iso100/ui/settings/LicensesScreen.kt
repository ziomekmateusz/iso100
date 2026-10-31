package io.github.ziomekmateusz.iso100.ui.settings

import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** The licenses of what ships in the APK; [file] is the text in `assets/licenses/`. */
@Serializable
enum class OpenLicense(@StringRes val title: Int, @StringRes val covers: Int, val file: String) {
    APACHE(R.string.license_apache, R.string.license_apache_covers, "apache-2.0.txt"),
    BSD(R.string.license_bsd, R.string.license_bsd_covers, "protobuf-BSD-3-Clause.txt"),
    CC_BY_SA(R.string.license_cc_by_sa, R.string.license_cc_by_sa_covers, "cc-by-sa-3.0.txt"),
    OFL(R.string.license_ofl, R.string.license_ofl_covers, "barlow-OFL.txt"),
}

@Composable
fun LicensesScreen(onBack: () -> Unit, onOpen: (OpenLicense) -> Unit) {
    val colors = MaterialTheme.colorScheme
    ToolScaffold(title = stringResource(R.string.title_licenses), onBack = onBack) {
        OpenLicense.entries.forEach { license ->
            Surface(onClick = { onOpen(license) }, shape = MaterialTheme.shapes.medium, color = colors.surface) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(license.title), fontSize = 17.sp)
                        Text(stringResource(license.covers), fontSize = 14.sp, color = colors.onSurfaceVariant)
                    }
                    Icon(painterResource(R.drawable.ic_forward), null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LicenseScreen(license: OpenLicense, onBack: () -> Unit) {
    val assets = LocalContext.current.assets
    val text by produceState("", license) {
        value = withContext(Dispatchers.IO) { assets.open("licenses/${license.file}").bufferedReader().use { it.readText() } }
    }
    LicenseContent(license, text, onBack)
}

@Composable
fun LicenseContent(license: OpenLicense, text: String, onBack: () -> Unit) {
    ToolScaffold(title = stringResource(license.title), onBack = onBack) {
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun LicensesPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { LicensesScreen({}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun LicensePreview() {
    Iso100Theme { LicenseContent(OpenLicense.OFL, "This Font Software is licensed under the SIL Open Font License, Version 1.1.", {}) }
}
