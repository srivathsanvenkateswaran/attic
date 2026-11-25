package app.attic.player.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import app.attic.player.ui.DesignTokens.hex
import app.attic.player.ui.DesignTokens.num
import app.attic.player.ui.comfort.ComfortColors
import app.attic.player.ui.comfort.ComfortDark
import app.attic.player.ui.comfort.ComfortLight
import app.attic.player.ui.comfort.ComfortShapes
import app.attic.player.ui.comfort.ComfortTypography
import app.attic.player.ui.expressive.ExpressiveMotion
import app.attic.player.ui.expressive.ExpressiveShapes
import app.attic.player.ui.expressive.ExpressiveTypography
import java.io.File
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Keeps docs/design-system in step with the code. tokens.json must be exactly what [DesignTokens]
 * reads from the code, and every checked row of tokens.md (a table row whose first cell is a token's
 * code expression, such as `` `Comfort.colors.ground` ``) must carry the value the code has.
 *
 * After changing a token: run scripts/design_tokens.sh to rewrite tokens.json, then fix tokens.md.
 */
class DesignTokensTest {

    private val root: File = generateSequence(File(System.getProperty("user.dir")!!).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }
    private val docs = File(root, "docs/design-system")
    private val res = File(root, "app/src/main/res")

    @Test
    fun tokenListsCoverEveryProperty() {
        assertSameNames(
            "ComfortColors",
            instanceFields(ComfortColors::class.java).filter { it != "dark" },
            DesignTokens.comfortColorNames,
        )
        assertSameNames(
            "ComfortTypography",
            instanceFields(ComfortTypography::class.java, TextStyle::class.java),
            DesignTokens.comfortTypeStyles().map { it.first },
        )
        assertSameNames(
            "ExpressiveTypography",
            instanceFields(ExpressiveTypography::class.java, TextStyle::class.java),
            DesignTokens.expressiveTypeStyles().map { it.first },
        )
        // A Dp is a Float underneath, so an object's Dp vals are its float fields.
        assertSameNames("ExpressiveShapes", floatFields(ExpressiveShapes::class.java), DesignTokens.expressiveCorners.map { it.first })
        assertSameNames("ComfortShapes", floatFields(ComfortShapes::class.java), DesignTokens.comfortRadii.map { it.first })
        assertSameNames(
            "ExpressiveMotion",
            ExpressiveMotion::class.java.declaredMethods.filter { Modifier.isPublic(it.modifiers) }.map { it.name },
            DesignTokens.expressiveMotion.map { it.first },
        )
    }

    @Test
    fun tokensJsonMatchesCode() {
        val generated = DesignTokens.json(DesignTokens.tree(res)) + "\n"
        val file = File(docs, "tokens.json")
        if (System.getProperty("designTokens.write") == "true") {
            file.parentFile.mkdirs()
            file.writeText(generated)
            return
        }
        if (!file.exists()) fail("docs/design-system/tokens.json is missing. Run scripts/design_tokens.sh.")
        assertEquals(
            "docs/design-system/tokens.json no longer matches the code. Run scripts/design_tokens.sh, then update tokens.md.",
            generated,
            file.readText(),
        )
    }

    /** The widget's first frame and the splash window are written as hex in colors.xml; they must be the kit's values. */
    @Test
    fun colourResourcesFollowComfortPalette() {
        val pairs = listOf(
            "comfort_window" to "ground", "comfort_widget_card" to "card", "comfort_widget_on" to "on",
            "comfort_widget_on_2" to "on2", "comfort_widget_accent" to "accent", "comfort_widget_on_accent" to "onAccent",
            "comfort_widget_fill" to "fill", "comfort_widget_line" to "line",
        )
        val problems = mutableListOf<String>()
        for ((dir, palette) in listOf("values" to ComfortLight, "values-night" to ComfortDark)) {
            val colours = DesignTokens.colorResources(File(res, "$dir/colors.xml"))
            for ((resource, token) in pairs) {
                val want = hex(DesignTokens.comfortColor(palette, token))
                val got = colours[resource]?.uppercase()
                if (got != want) problems += "$dir/colors.xml $resource is $got; Comfort.colors.$token is $want"
            }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun tokensMdMatchesCode() {
        val file = File(docs, "tokens.md")
        if (!file.exists()) fail("docs/design-system/tokens.md is missing.")
        val rows = file.readLines().filter { it.trimStart().startsWith("|") }.mapNotNull { line ->
            val cells = line.trim().trim('|').split("|").map { it.trim() }
            val first = cells.firstOrNull() ?: return@mapNotNull null
            if (first.length > 2 && first.startsWith("`") && first.endsWith("`")) first.removeSurrounding("`") to line else null
        }
        val byKey = rows.groupBy({ it.first }, { it.second })
        val problems = mutableListOf<String>()

        fun check(key: String, vararg wants: String, ordered: Boolean = false) {
            val found = byKey[key].orEmpty()
            when {
                found.isEmpty() -> problems += "No row for `$key`"
                found.size > 1 -> problems += "`$key` starts ${found.size} rows; it should start one"
                else -> {
                    val row = found.single()
                    val missing = wants.filterNot { hasValue(row, it) }
                    if (missing.isNotEmpty()) problems += "`$key` row should contain ${missing.joinToString()}: $row"
                    else if (ordered && wants.size == 2 && wants[0] != wants[1] && row.indexOf(wants[0]) > row.indexOf(wants[1])) {
                        problems += "`$key` row should give dark (${wants[0]}) before light (${wants[1]}): $row"
                    }
                }
            }
        }

        for (name in DesignTokens.comfortColorNames) {
            fun cell(c: Color) = "`${hex(c)}`"
            check(
                "Comfort.colors.$name",
                cell(DesignTokens.comfortColor(ComfortDark, name)),
                cell(DesignTokens.comfortColor(ComfortLight, name)),
                ordered = true,
            )
        }
        for ((name, style) in DesignTokens.comfortTypeStyles()) {
            val tracking = DesignTokens.comfortTrackingEm(style)
            val wants = listOfNotNull(sizeLine(style), style.fontWeight!!.weight.toString(), if (tracking == 0.0) null else "${num(tracking)}em")
            check("Comfort.type.$name", *wants.toTypedArray())
        }
        for ((name, style) in DesignTokens.expressiveTypeStyles()) {
            val tracking = style.letterSpacing.value
            val wants = listOfNotNull(sizeLine(style), style.fontWeight!!.weight.toString(), if (tracking == 0f) null else "${num(tracking)}sp")
            check("Expressive.type.$name", *wants.toTypedArray())
        }
        for ((name, dp) in DesignTokens.comfortRadii) check("ComfortShapes.$name", "${num(dp.value)}dp")
        for ((name, n) in DesignTokens.comfortSuperellipses) check("ComfortShapes.$name", "n = ${num(n)}")
        for ((name, dp) in DesignTokens.expressiveCorners) check("ExpressiveShapes.$name", "${num(dp.value)}dp")
        for ((name, spec) in DesignTokens.expressiveMotion) check("Expressive.motion.$name", num(spec.dampingRatio), num(spec.stiffness))
        check("TintSpec", "${DesignTokens.tintMillis} ms")

        // A row for a token the code no longer has.
        val known = DesignTokens.comfortColorNames.map { "Comfort.colors.$it" } +
            DesignTokens.comfortTypeStyles().map { "Comfort.type.${it.first}" } +
            DesignTokens.expressiveTypeStyles().map { "Expressive.type.${it.first}" } +
            DesignTokens.expressiveMotion.map { "Expressive.motion.${it.first}" }
        val prefixes = listOf("Comfort.colors.", "Comfort.type.", "Expressive.type.", "Expressive.motion.")
        for (key in byKey.keys) {
            if (prefixes.any { key.startsWith(it) } && key !in known) problems += "`$key` is not a token in the code"
        }

        if (problems.isNotEmpty()) fail("docs/design-system/tokens.md disagrees with the code:\n" + problems.joinToString("\n"))
    }

    private fun sizeLine(style: TextStyle) = "${num(style.fontSize.value)}/${num(style.lineHeight.value)}"

    /** [want] appears in [row] as a whole value, so `500` does not match `1500` or `#L500`. */
    private fun hasValue(row: String, want: String): Boolean {
        if (want.startsWith("`")) return row.contains(want)
        return Regex("""(?<![\w.#/-])""" + Regex.escape(want) + """(?![\w.]|/\d)""").containsMatchIn(row)
    }

    private fun instanceFields(type: Class<*>, of: Class<*>? = null) = type.declaredFields
        .filter { !Modifier.isStatic(it.modifiers) && (of == null || it.type == of) }
        .map { it.name }

    private fun floatFields(type: Class<*>) = type.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.type == Float::class.javaPrimitiveType }
        .map { it.name }

    private fun assertSameNames(what: String, inCode: List<String>, listed: List<String>) {
        val missing = inCode.toSet() - listed.toSet()
        val extra = listed.toSet() - inCode.toSet()
        if (missing.isNotEmpty() || extra.isNotEmpty()) {
            fail("DesignTokens lists for $what are out of date. Not listed: $missing. Listed but gone: $extra.")
        }
    }
}
