package app.attic.player.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.core.content.res.ResourcesCompat
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortIconWeight
import app.attic.player.ui.comfort.Onest
import app.attic.player.ui.expressive.DefaultExpressiveType
import app.attic.player.ui.expressive.allStyles
import app.attic.player.ui.expressive.commonSymbolFamilies
import java.util.concurrent.ConcurrentHashMap

/**
 * One weight of a bundled variable font with its axes pinned, like `Font(resId, weight,
 * variationSettings)`, but its typeface is made once per process and kept.
 *
 * Every axis setting is its own Android typeface, and making one costs a few milliseconds on the
 * thread that asks (`Typeface.createFromTypefaceWithVariation`, which builds a minikin font
 * collection). Compose keeps only the last 16 it resolved, and the styles use more than that, so with
 * plain `Font`s the same typefaces were made again and again, on the main thread: Expressive's first
 * frame spent about 30 ms there. Here an evicted one is a map lookup, and [warmTypefaces] makes the
 * style's typefaces on a background thread while the app starts.
 */
fun variableFont(@FontRes resId: Int, weight: FontWeight, settings: FontVariation.Settings): Font =
    VariableFont(resId, weight, settings)

private class VariableFont(
    val resId: Int,
    override val weight: FontWeight,
    val settings: FontVariation.Settings,
) : AndroidFont(FontLoadingStrategy.Blocking, VariableTypefaces, settings) {
    override val style: FontStyle get() = FontStyle.Normal

    override fun equals(other: Any?) =
        other is VariableFont && other.resId == resId && other.weight == weight && other.settings == settings

    override fun hashCode() = (resId * 31 + weight.hashCode()) * 31 + settings.hashCode()

    override fun toString() = "VariableFont(resId=$resId, weight=$weight, settings=$settings)"
}

private object VariableTypefaces : AndroidFont.TypefaceLoader {
    private data class Key(val resId: Int, val settings: String)

    private val made = ConcurrentHashMap<Key, Typeface>()

    override fun loadBlocking(context: Context, font: AndroidFont): Typeface {
        font as VariableFont
        // Only plain number axes are used (weight, width, ROND, opsz, FILL), so no density is needed.
        val settings = font.settings.settings.joinToString(",") { "'${it.axisName}' ${it.toVariationValue(null)}" }
        // computeIfAbsent: a thread that asks for one the warm-up is still making waits for it
        // instead of making a second copy.
        return made.computeIfAbsent(Key(font.resId, settings)) {
            val base = ResourcesCompat.getFont(context, font.resId) ?: error("No font ${font.resId}")
            Paint().apply {
                typeface = base
                fontVariationSettings = settings
            }.typeface
        }
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface = loadBlocking(context, font)
}

/**
 * Makes the typefaces for [styles] and [families] (each at the weights given) through Compose's own
 * resolver, so the first screen finds them made. Blocking; call it off the main thread.
 */
fun warmTypefaces(context: Context, styles: List<TextStyle>, families: List<Pair<FontFamily, FontWeight>> = emptyList()) {
    val resolver = createFontFamilyResolver(context)
    for (style in styles) {
        resolver.resolve(style.fontFamily, style.fontWeight ?: FontWeight.Normal)
    }
    for ((family, weight) in families) {
        resolver.resolve(family, weight)
    }
}

/** [warmTypefaces] for [style]: its type scale and its common icons. */
fun warmStyleTypefaces(context: Context, style: UiStyle) = when (style) {
    UiStyle.Expressive -> warmTypefaces(context, DefaultExpressiveType.allStyles(), commonSymbolFamilies(400))
    UiStyle.Comfort -> warmTypefaces(
        context,
        styles = emptyList(),
        families = listOf(400, 500, 600, 700, 800).map { Onest to FontWeight(it) } + commonSymbolFamilies(ComfortIconWeight),
    )
}
