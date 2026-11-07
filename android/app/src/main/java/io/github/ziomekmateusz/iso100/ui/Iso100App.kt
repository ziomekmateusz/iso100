package io.github.ziomekmateusz.iso100.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.ziomekmateusz.iso100.ui.camera.CameraPickerScreen
import io.github.ziomekmateusz.iso100.ui.dof.DofScreen
import io.github.ziomekmateusz.iso100.ui.exposure.ExposureScreen
import io.github.ziomekmateusz.iso100.ui.fov.FovScreen
import io.github.ziomekmateusz.iso100.ui.home.HomeScreen
import io.github.ziomekmateusz.iso100.ui.moon.MoonScreen
import io.github.ziomekmateusz.iso100.ui.place.SavedPlacesScreen
import io.github.ziomekmateusz.iso100.ui.settings.LicenseScreen
import io.github.ziomekmateusz.iso100.ui.settings.LicensesScreen
import io.github.ziomekmateusz.iso100.ui.settings.OpenLicense
import io.github.ziomekmateusz.iso100.ui.settings.SettingsScreen
import io.github.ziomekmateusz.iso100.ui.stars.StarsScreen
import io.github.ziomekmateusz.iso100.ui.sun.SunScreen
import io.github.ziomekmateusz.iso100.ui.timelapse.TimelapseScreen
import io.github.ziomekmateusz.iso100.ui.timer.TimerScreen
import kotlinx.serialization.Serializable

/** The screens of the app: one key per tool, plus settings, camera picker, saved places and licenses. */
@Serializable sealed interface Destination : NavKey

@Serializable data object Home : Destination

@Serializable data object Exposure : Destination

@Serializable data object Stars : Destination

@Serializable data object Dof : Destination

@Serializable data object Fov : Destination

@Serializable data object Sun : Destination

@Serializable data object Moon : Destination

@Serializable data object Timelapse : Destination

@Serializable data object Settings : Destination

@Serializable data object CameraPicker : Destination

@Serializable data object SavedPlaces : Destination

@Serializable data object Licenses : Destination

@Serializable data class LicenseText(val license: OpenLicense) : Destination

@Serializable
data class Timer(val seconds: Double, val settings: String) : Destination

private val iosEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)
private fun <T> ios() = tween<T>(durationMillis = 400, easing = iosEasing)

private val push: ContentTransform =
    slideInHorizontally(ios()) { it } togetherWith
            (slideOutHorizontally(ios()) { -it / 3 } + fadeOut(ios(), targetAlpha = 0.6f))

private val pop: ContentTransform =
    (
            (slideInHorizontally(ios()) { -it / 3 } + fadeIn(ios(), initialAlpha = 0.6f)) togetherWith
                    slideOutHorizontally(ios()) { it }
            )
        .apply { targetContentZIndex = -1f }

@Composable
fun Iso100App(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)
    val back: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = back,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        transitionSpec = { push },
        popTransitionSpec = { pop },
        predictivePopTransitionSpec = { pop },
        entryProvider = { key ->
            NavEntry(key) {
                val chooseCamera: () -> Unit = { backStack.add(CameraPicker) }
                when (key) {
                    Home -> HomeScreen(onOpen = { backStack.add(it) })

                    CameraPicker -> CameraPickerScreen(onClose = back)

                    Exposure -> ExposureScreen(
                        onBack = back,
                        onStartTimer = { seconds, settings ->
                            backStack.add(
                                Timer(
                                    seconds,
                                    settings,
                                ),
                            )
                        },
                    )

                    is Timer -> TimerScreen(key.seconds, key.settings, onBack = back)

                    Stars -> StarsScreen(onBack = back, onChooseCamera = chooseCamera)

                    Timelapse -> TimelapseScreen(onBack = back)

                    Sun -> SunScreen(onBack = back)

                    Moon -> MoonScreen(onBack = back)

                    SavedPlaces -> SavedPlacesScreen(onBack = back)

                    Settings -> SettingsScreen(onBack = back, onChooseCamera = chooseCamera, onSavedPlaces = {
                        backStack.add(SavedPlaces)
                    }, onLicenses = { backStack.add(Licenses) })

                    Licenses -> LicensesScreen(onBack = back, onOpen = { backStack.add(LicenseText(it)) })

                    is LicenseText -> LicenseScreen(key.license, onBack = back)

                    Dof -> DofScreen(onBack = back, onChooseCamera = chooseCamera)

                    Fov -> FovScreen(onBack = back, onChooseCamera = chooseCamera)

                    else -> error("unknown destination $key")
                }
            }
        },
    )
}
