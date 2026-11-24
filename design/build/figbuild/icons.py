"""Material Symbols Rounded glyphs as SVG path data in a 24 x 24 box.

Reads the app's own subset font (res/font/material_symbols_rounded.ttf) and the Symbol enum in
ui/expressive/Symbols.kt, so the icon set in the design files is exactly the set the app ships.
"""
import re
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.varLib.instancer import instantiateVariableFont

REPO = Path(__file__).resolve().parents[3]
FONT = REPO / "app/src/main/res/font/material_symbols_rounded.ttf"
SYMBOLS = REPO / "app/src/main/java/app/attic/player/ui/expressive/Symbols.kt"


def symbols():
    """[(EnumName, ligature, codepoint)] in the order Symbols.kt lists them."""
    text = SYMBOLS.read_text()
    return [(m[0], m[1], int(m[2], 16))
            for m in re.findall(r'^\s*(\w+)\("([a-z0-9_]+)", 0x([0-9A-Fa-f]+)\)', text, re.M)]


_cache = {}


def _font(fill: int, wght: int = 400, opsz: int = 24):
    key = (fill, wght, opsz)
    if key not in _cache:
        _cache[key] = instantiateVariableFont(TTFont(FONT), {"FILL": fill, "wght": wght, "opsz": opsz})
    return _cache[key]


def path(codepoint: int, fill: int = 0, size: float = 24.0, wght: int = 400, opsz: int = 24) -> str:
    """SVG path data for one glyph, y down, in a size x size box (the glyph's own 960-unit em)."""
    font = _font(fill, wght, opsz)
    gs = font.getGlyphSet()
    name = font.getBestCmap()[codepoint]
    pen = SVGPathPen(gs)
    s = size / 960.0
    gs[name].draw(TransformPen(pen, (s, 0, 0, -s, 0, size)))
    return pen.getCommands()
