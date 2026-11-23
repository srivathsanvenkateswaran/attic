package app.attic.player.designrefs

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import androidx.test.core.app.ApplicationProvider
import app.attic.player.storeshots.DemoCovers
import app.attic.player.storeshots.DemoLibrary
import app.attic.player.storeshots.ShotsDir
import app.attic.player.ui.comfort.ComfortColors
import app.attic.player.ui.comfort.ComfortTint
import app.attic.player.ui.comfort.Oklch
import app.attic.player.ui.comfort.comfortColors
import app.attic.player.ui.comfort.toColorScheme
import app.attic.player.ui.coverSwatch
import app.attic.player.ui.coverTint
import app.attic.player.ui.expressive.ArtScheme
import app.attic.player.ui.expressive.Hct
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The colours the code computes, as JSON for the design files: the Expressive app scheme, the
 * scheme each demo cover gives a player surface, the Comfort palette and what `Tinted` makes of each
 * cover. Written to `<ShotsDir>/colors`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi", application = Application::class)
class DesignColors {
    private val dir = File(ShotsDir, "colors").apply { mkdirs() }
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun all() {
        primeCoverColours()
        expressiveBase()
        expressiveCovers()
        comfortBase()
        comfortTinted()
        File(dir, "DONE").writeText("done\n")
    }

    private fun expressiveBase() {
        val json = obj(
            "source" to "app/src/main/java/app/attic/player/ui/expressive/ExpressiveTheme.kt: expressiveColorScheme(dark), " +
                "which is androidx.compose.material3.dynamicLightColorScheme / dynamicDarkColorScheme(context)",
            "note" to "Expressive's app scheme is the phone's wallpaper (dynamic) colour scheme; there is no fixed palette in the code. " +
                "These values are what the JVM harness (Robolectric, SDK 35, AOSP default system_accent/system_neutral resources) " +
                "returns, which is also what every Expressive render in design-refs is drawn in. On a real phone every role follows the wallpaper.",
            "light" to scheme(dynamicLightColorScheme(context)),
            "dark" to scheme(dynamicDarkColorScheme(context)),
        )
        File(dir, "expressive-base.json").writeText(json.toJson() + "\n")
    }

    private fun expressiveCovers() {
        for (index in 0 until DemoCovers.COUNT) {
            val album = albumFor(index)
            val mediaId = album.tracks.first().mediaId
            val swatch = coverSwatch(context, mediaId)!!
            val seed = coverTint(context, mediaId)!!
            val hct = Hct.fromArgb(seed)
            fun theme(dark: Boolean): Map<String, Any?> {
                val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                val table = if (dark) ArtScheme.dark else ArtScheme.light
                return obj(
                    "roles" to obj(*ArtScheme.roles(seed, dark).map { (name, argb) ->
                        val (palette, role) = table.getValue(name)
                        name to obj(
                            "hex" to hex(argb),
                            "palette" to palette.name,
                            "hctHue" to round(ArtScheme.paletteHue(palette, hct.hue)),
                            "hctChroma" to role.chroma,
                            "hctTone" to role.tone,
                        )
                    }.toTypedArray()),
                    "colorScheme" to scheme(ArtScheme.colorScheme(seed, dark, base)),
                )
            }
            val json = obj(
                "source" to "app/src/main/java/app/attic/player/ui/expressive/FromArt.kt: FromArt(seed) / rememberArtScheme -> " +
                    "ArtScheme.kt: ArtScheme.colorScheme(seedArgb, dark, fallback = app scheme); seed from ui/Art.kt rememberArtColor(mediaId) (deepen = true)",
                "coverIndex" to index,
                "coverFile" to "covers/$index-${slug(album.name)}.jpg",
                "album" to album.name,
                "artist" to album.artist,
                "paletteSwatch" to obj(
                    "hex" to hex(swatch),
                    "how" to "androidx.palette Palette.from(cover scaled to 96x96).generate(): vibrantSwatch ?: mutedSwatch ?: dominantSwatch (ui/Art.kt coverSwatch)",
                ),
                "seed" to obj(
                    "hex" to hex(seed),
                    "how" to "ui/Art.kt deepenTint(swatch): HSL saturation capped at 0.62, lightness clamped to 0.2..0.3",
                    "hctHue" to round(hct.hue),
                    "hctChroma" to round(hct.chroma),
                    "hctTone" to round(hct.tone),
                ),
                "rule" to "Only the seed's HCT hue is read. Each role is Hct(hue, fixedChroma, fixedTone); tertiary uses hue + 60. " +
                    "background = surface, onBackground = onSurface, surfaceVariant = surfaceContainerHighest, surfaceTint = primary. " +
                    "Error roles, scrim and the *Fixed roles are not touched: they come from the app scheme (expressive-base.json).",
                "light" to theme(false),
                "dark" to theme(true),
            )
            File(dir, "expressive-cover-$index.json").writeText(json.toJson() + "\n")
        }
    }

    private fun comfortBase() {
        fun theme(dark: Boolean): Map<String, Any?> {
            val colors = comfortColors(dark)
            return obj("tokens" to tokens(colors), "colorScheme" to scheme(colors.toColorScheme()))
        }
        val json = obj(
            "source" to "app/src/main/java/app/attic/player/ui/comfort/ComfortColors.kt: ComfortDark, ComfortLight and ComfortColors.toColorScheme(); " +
                "chosen by ComfortTheme.kt comfortColors(dark)",
            "note" to "tokens are the Comfort kit tokens by their Kotlin names (kit.css --fill-2 is fill2, --accent-ink is accentInk). " +
                "accent is Lamplight, onAccent Soot, accentInk Brass in light, danger Ember, ground Night/Plaster, card Loft/Paper, raised Rafter, " +
                "fill Slate/Putty, line Seam, on Chalk/Ink, on2 Dust, on3 Ash. colorScheme is the Material scheme toColorScheme() builds from them " +
                "for stock Material components.",
            "light" to theme(false),
            "dark" to theme(true),
        )
        File(dir, "comfort-base.json").writeText(json.toJson() + "\n")
    }

    private fun comfortTinted() {
        for (index in 0 until DemoCovers.COUNT) {
            val album = albumFor(index)
            val seed = coverSwatch(context, album.tracks.first().mediaId)!!
            val lch = Oklch.fromArgb(seed)
            fun theme(dark: Boolean): Map<String, Any?> {
                val base = comfortColors(dark)
                fun variant(wash: Boolean): Map<String, Any?> {
                    val c = ComfortTint.apply(base, seed, wash)
                    val specs = buildMap {
                        putAll(if (dark) ComfortTint.darkTinted else ComfortTint.lightTinted)
                        if (wash) putAll(if (dark) ComfortTint.darkWash else ComfortTint.lightWash)
                    }
                    val all = tokens(c)
                    val baseTokens = tokens(base)
                    return obj(
                        "changed" to obj(*all.filter { it.value != baseTokens[it.key] }.map { (name, value) ->
                            val spec = specs[name]
                            name to obj(
                                "hex" to value,
                                "oklchL" to spec?.lightness,
                                "chromaFactor" to spec?.chroma,
                                "oklchC" to spec?.let { round(lch.c * it.chroma, 4) },
                                "rule" to (spec?.let { "oklch(${it.lightness} seed.c*${it.chroma} seed.h)" } ?: "fixed white (light theme onAccent)"),
                            )
                        }.toTypedArray()),
                        "tokens" to all,
                        "colorScheme" to scheme(c.toColorScheme()),
                    )
                }
                return obj("tinted" to variant(false), "tintedWash" to variant(true))
            }
            val json = obj(
                "source" to "app/src/main/java/app/attic/player/ui/comfort/Tinted.kt: Tinted(seed, wash) -> ComfortTint.apply(base, tint, wash), " +
                    "with Oklch.kt; seed from ui/Art.kt rememberArtColor(mediaId, deepen = false) = coverSwatch",
                "coverIndex" to index,
                "coverFile" to "covers/$index-${slug(album.name)}.jpg",
                "album" to album.name,
                "artist" to album.artist,
                "seed" to obj(
                    "hex" to hex(seed),
                    "how" to "androidx.palette Palette.from(cover scaled to 96x96).generate(): vibrantSwatch ?: mutedSwatch ?: dominantSwatch, not deepened",
                    "oklchL" to round(lch.l, 4),
                    "oklchC" to round(lch.c, 4),
                    "oklchH" to round(lch.h, 2),
                ),
                "variants" to "tinted = Tinted(seed) (wash = false): accent tokens and the mini player's fills. " +
                    "tintedWash = Tinted(seed, wash = true): also the surfaces and on2/on3. on is never changed. See comfort-tinted-rules.md.",
                "light" to theme(false),
                "dark" to theme(true),
            )
            File(dir, "comfort-tinted-$index.json").writeText(json.toJson() + "\n")
        }
    }

    // Helpers

    private fun albumFor(index: Int) = DemoLibrary.library.albums.first { DemoLibrary.coverFor(it.tracks.first().mediaId) == index }

    /** As the store-shot harness does it: ui/Art.kt's cover-colour cache, filled from the generated covers. */
    @Suppress("UNCHECKED_CAST")
    private fun primeCoverColours() {
        val field = Class.forName("app.attic.player.ui.ArtKt").getDeclaredField("artColors").apply { isAccessible = true }
        val cache = field.get(null) as LruCache<Long, Int>
        for (track in DemoLibrary.library.tracks) {
            val cover = DemoCovers.get(DemoLibrary.coverFor(track.mediaId)!!)
            val small = Bitmap.createScaledBitmap(cover, 96, 96, true)
            val palette = Palette.from(small).generate()
            val swatch = palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
            cache.put(track.mediaId, swatch!!.rgb)
        }
    }

    private fun scheme(s: ColorScheme): Map<String, Any?> = colourGetters(s)
    private fun tokens(c: ComfortColors): Map<String, Any?> = colourGetters(c)

    /** Every Color property of [o] (Compose stores them as a mangled `long` getter), in declaration order. */
    private fun colourGetters(o: Any): Map<String, Any?> {
        val order = o.javaClass.declaredFields.map { it.name.removeSuffix("\$delegate") }
        val getters = o.javaClass.methods.filter { m ->
            m.parameterCount == 0 && m.returnType == java.lang.Long.TYPE && m.name.startsWith("get")
        }.associateBy { m -> m.name.removePrefix("get").substringBefore('-').replaceFirstChar { it.lowercase() } }
        val names = order.filter { it in getters } + getters.keys.filter { it !in order }.sorted()
        return obj(*names.map { name ->
            val raw = getters.getValue(name).invoke(o) as Long
            name to hex(Color(raw.toULong()))
        }.toTypedArray())
    }

    private fun hex(c: Color): String = hex(c.toArgb())
    private fun hex(argb: Int): String = "#%06X%02X".format(argb and 0xFFFFFF, (argb ushr 24) and 0xFF)
    private fun round(v: Double, places: Int = 2): Double = Math.round(v * Math.pow(10.0, places.toDouble())) / Math.pow(10.0, places.toDouble())
    private fun obj(vararg pairs: Pair<String, Any?>): Map<String, Any?> = linkedMapOf(*pairs)

    private fun Any?.toJson(indent: String = ""): String = when (this) {
        null -> "null"
        is String -> "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Number, is Boolean -> toString()
        is Map<*, *> -> if (isEmpty()) "{}" else entries.joinToString(",\n", "{\n", "\n$indent}") { (k, v) ->
            "$indent  \"$k\": " + v.toJson("$indent  ")
        }
        is List<*> -> joinToString(", ", "[", "]") { it.toJson(indent) }
        else -> "\"$this\""
    }
}
