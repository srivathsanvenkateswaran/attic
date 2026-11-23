package app.attic.player.ui.expressive

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** The Expressive type scale, provided by AtticTheme when the style is Expressive. */
val LocalExpressiveType = staticCompositionLocalOf { DefaultExpressiveType }

/** Shared typography instance, so families and styles are built once. */
internal val DefaultExpressiveType by lazy { ExpressiveTypography() }

/**
 * The expressive motion scheme, from the direction sheet. Spatial springs (position, size, shape)
 * overshoot; effects springs (colour, alpha) do not. Material 3 1.4 keeps its MotionScheme internal,
 * so these are the same values written out.
 */
object ExpressiveMotion {
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)
    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
    fun <T> slowEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)
}

/** One place to reach the Expressive tokens that MaterialTheme has no slot for. */
object Expressive {
    /** The type scale with its emphasised styles. The plain ones are also MaterialTheme.typography. */
    val type: ExpressiveTypography
        @Composable @ReadOnlyComposable get() = LocalExpressiveType.current

    val motion = ExpressiveMotion
    val shapes = ExpressiveShapes
}

/** The app scheme: the system's wallpaper scheme. minSdk is 33, so dynamic colour is always there. */
@Composable
fun expressiveColorScheme(dark: Boolean): ColorScheme {
    val context = LocalContext.current
    return remember(context, dark) { if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
}
