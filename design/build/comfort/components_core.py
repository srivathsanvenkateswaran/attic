"""Core components of Attic Comfort. Pages: Components / Actions, Components / Lists and cards,
Components / Navigation, Components / Playback.

Owner: foundations agent. Every set is drawn from ui/comfort/ (the description names file and composable),
is linked with codeConnect (compose, app.attic.player.ui.comfort), binds colours to Color/*, text to the
foundations' text styles and icons as Icon/* instances. Sizes are dp. Names below are stable.

Variant properties are listed as Prop=Value|Value; component properties after "props".

COMPONENTS / ACTIONS
  Button                Style=Filled|Tonal|Plain|Text, Size=Small|Medium|Large, State=Default|Disabled,
                        Tone=Default|Danger (Disabled: Medium only; Danger: Plain and Text, Medium).
                        props Label (text), Show leading icon (bool), Leading icon (swap). Buttons.kt ComfortButton
  Icon button           Tone=Default|Dim, State=Default|Disabled. props Icon (swap). Buttons.kt ComfortIconButton
  Tonal icon button     Size=Medium|Large, Checked=False|True, State=Default|Disabled, Container=Fill|Fill 2|Card|Ground toggle.
                        props Icon (swap). Buttons.kt TonalIconButton
  Heart button          Tonal=False|True, Favourite=False|True. Buttons.kt HeartButton
  Play button           Size=Large|Medium|Small, Playing=False|True. Buttons.kt PlayButton
  Collection actions    Playing=False|True. Buttons.kt CollectionActions + CollectionPlayButton
  Sub-tab               Selected=False|True. props Label. Bars.kt SubTabs (one option)
  Chip                  Selected=False|True, Container=Ground|Card. props Label. Bars.kt FilterChip
  Chip row              (single) four Chip instances. Bars.kt ChipRow
  Switch                Checked=False|True, State=Default|Disabled. Controls.kt ComfortSwitch
  Radio                 Selected=False|True. Controls.kt ComfortRadio
  Progress              Thick=False|True. Controls.kt ComfortProgress
  Spinner               Size=Default|Small|Large. Controls.kt Spinner (a still frame at the kit's 28 degrees)
  Search field          State=Empty|Query, Container=Ground|Card. props Query. Bars.kt SearchField

COMPONENTS / LISTS AND CARDS
  Cover/Album           Shape=Squircle|Squircle large, Art=Image|None, Dimmed=False|True. Artwork.kt Cover
                        Image: place an image on the "Art" layer: cover_image(d, instance, path).
  Cover/Artist          Art=Image|None. Artwork.kt ArtistCover (n = 2.6)
  Cover/Mosaic          Shape=Squircle|Squircle large. Layers "Art 1".."Art 4" take images. Artwork.kt Mosaic
  Icon box              Style=Tonal|Neutral|Solid. props Icon (swap). Artwork.kt IconBox / FavouritesArt / NoArt
  Playing bars          Size=Row|Tile, Playing=False|True. Rows.kt PlayingBars
  Song row trailing     Kind=More|Drag handle|Add|Added|Radio, Tone=Default|Current. Rows.kt SongMoreButton,
                        SongDragHandle, SongAddButton, SongRadioMark
  Song row              Lead=Cover|Number|Artist|Rank|Rank artist|None, Current=False|True.
                        props Title, Subtitle, Show subtitle, Number, Meta, Show meta, Show trailing, Show hairline; the nested
                        "Trailing" (Song row trailing) is exposed, so Kind shows on the row. Rows.kt SongRow
  Collection row        Kind=Album|Artist|Playlist|Folder|Favourites, Size=Small|Medium|Large.
                        props Title, Subtitle, Show subtitle, Overline, Show overline, Value, Show value,
                        Show chevron, Show hairline. Rows.kt CollectionRow
  Settings row          Control=None|Switch off|Switch on|Radio off|Radio on|Value|Chevron.
                        props Title, Summary, Show summary, Value, Show hairline. Controls.kt SetRow, SetSwitchRow, SetRadioRow
  Settings block        After row=False|True. props Text, Show text, Small print above, Show small print above,
                        Show progress, Small print, Show small print, Show button. Controls.kt SetBlock; ComfortSettings.kt
                        BlockText, BlockSmall, BlockButton
  Key value             (single) props Label, Value. Rows.kt KeyValue
  Section label         Spacing=Default|Tight. props Label, Small, Show small, End, End icon (swap), Show end. Groups.kt Label
  Note                  (single) props Text. Groups.kt Note
  Group card            Pad=False|True. A slot "Content": instance.slotInsert / node.reparent rows into it.
                        Groups.kt Group
  Tile                  Kind=Album|Recent|More from|Grid|Artist|Shortcut|Folder, Current=False|True (Album, Recent).
                        props Title, Subtitle (Artist and Shortcut have none), Show icon box (Shortcut: Favourites'
                        tonal heart in place of the cover). Tiles.kt Tile
  Browse tile           Tinted=False|True (True sets Color mode Tinted wash). props Title, Subtitle. Tiles.kt BrowseTile
  Collection header     Kind=Album|Person. props Title, Byline, Meta. Tiles.kt CollectionHeader
  Recap entry           (single) props Title, Body. ComfortHome.kt RecapEntry

COMPONENTS / NAVIGATION
  Status bar            (single) "10:24", signal, wifi, battery in Color/system/ink. storeShots ShotHarness.kt SystemBars
  Gesture bar           (single) the handle in Color/system/gesture. ShotHarness.kt SystemBars
  Mark                  (single) the Headroom mark in Color/accent, 21 x 30. ui/expressive/Mark.kt AtticMarkIcon
  Hero                  Mark=False|True, Short=False|True. props Title, Subtitle, Show subtitle. Bars.kt Hero
  Top bar               Back=None|Arrow|Close, Lift=False|True, Count=False|True. props Title, Show action 1/2,
                        Action 1/2 (swap). Bars.kt Bar
  Sub-tab row           Lift=False|True. Four Sub-tab instances (Components / Actions). Bars.kt SubTabs, the whole row
  Tools                 Leading=Sub-tabs|None (None shows search and add, as Library does). Bars.kt Tools
  Tab                   Selected=False|True. props Label, Icon (swap). Dock.kt TabBar item
  Tab bar               Tab=Home|Search|Library. Dock.kt TabBar
  Mini player           Placement=Dock|Alone|Top, Playing=False|True, Favourite=False|True, Art=Image|None (Dock only).
                        props Title, Artist.
                        Dock variants set Color mode Tinted on themselves (MiniPlayer tints itself). Dock.kt MiniPlayer
  Dock                  Tab=Home|Search|Library|None, Mini player=False|True. Dock.kt Dock
  Action bar item       Tone=Default|Go|Danger, State=Default|Disabled. props Label, Icon (swap). Controls.kt ActionBarItem
  Action bar            (single) Controls.kt ActionBar
  Step pips             Step=1|2|3|4. ComfortOnboarding.kt StepPips

COMPONENTS / PLAYBACK
  Seek bar              Progress=Start|Playing|End. props Elapsed, Total. NowPlayingParts.kt SeekBar
  Skip button           Direction=Previous|Next. NowPlayingParts.kt SkipButton
  Transport             Playing=False|True. NowPlayingParts.kt Transport
  Toggles               (single) shuffle, repeat, heart, queue (nested instances exposed). NowPlayingParts.kt Toggles
  Deck                  Playing=False|True. NowPlayingParts.kt Deck
  Now playing header    (single) props Context. NowPlayingParts.kt NowPlayingHeader
  Now playing cover     Paused=False|True. NowPlayingParts.kt NowPlayingCover
  Now playing title     (single) props Title, Artist. NowPlayingParts.kt NowPlayingTitle
  Now playing lyric     (single) props Line. NowPlayingParts.kt NowPlayingLyric
  Lyrics line           State=Sung|Current|Upcoming|Blank. props Line. ComfortLyrics.kt SyncedLine
  Lyrics card           State=Synced|Plain|Instrumental|Not downloaded|Not found|Downloading.
                        NowPlayingParts.kt LyricsCard, LyricsLine, LyricsMessage (ComfortNowPlaying.kt LyricsSection)
  Queue row             State=Default|Dragging|Current|Swiping. ComfortQueue.kt + Rows.kt SongRow, SwipeToRemove
  Queue clear           (single) ComfortQueue.kt ClearFoot

Every page ends with a `Dark preview` frame (Palette, Tinted, Elevation in Dark) holding instances.
"""
from __future__ import annotations

from figbuild.kit import Doc, V

from .foundations import Sheet, superellipse_path, COVERS_DIR, _modes, ICON_DROP

REFS: dict[str, str] = {}

PKG = "app.attic.player.ui.comfort"
SRC = "ui/comfort/"
COVER = {i: str(p) for i, p in ((int(p.name.split("-")[0]), p) for p in COVERS_DIR.glob("*.jpg"))}
NOW_FRACTION = 78.5 / 232


STORE_SHOTS = "app/src/storeShots/kotlin/app/attic/player/"


def src_path(source: str) -> str:
    """A component's source as a path: a bare file name is in ui/comfort/; paths under app.attic.player (analysis/,
    playback/, widget/, ui/expressive/) and res/ stay as they are; the render harness lives in the storeShots set."""
    if source.startswith("storeShots designrefs "):
        return STORE_SHOTS + "designrefs/" + source[len("storeShots designrefs "):]
    if source.startswith("storeShots "):
        return STORE_SHOTS + "storeshots/" + source[len("storeShots "):]
    if "/" in source.split(" · ")[0]:
        return source
    return SRC + source


def glyph_drop(size: float) -> float:
    """The extra drop to give an icon instance of `size`, beyond what the Icon/* component already draws.

    Compose draws a Material Symbols glyph as text (SymbolIcon: 1em, centred line box), which puts it below the
    box's centre. Measured on the renders: about 2.6dp at 24 (back arrows, more, settings, tabs), 2.3dp at 30 (opsz
    24), 4.2dp at 42 (opsz 40). The Icon/* components carry the 24dp drop themselves (foundations.ICON_DROP, scaling
    with the instance), so this returns only the difference at the larger sizes the kit places by hand."""
    if size >= 40:
        total = size * 0.1
    elif size > 24:
        total = size * 0.077
    else:
        return 0.0
    return round(total - ICON_DROP * size / 24, 2)


def drawable_icon(d: Doc, inst: int, wrap: bool = False):
    """Centre the glyph of an Icon/* instance in its box: for icons Android draws as vector drawables (the widget's
    RemoteViews, the media notification, system chrome), which don't carry the drop Compose's text-drawn symbols have.
    Call it after placing the instance; `wrap` for an instance inside an auto layout frame."""
    n = d.get(inst)
    lift = round(ICON_DROP * n["width"] / 24, 3)
    if wrap:
        # In an auto layout row the instance can't move: frame it (in place) and lift it inside the frame.
        box = d.cmd("object.frameSelection", ids=[inst], name="Icon box")["id"]
        d.set(box, fills=[], clip=False)
        d.set(inst, y=-lift)
    else:
        d.set(inst, y=round(n["y"] - lift, 3))
    return inst


def cover_image(d: Doc, inst: int, path: str, layer: str = "Art"):
    """Put an image on a cover instance (Cover/Album, Cover/Artist: "Art"; Cover/Mosaic: "Art 1".."Art 4")."""
    lid = d.child(inst, layer)
    # Replace the cover's own image paint rather than stacking a second one on it: a stacked
    # paint is an added fill, which the .fig export can't carry as an instance override.
    d.cmd("image.replace", ids=[lid], path=str(path))
    return lid


# ======================================================================== small builders
class K:
    """Shortcuts over Doc for component drawing."""

    def __init__(self, d: Doc, ids: dict):
        self.d, self.ids = d, ids

    def text(self, parent, chars, style, color, name, *, fill=False, lines=None, align=None, w=None):
        kw = {}
        if w:
            kw.update(width=w, textAutoResize="height")
        if align:
            kw["textAlign"] = align
        t = self.d.text(parent, chars, style=style, color=color, name=name, **kw)
        if fill:
            self.d.set(t, sizingH="fill", textAutoResize="height")
        if lines:
            self.d.set(t, maxLines=lines)
        return t

    def icon(self, parent, lig, *, size=24, color="Color/on", filled=False, w400=False, name=None):
        d = self.d
        if w400:
            iid = d.instance(f"Icon/{lig} (filled, 400)", parent, name=name or f"Icon {lig}")
            if size != 24:
                d.set(iid, width=size, height=size)
            d.override(iid, "Glyph", fill=color)
            return iid
        return d.icon(parent, lig, size=size, color=color, filled=filled, name=name)

    def sq(self, parent, n, w, h, fill, name, x=0, y=0):
        return self.d.vector(parent, name=name, path=superellipse_path(n, w, h), x=x, y=y, w=w, h=h, fill=fill,
                             constraints={"horizontal": "scale", "vertical": "scale"})

    def absolute(self, node, x, y, w=None, h=None, constraints=None):
        d = self.d
        d.set(node, absolute=True)
        props = {"x": x, "y": y}
        if w is not None:
            props["width"] = w
        if h is not None:
            props["height"] = h
        d.set(node, **props)
        if constraints:
            d.set(node, constraints=constraints)
        return node

    def bind_radius(self, node, token):
        self.d.bind(node, "cornerRadius", self.ids[f"Dimension/radius/{token}"])


class SetBuilder:
    """Variants of one component set, then combine, properties, description, Code Connect, spec card."""

    def __init__(self, cc: "Components", name: str, source: str, code_name: str, notes: str = ""):
        self.cc, self.d, self.name = cc, cc.d, name
        self.source, self.code_name, self.notes = source, code_name, notes
        self.variants: list[tuple[dict, int]] = []
        self.binds: list[tuple[str, str, int, object]] = []  # (prop, type, layer, default/component)
        self.preferred: dict[str, list] = {}
        self.var_texts: dict[str, dict[str, int]] = {}  # variant name -> {text prop: layer}

    def variant(self, props: dict, **frame_kw) -> int:
        nm = ", ".join(f"{k}={v}" for k, v in props.items())
        cid = self.d.component(None, name=nm, **frame_kw)
        self.variants.append((props, cid))
        return cid

    def text(self, prop, layer, default):
        self.binds.append((prop, "text", layer, default))
        if self.variants:
            nm = ", ".join(f"{k}={v}" for k, v in self.variants[-1][0].items())
            self.var_texts.setdefault(nm, {})[prop] = layer

    def bool(self, prop, layer, default):
        self.binds.append((prop, "boolean", layer, default))

    def swap(self, prop, layer, default_component: str, preferred=()):
        self.binds.append((prop, "instanceSwap", layer, default_component))
        if preferred:
            self.preferred[prop] = list(preferred)

    def finish(self, preview: list[dict] | None = None, *, description_extra="") -> int:
        d = self.d
        desc = f"Compose: {src_path(self.source)}." + (f" {description_extra}" if description_extra else "")
        sid = d.combine(self.name, [c for _, c in self.variants], description=desc)
        d.set(sid, fill=V("Color/ground"))
        if d.get(sid)["width"] >= 1599:
            # kit.combine's wrap never wraps while the set hugs its width; fix the width so it does.
            d.set(sid, sizingH="fixed", width=1600)
        _props(d, sid, self.binds, self.preferred)
        d.cmd("codeConnect.set", id=sid, language="compose", importPath=PKG, name=self.code_name)
        self.cc.place(self.name, sid, self.source, self.notes)
        chosen = preview if preview is not None else [p for p, _ in self.variants][:6]
        self.cc.preview.append((self.name, [dict(p, **self.texts_of(p)) for p in chosen]))
        return sid

    def texts_of(self, props: dict) -> dict:
        """The variant's own text, for instances switched to it (a text property keeps its value across a switch)."""
        nm = ", ".join(f"{k}={v}" for k, v in props.items())
        return {p: self.d.get(l)["characters"] for p, l in self.var_texts.get(nm, {}).items()}


def _props(d: Doc, owner: int, binds, preferred=None):
    preferred = preferred or {}
    done = {}
    for prop, typ, layer, default in binds:
        if prop not in done:
            args = {"id": owner, "name": prop, "type": typ}
            if typ == "instanceSwap":
                args["default"] = d.comp[default]
                if prop in preferred:
                    args["preferred"] = [d.comp[p] for p in preferred[prop]]
            else:
                args["default"] = default
            r = d.cmd("component.addProperty", **args)
            done[prop] = r.get("name", prop) if isinstance(r, dict) else prop
        field = {"text": "characters", "boolean": "visible", "instanceSwap": "mainComponent"}[typ]
        d.cmd("component.bindProperty", layer=layer, property=done[prop], field=field)
    return done


class Components:
    """Lays a component page out as spec cards (heading, source, notes, the set), rows of cards, and the
    Dark preview at the end."""
    ROW_MAX = 4200

    def __init__(self, d: Doc, page: str, intro: str):
        self.d, self.page = d, page
        self.sheet = Sheet(d, page, page.split(" / ")[-1], intro, intro_w=1700)
        self.preview: list[tuple[str, list[dict]]] = []

    def single(self, name, source, code_name, notes="", **frame_kw) -> int:
        cid = self.d.component(None, name=name, description=f"Compose: {src_path(source)}.", **frame_kw)
        return cid

    def finish_single(self, name, cid, source, code_name, notes="", binds=(), preferred=None, preview=True):
        d = self.d
        d.register(name, cid)
        if binds:
            _props(d, cid, binds, preferred)
        d.cmd("codeConnect.set", id=cid, language="compose", importPath=PKG, name=code_name)
        self.place(name, cid, source, notes)
        if preview:
            self.preview.append((name, [{}]))
        return cid

    def place(self, name, node, source, notes):
        s, d = self.sheet, self.d
        w = d.get(node)["width"]
        if s.x > 0 and s.x + w + 96 > self.ROW_MAX:
            s.newrow()
        card = d.frame(None, name=f"Spec: {name}", x=s.x, y=s.y, layout="v", gap=16, pad=48, fill="Canvas/card",
                       radius=32, sizing=("hug", "hug"))
        s.heading(card, name)
        s.label(card, src_path(source), name="Source", style="Doc/Code", muted=True)
        if notes:
            s.label(card, notes, name="Notes", style="Doc/Caption", w=max(560, min(w, 1100)))
        if d.get(node)["type"] == "COMPONENT_SET":
            d.cmd("node.reparent", ids=[node], parent=card)
        else:
            stage = d.frame(card, name="Stage", layout="h", pad=24, fill="Color/ground", radius=12, sizing=("hug", "hug"),
                            stroke="Canvas/component-border", stroke_w=1, dashes=[6, 4])
            d.cmd("node.reparent", ids=[node], parent=stage)
        s.done(card)

    @classmethod
    def extend(cls, d: Doc, page: str) -> "Components":
        """Add spec cards to a page another module laid out: the cards continue its grid on a new row where its Dark
        preview was, and dark_preview() adds their instances to that Dark preview and moves it under them."""
        self = cls.__new__(cls)
        self.d, self.page = d, page
        pid = d.page(page)
        kids = d.get(pid, depth=1).get("children", [])
        self.existing = next((c["id"] for c in kids if c.get("name") == "Dark preview"), None)
        s = Sheet.__new__(Sheet)
        s.d, s.pid = d, pid
        y = d.get(self.existing)["y"] if self.existing else max(c.get("y", 0) + c.get("height", 0) for c in kids) + Sheet.GAP
        s.x, s.y, s.row_h = 0, y, 0
        self.sheet = s
        self.preview = []
        return self

    def dark_preview(self):
        d, s = self.d, self.sheet
        s.newrow()
        if getattr(self, "existing", None):
            f = self.existing
            d.set(f, x=0, y=s.y)
        else:
            f = d.frame(None, name="Dark preview", x=0, y=s.y, w=self.ROW_MAX, layout="h", wrap=True, gap=40, crossGap=40,
                        pad=60, fill="Color/ground", radius=32, sizing=("fixed", "hug"), cross="min")
            _modes(d, f, "Dark")
            d.text(f, "Dark preview", style="Doc/Section title", color="Color/on", name="Title")
        for name, variants in self.preview:
            for props in variants:
                cell = d.frame(f, name=f"Preview: {name}", layout="v", gap=10, sizing=("hug", "hug"))
                iid = d.instance(name, cell, name=name)
                variant = {k: v for k, v in props.items() if v in ("True", "False") or k in _variant_props(d, iid)}
                if props:
                    d.cmd("instance.setProperties", ids=[iid], properties={k: str(v) for k, v in props.items()})
                d.text(cell, name + (" · " + ", ".join(f"{k}={v}" for k, v in variant.items()) if variant else ""),
                       style="Doc/Caption", color="Color/on-2", name="Caption")
        return f


# ======================================================================== build
def build(d: Doc, ctx: dict):
    k = K(d, ctx["tokens"])
    _actions(d, k)
    _lists(d, k)
    _navigation(d, k)
    _playback(d, k)


# ------------------------------------------------------------------------ Actions
BTN = {"Small": (36, 16, "Component/Button small"), "Medium": (48, 22, "Scale/Button"), "Large": (56, 28, "Component/Button large")}
BTN_COLORS = {"Filled": ("Color/accent", "Color/on-accent"), "Tonal": ("Color/accent-soft", "Color/on-accent-soft"),
              "Plain": ("Color/fill", "Color/on"), "Text": (None, "Color/accent-ink")}


def make_button(d, k, sb, style, size, state="Default", danger=False, label="Play", lead="play_arrow", lead_filled=True,
                show_lead=False):
    h, pad, ts = BTN[size]
    if style == "Text":
        pad = 12
    bg, ink = BTN_COLORS[style]
    if danger:
        ink = "Color/danger"
    c = sb.variant({"Style": style, "Size": size, "State": state, "Tone": "Danger" if danger else "Default"}, layout="h", h=h,
                   pad=[0, pad, 0, pad], fill=bg, main="center", cross="center", sizing=("hug", "fixed"))
    d.set(c, minWidth=58)
    k.bind_radius(c, "pill")
    if state == "Disabled":
        d.set(c, opacity=0.38)
    lead_box = d.frame(c, name="Leading", w=28, h=24)
    ic = k.icon(lead_box, lead, filled=lead_filled, color=ink, name="Leading icon")
    d.set(ic, x=-4, y=0)
    d.set(lead_box, visible=show_lead)
    t = k.text(c, label, ts, ink, "Label")
    sb.text("Label", t, label)
    sb.bool("Show leading icon", lead_box, show_lead)
    sb.swap("Leading icon", ic, f"Icon/{lead}" + (" (filled)" if lead_filled else ""))
    return c


def _actions(d: Doc, k: K):
    cc = Components(d, "Components / Actions",
                    "Buttons are pills; tonal toggles and the play button are superellipses. Amber marks the one "
                    "filled button on a screen, switches, progress and selection. Disabled is 38 % opacity (35 % for a "
                    "bare icon button). Source: ui/comfort/Buttons.kt, Bars.kt, Controls.kt.")

    # -- Button
    sb = SetBuilder(cc, "Button", "Buttons.kt · ComfortButton", "ComfortButton",
                    "A pill on Material's Button: Small 36 (padding 16, 14/20), Medium 48 (22, 15.5/20), Large 56 (28, "
                    "17/20), all semibold; Text style pads 12. The leading icon sits 4dp left of the content box "
                    "(offset(x = -4.dp)) then 4dp before the label, as the code draws it. Min width 58.")
    for style in ("Filled", "Tonal", "Plain", "Text"):
        for size in ("Small", "Medium", "Large"):
            make_button(d, k, sb, style, size)
    for style in ("Filled", "Tonal", "Plain", "Text"):
        make_button(d, k, sb, style, "Medium", state="Disabled")
    for style in ("Plain", "Text"):
        make_button(d, k, sb, style, "Medium", danger=True)
    sb.finish([{"Style": s, "Size": "Medium", "State": "Default", "Tone": "Default"} for s in ("Filled", "Tonal", "Plain", "Text")]
              + [{"Style": "Text", "Size": "Medium", "State": "Default", "Tone": "Danger"}])

    # -- Icon button
    sb = SetBuilder(cc, "Icon button", "Buttons.kt · ComfortIconButton", "ComfortIconButton",
                    "A bare icon with a 48dp target. Dim takes on-2; otherwise the ink around it (on). Disabled 35 %.")
    for tone in ("Default", "Dim"):
        for state in ("Default", "Disabled"):
            c = sb.variant({"Tone": tone, "State": state}, w=48, h=48, layout="h", main="center", cross="center")
            if state == "Disabled":
                d.set(c, opacity=0.35)
            ic = k.icon(c, "more_vert", color="Color/on-2" if tone == "Dim" else "Color/on", name="Icon")
            sb.swap("Icon", ic, "Icon/more_vert")
    sb.finish()

    # -- Tonal icon button
    sb = SetBuilder(cc, "Tonal icon button", "Buttons.kt · TonalIconButton", "TonalIconButton",
                    "The icon in a superellipse (n = 4), 48dp or 54dp (large). Checked takes the accent with on-accent "
                    "ink and a filled glyph. Container replaces fill while off: Fill 2 on a dark sheet, Card on light "
                    "ground, Ground toggle for the Play row (card light, fill dark).")
    conts = {"Fill": "Color/fill", "Fill 2": "Color/fill-2", "Card": "Color/card", "Ground toggle": "Color/derived/ground-toggle"}
    combos = [(s, ch, "Default", "Fill") for s in ("Medium", "Large") for ch in (False, True)]
    combos += [(s, False, "Disabled", "Fill") for s in ("Medium", "Large")]
    combos += [("Large", False, "Default", c) for c in ("Fill 2", "Card", "Ground toggle")]
    for size, checked, state, cont in combos:
        side = 54 if size == "Large" else 48
        c = sb.variant({"Size": size, "Checked": str(checked), "State": state, "Container": cont}, w=side, h=side)
        if state == "Disabled":
            d.set(c, opacity=0.38)
        k.sq(c, 4, side, side, "Color/accent" if checked else conts[cont], "Container")
        ic = k.icon(c, "shuffle", filled=checked, color="Color/on-accent" if checked else "Color/on", name="Icon")
        d.set(ic, x=(side - 24) / 2, y=(side - 24) / 2, constraints={"horizontal": "center", "vertical": "center"})
        sb.swap("Icon", ic, "Icon/shuffle (filled)" if checked else "Icon/shuffle")
    sb.finish([{"Size": "Large", "Checked": "False", "State": "Default", "Container": "Fill"},
               {"Size": "Large", "Checked": "True", "State": "Default", "Container": "Fill"},
               {"Size": "Large", "Checked": "False", "State": "Default", "Container": "Ground toggle"}])

    # -- Heart button
    sb = SetBuilder(cc, "Heart button", "Buttons.kt · HeartButton", "HeartButton",
                    "The favourite. Bare: an outline in the ink around it, filled accent when on. Tonal: the deck's 54dp "
                    "toggle, accent fill when on.")
    for tonal in (False, True):
        for fav in (False, True):
            if tonal:
                c = sb.variant({"Tonal": "True", "Favourite": str(fav)}, w=54, h=54)
                k.sq(c, 4, 54, 54, "Color/accent" if fav else "Color/fill", "Container")
                ic = k.icon(c, "favorite", filled=fav, color="Color/on-accent" if fav else "Color/on", name="Icon")
                d.set(ic, x=15, y=15)
            else:
                c = sb.variant({"Tonal": "False", "Favourite": str(fav)}, w=48, h=48, layout="h", main="center", cross="center")
                k.icon(c, "favorite", filled=fav, color="Color/accent" if fav else "Color/on", name="Icon")
    sb.finish()

    # -- Play button
    sb = SetBuilder(cc, "Play button", "Buttons.kt · PlayButton", "PlayButton",
                    "The accent in a superellipse with a filled play or pause glyph at weight 400 in on-accent: Large 76 "
                    "(glyph 42), Medium 56 (32), Small 44 (26).")
    for size, (side, g) in {"Large": (76, 42), "Medium": (56, 32), "Small": (44, 26)}.items():
        for playing in (False, True):
            c = sb.variant({"Size": size, "Playing": str(playing)}, w=side, h=side)
            k.sq(c, 4, side, side, "Color/accent", "Container")
            ic = k.icon(c, "pause" if playing else "play_arrow", size=g, w400=True, color="Color/on-accent", name="Icon")
            d.set(ic, x=(side - g) / 2, y=(side - g) / 2 + glyph_drop(g))
    sb.finish()

    # -- Collection actions
    sb = SetBuilder(cc, "Collection actions", "Buttons.kt · CollectionActions", "CollectionActions",
                    "A collection's Play row (ComfortCollections.kt PlayRow): the filled Play button taking the width (Pause "
                    "while this collection plays), then 54dp tonal toggles on the ground-toggle container (card light, fill "
                    "dark): shuffle and add to playlist. 16dp sides, 20 above, 4 below, 10 between.")
    for playing in (False, True):
        c = sb.variant({"Playing": str(playing)}, layout="h", w=411, pad=[20, 16, 4, 16], gap=10, cross="center",
                       sizing=("fixed", "hug"))
        b = d.instance("Button", c, name="Play")
        d.cmd("instance.setProperties", ids=[b], properties={"Style": "Filled", "Size": "Medium", "State": "Default", "Tone": "Default",
                                                             "Label": "Pause" if playing else "Play", "Show leading icon": True})
        _swap_icon(d, b, "Icon/pause (filled)" if playing else "Icon/play_arrow (filled)", "Leading icon")
        d.set(b, sizingH="fill")
        for nm, lig in (("Shuffle", "shuffle"), ("Add to playlist", "playlist_add")):
            t = d.instance("Tonal icon button", c, name=nm)
            d.cmd("instance.setProperties", ids=[t], properties={"Size": "Large", "Checked": "False", "State": "Default",
                                                                 "Container": "Ground toggle"})
            _swap_icon(d, t, f"Icon/{lig}")
    sb.finish([{"Playing": "False"}])

    # -- Sub-tab
    sb = SetBuilder(cc, "Sub-tab", "Bars.kt · SubTabs", "SubTabs",
                    "One option of Bars.kt SubTabs (Home's filters, Recap's periods); Sub-tab row (Components / Navigation) is the whole "
                    "row of four. 48dp tall, "
                    "10dp sides; selected is bold on with a 3dp accent underline inset 10 and 7 above the bottom.")
    for sel in (False, True):
        c = sb.variant({"Selected": str(sel)}, layout="h", h=48, pad=[0, 10, 0, 10], cross="center", sizing=("hug", "fixed"))
        t = k.text(c, "All", "Component/Sub-tab selected" if sel else "Component/Sub-tab", "Color/on" if sel else "Color/on-2", "Label")
        sb.text("Label", t, "All")
        u = d.rect(c, name="Underline", w=10, h=3, fill="Color/accent", radius=2)
        k.absolute(u, 10, 38, w=d.get(c)["width"] - 20, constraints={"horizontal": "leftRight", "vertical": "bottom"})
        d.set(u, visible=sel)
    sb.finish()

    # -- Chip
    sb = SetBuilder(cc, "Chip", "Bars.kt · FilterChip", "FilterChip",
                    "A filter that can be switched off (Library). 36dp pill, 16dp sides. Off: card on the ground, fill on a "
                    "card, sheet or dialog. On: accent-soft with an 18dp tick 6dp before the label, both drawn 4dp left "
                    "(offset(x = -4.dp)), so the pill is 4dp wider than its parts and the space shows at the end.")
    for sel in (False, True):
        for cont in ("Ground", "Card"):
            bg = "Color/accent-soft" if sel else ("Color/card" if cont == "Ground" else "Color/fill")
            c = sb.variant({"Selected": str(sel), "Container": cont}, layout="h", h=36,
                           pad=[0, 20, 0, 12] if sel else [0, 16, 0, 16], gap=6, cross="center", fill=bg,
                           sizing=("hug", "fixed"))
            k.bind_radius(c, "pill")
            if sel:
                k.icon(c, "check", size=18, color="Color/on-accent-soft", name="Tick")
            t = k.text(c, "Albums", "Component/Chip selected" if sel else "Component/Chip",
                       "Color/on-accent-soft" if sel else "Color/on", "Label")
            sb.text("Label", t, "Albums")
    sb.finish()

    # -- Chip row
    c = cc.single("Chip row", "Bars.kt · ChipRow", "ChipRow", layout="h", gap=8, pad=[6, 16, 6, 16], w=411, cross="center",
                  sizing=("fixed", "hug"))
    for i, lab in enumerate(("Playlists", "Albums", "Artists", "Songs")):
        iid = d.instance("Chip", c, name=f"Chip: {lab}")
        _set_text_prop(d, iid, "Label", lab)
    cc.finish_single("Chip row", c, "Bars.kt · ChipRow", "ChipRow", "Library's filters: a sideways-scrolling row, 8dp apart, 16dp in.")

    # -- Switch
    sb = SetBuilder(cc, "Switch", "Controls.kt · ComfortSwitch", "ComfortSwitch",
                    "A 52 x 30 pill track (fill-2 off, accent on) with a 24dp knob that carries Shadow/Small. Drawn by the "
                    "kit: Material's knob is 16dp when off. Colour and knob slide over 160 ms.")
    for checked in (False, True):
        for state in ("Default", "Disabled"):
            c = sb.variant({"Checked": str(checked), "State": state}, w=52, h=30, fill="Color/accent" if checked else "Color/fill-2")
            k.bind_radius(c, "pill")
            if state == "Disabled":
                d.set(c, opacity=0.38)
            kn = d.ellipse(c, name="Knob", w=24, h=24, x=25 if checked else 3, y=3, fill="Color/knob")
            d.cmd("style.apply", ids=[kn], style=d.effect_styles["Shadow/Small"])
    sb.finish()

    # -- Radio
    sb = SetBuilder(cc, "Radio", "Controls.kt · ComfortRadio", "ComfortRadio",
                    "A 24dp ring, 2dp, in on-3; selected: accent with a 12dp accent dot. One of the kit's three circles.")
    for sel in (False, True):
        c = sb.variant({"Selected": str(sel)}, w=24, h=24, stroke="Color/accent" if sel else "Color/on-3", stroke_w=2,
                       strokeAlign="inside", radius=12)
        dot = d.ellipse(c, name="Dot", w=12, h=12, x=6, y=6, fill="Color/accent")
        d.set(dot, visible=sel)
    sb.finish()

    # -- Progress
    sb = SetBuilder(cc, "Progress", "Controls.kt · ComfortProgress", "ComfortProgress",
                    "A 10dp capsule (16 thick) in fill-2 that fills with the accent. Progress is never a thin line in "
                    "Comfort. Resize the Fill layer's width for the value.")
    for thick in (False, True):
        h = 16 if thick else 10
        c = sb.variant({"Thick": str(thick)}, w=320, h=h, fill="Color/fill-2", clip=True)
        k.bind_radius(c, "pill")
        f = d.rect(c, name="Fill", w=128, h=h, fill="Color/accent", radius=h / 2)
    sb.finish()

    # -- Spinner
    sb = SetBuilder(cc, "Spinner", "Controls.kt · Spinner", "Spinner",
                    "A ring in fill-2, half of it in the accent, turning once every 900 ms: Default 36dp with a 4dp stroke, "
                    "Small 22 with 3, Large 44 with 4 (Spinner(Modifier.size(44.dp)): first scan, loading; the ring grows, "
                    "the stroke doesn't). Drawn here at its resting 28 degrees.")
    for size, (side, stroke) in {"Default": (36, 4), "Small": (22, 3), "Large": (44, 4)}.items():
        c = sb.variant({"Size": size}, w=side, h=side)
        r = (side - stroke) / 2
        cx = cy = side / 2
        d.ellipse(c, name="Track", w=side - stroke, h=side - stroke, x=stroke / 2, y=stroke / 2, fill="none",
                  stroke=V("Color/fill-2"), strokeWeight=stroke)
        import math
        a0, a1 = math.radians(225 + 28), math.radians(225 + 28 + 180)
        p0 = (cx + r * math.cos(a0), cy + r * math.sin(a0))
        pm = (cx + r * math.cos((a0 + a1) / 2), cy + r * math.sin((a0 + a1) / 2))
        p1 = (cx + r * math.cos(a1), cy + r * math.sin(a1))
        arc = f"M{p0[0]:.3f} {p0[1]:.3f} " + _arc_cubic(cx, cy, r, 225 + 28, 225 + 28 + 180)
        v = d.vector(c, name="Arc", path=arc, stroke="Color/accent", stroke_w=stroke)
        xs = [cx + r * math.cos(math.radians(a)) for a in range(253, 434)]
        ys = [cy + r * math.sin(math.radians(a)) for a in range(253, 434)]
        d.set(v, x=min(xs), y=min(ys))
    sb.finish()

    # -- Search field
    sb = SetBuilder(cc, "Search field", "Bars.kt · SearchField", "SearchField",
                    "A 50dp capsule, card on the ground (fill on a card or in a sheet): the search glyph in on-2, the "
                    "query (placeholder in on-3), and once there is text a dim clear button. The caret is the accent. "
                    "Width fills its parent minus 16dp a side.")
    for state in ("Empty", "Query"):
        for cont in ("Ground", "Card"):
            c = sb.variant({"State": state, "Container": cont}, layout="h", w=379, h=50, pad=[0, 4, 0, 18], gap=12,
                           cross="center", fill="Color/card" if cont == "Ground" else "Color/fill", sizing=("fixed", "fixed"))
            k.bind_radius(c, "pill")
            k.icon(c, "search", color="Color/on-2", name="Search icon")
            if state == "Empty":
                t = k.text(c, "Search your music", "Component/Search", "Color/on-3", "Placeholder", fill=True, lines=1)
                sb.text("Placeholder", t, "Search your music")
                d.frame(c, name="End space", w=4, h=4)
            else:
                t = k.text(c, "lanterns", "Component/Search", "Color/on", "Query", fill=True, lines=1)
                sb.text("Query", t, "lanterns")
                cb = d.instance("Icon button", c, name="Clear")
                d.cmd("instance.setProperties", ids=[cb], properties={"Tone": "Dim"})
                _swap_icon(d, cb, "Icon/close")
    sb.finish()

    cc.dark_preview()


def _variant_props(d: Doc, inst: int) -> set:
    return {p["name"] for p in d.cmd("component.properties", id=inst).get("properties", []) if p["type"] == "variant"}


def _set_text_prop(d: Doc, inst: int, prop: str, value: str):
    """Set a text component property by its display name (FigCraft may suffix property names)."""
    props = d.cmd("component.properties", id=inst)
    for p in props.get("properties", []):
        if p["name"].split("#")[0] == prop:
            d.cmd("instance.setProperties", ids=[inst], properties={p["name"]: value})
            return
    raise KeyError(f"{prop} on {inst}")


def _swap_icon(d: Doc, inst: int, icon_name: str, prop: str = "Icon"):
    props = d.cmd("component.properties", id=inst)
    for p in props.get("properties", []):
        if p["name"].split("#")[0] == prop:
            d.cmd("instance.setProperties", ids=[inst], properties={p["name"]: d.comp[icon_name]})
            return
    raise KeyError(f"{prop} on {inst}")


# ------------------------------------------------------------------------ Lists and cards
def _cover(d, k, parent, size, *, kind="Album", art="Image", dimmed=False, image=0, name="Cover"):
    """An instance of Cover/<kind> at `size`, with a demo cover image."""
    comp = f"Cover/{kind}"
    iid = d.instance(comp, parent, name=name)
    props = {}
    if kind == "Album":
        props = {"Shape": "Squircle large" if size >= 100 else "Squircle", "Art": art, "Dimmed": str(dimmed)}
    elif kind == "Artist":
        props = {"Art": art}
    elif kind == "Mosaic":
        props = {"Shape": "Squircle large" if size >= 100 else "Squircle"}
    if props:
        d.cmd("instance.setProperties", ids=[iid], properties=props)
    d.set(iid, width=size, height=size)
    if not (d.get(parent).get("layout") or {}).get("mode"):
        d.set(iid, x=0, y=0)
    return iid


def _lists(d: Doc, k: K):
    cc = Components(d, "Components / Lists and cards",
                    "Rows sit in groups (cards with radius 26 and no shadow) and draw their own hairline from where their "
                    "text starts. Every image and icon container is a superellipse: n = 4 under 100dp, n = 5 from 100dp, "
                    "n = 2.6 for artists. Source: ui/comfort/Artwork.kt, Rows.kt, Groups.kt, Tiles.kt, Controls.kt.")

    # -- Cover/Album
    sb = SetBuilder(cc, "Cover/Album", "Artwork.kt · Cover", "Cover",
                    "A superellipse cover: Squircle (n = 4) below 100dp, Squircle large (n = 5) from 100dp; pick the variant "
                    "that matches the size you draw. Art=None is the neutral note box. Dimmed is the current song's 60 % "
                    "black for the playing bars. Put the image on the Art layer (cover_image()).")
    for shape, n, side in (("Squircle", 4, 56), ("Squircle large", 5, 196)):
        for art, dim in (("Image", False), ("None", False), ("Image", True)):
            c = sb.variant({"Shape": shape, "Art": art, "Dimmed": str(dim)}, w=side, h=side)
            a = k.sq(c, n, side, side, "Color/fill", "Art")
            if art == "Image":
                d.cmd("image.place", path=COVER[0], target=a, mode="fill")
            else:
                g = side * 0.46
                ic = k.icon(c, "music_note", size=g, color="Color/on-2", name="No art")
                d.set(ic, x=(side - g) / 2, y=(side - g) / 2, constraints={"horizontal": "scale", "vertical": "scale"})
            if dim:
                k.sq(c, n, side, side, "Color/derived/art-dim", "Dim")
    sb.finish()

    sb = SetBuilder(cc, "Cover/Artist", "Artwork.kt · ArtistCover", "ArtistCover",
                    "An artist: the rounder superellipse (n = 2.6) at every size. Never a circle.")
    for art in ("Image", "None"):
        c = sb.variant({"Art": art}, w=56, h=56)
        a = k.sq(c, 2.6, 56, 56, "Color/fill", "Art")
        if art == "Image":
            d.cmd("image.place", path=COVER[0], target=a, mode="fill")
        else:
            ic = k.icon(c, "music_note", size=25.76, color="Color/on-2", name="No art")
            d.set(ic, x=15.12, y=15.12, constraints={"horizontal": "scale", "vertical": "scale"})
    sb.finish()

    sb = SetBuilder(cc, "Cover/Mosaic", "Artwork.kt · Mosaic", "Mosaic",
                    "A folder or playlist: four covers from different albums in a 2 x 2 grid inside the superellipse "
                    "(fewer than four: use Cover/Album). Images go on Art 1 to Art 4.")
    for shape, n, side in (("Squircle", 4, 56), ("Squircle large", 5, 132)):
        c = sb.variant({"Shape": shape}, w=side, h=side)
        m = k.sq(c, n, side, side, "Color/fill", "Mask")
        d.set(m, isMask=True)
        for i, img in enumerate((8, 10, 3, 6)):
            r = d.rect(c, name=f"Art {i + 1}", w=side / 2, h=side / 2, x=(i % 2) * side / 2, y=(i // 2) * side / 2,
                       fill="Color/fill", constraints={"horizontal": "scale", "vertical": "scale"})
            d.cmd("image.place", path=COVER[img], target=r, mode="fill")
    sb.finish()

    sb = SetBuilder(cc, "Icon box", "Artwork.kt · IconBox", "IconBox",
                    "One icon at 46 % of a superellipse: Tonal (accent-soft, accent-ink; Favourites' filled heart), Neutral "
                    "(fill, on-2: placeholders, empty states at 76dp), Solid (accent, on-accent).")
    for style, bg, ink, lig, filled in (("Tonal", "Color/accent-soft", "Color/accent-ink", "favorite", True),
                                        ("Neutral", "Color/fill", "Color/on-2", "music_note", False),
                                        ("Solid", "Color/accent", "Color/on-accent", "shuffle", False)):
        c = sb.variant({"Style": style}, w=56, h=56)
        k.sq(c, 4, 56, 56, bg, "Container")
        ic = k.icon(c, lig, size=25.76, filled=filled, color=ink, name="Icon")
        d.set(ic, x=15.12, y=15.12, constraints={"horizontal": "scale", "vertical": "scale"})
        sb.swap("Icon", ic, f"Icon/{lig}" + (" (filled)" if filled else ""))
    sb.finish()

    # -- Playing bars
    sb = SetBuilder(cc, "Playing bars", "Rows.kt · PlayingBars", "PlayingBars",
                    "Three bars at 50, 100 and 68 % that bounce while playing; still, all three rest at 26 %. Row 16dp "
                    "(3dp bars, 2.5 gap); Tile 22dp (4dp, 3.5). In on-art over a dimmed cover; on-accent-soft in place "
                    "of a track number.")
    for size, (box, bw, gap) in {"Row": (16, 3, 2.5), "Tile": (22, 4, 3.5)}.items():
        for playing in (False, True):
            c = sb.variant({"Size": size, "Playing": str(playing)}, w=box, h=box)
            x = (box - (bw * 3 + gap * 2)) / 2
            for i, f in enumerate((0.5, 1.0, 0.68) if playing else (0.26,) * 3):
                h = box * f
                d.rect(c, name=f"Bar {i + 1}", x=x, y=box - h, w=bw, h=h, fill="Color/on-art", radius=2,
                       constraints={"horizontal": "scale", "vertical": "scale"})
                x += bw + gap
    sid = sb.finish()
    d.set(sid, fill=V("Color/toast"))  # the bars are on-art (white): show them on a dark stage

    # -- Song row trailing
    sb = SetBuilder(cc, "Song row trailing", "Rows.kt · SongMoreButton, SongDragHandle, SongAddButton, SongRadioMark",
                    "SongMoreButton", "What ends a song row, in a 48dp target. More, the drag handle and the radio mark take "
                    "the row's ink (on-3, or on-accent-soft on the current row); Add is on, Added is accent-ink.")
    for kind, lig, tones in (("More", "more_vert", ("Default", "Current")), ("Drag handle", "drag_handle", ("Default", "Current")),
                             ("Radio", "sensors", ("Default", "Current")), ("Add", "add", ("Default",)), ("Added", "check", ("Default",))):
        for tone in tones:
            col = {"Add": "Color/on", "Added": "Color/accent-ink"}.get(kind, "Color/on-accent-soft" if tone == "Current" else "Color/on-3")
            c = sb.variant({"Kind": kind, "Tone": tone}, w=48, h=48, layout="h", main="center", cross="center")
            k.icon(c, lig, color=col, name="Icon")
    sb.finish()

    # -- Song row
    sb = SetBuilder(cc, "Song row", "Rows.kt · SongRow", "SongRow",
                    "64dp, 8dp above and below, 14dp between parts, 4dp at the end. Current (`.song.now`): an accent-soft bed "
                    "inset 6 and 4 with radius 18, a bold title, the second line in on-accent-soft, and the bars over the "
                    "dimmed cover or in place of the number. Rank is Recap's `.song.ranked` (the rank before a cover); Rank artist the same before an artist picture (top artists). Show hairline on every row after the first in a group: it runs "
                    "from where the text starts (78, 58 numbered, 112 ranked, 20 bare) to 20dp from the end. In a sheet "
                    "pad the start to 24.")
    seps = {"Cover": 78, "Number": 58, "Artist": 78, "Rank": 112, "Rank artist": 112, "None": 20}
    for lead in ("Cover", "Number", "Artist", "Rank", "Rank artist", "None"):
        for cur in (False, True):
            start = 20 if lead == "None" else 16
            c = sb.variant({"Lead": lead, "Current": str(cur)}, layout="h", w=379, pad=[8, 4, 8, start], gap=14,
                           cross="center", sizing=("fixed", "hug"))
            d.set(c, minHeight=64)
            bed = d.rect(c, name="Bed", w=367, h=56, fill="Color/accent-soft")
            k.bind_radius(bed, "control")
            k.absolute(bed, 6, 4, 367, 56, {"horizontal": "leftRight", "vertical": "topBottom"})
            d.set(bed, visible=cur)
            hl = d.rect(c, name="Hairline", w=379 - seps[lead] - 20, h=1, fill="Color/line")
            k.absolute(hl, seps[lead], 0, 379 - seps[lead] - 20, 1, {"horizontal": "leftRight", "vertical": "top"})
            d.set(hl, visible=False)
            sb.bool("Show hairline", hl, False)
            if lead in ("Rank", "Rank artist"):
                t = k.text(c, "1", "Component/Row rank", "Color/on-2", "Rank", w=20, align="center")
                sb.text("Number", t, "1")
            if lead in ("Cover", "Rank"):
                box = d.frame(c, name="Lead", w=48, h=48)
                _cover(d, k, box, 48, dimmed=cur, name="Cover")
                if cur:
                    b = d.instance("Playing bars", box, name="Playing bars", x=16, y=16)
                    d.cmd("instance.setProperties", ids=[b], properties={"Playing": "True"})
            elif lead == "Rank artist":
                box = d.frame(c, name="Lead", w=48, h=48)
                _cover(d, k, box, 48, kind="Artist", name="Cover")
            elif lead == "Artist":
                _cover(d, k, c, 48, kind="Artist", name="Lead")
            elif lead == "Number":
                box = d.frame(c, name="Lead", w=28, h=20, layout="h", main="center", cross="center")
                if cur:
                    b = d.instance("Playing bars", box, name="Playing bars")
                    d.cmd("instance.setProperties", ids=[b], properties={"Playing": "True"})
                    for i in (1, 2, 3):
                        d.override(b, f"Bar {i}", fill="Color/on-accent-soft")
                else:
                    t = k.text(box, "1", "Component/Row number", "Color/on-2", "Number", align="center")
                    sb.text("Number", t, "1")
            txt = d.frame(c, name="Labels", layout="v", sizing=("fill", "hug"))
            t1 = k.text(txt, "Porch Light", "Component/Row title current" if cur else "Scale/Row title", "Color/on", "Title", fill=True, lines=1)
            t2 = k.text(txt, "Mira Okafor", "Scale/Sub", "Color/on-accent-soft" if cur else "Color/on-2", "Subtitle", fill=True, lines=1)
            sb.text("Title", t1, "Porch Light")
            sb.text("Subtitle", t2, "Mira Okafor")
            sb.bool("Show subtitle", t2, True)
            mt = k.text(c, "3:21", "Component/Row meta", "Color/on-accent-soft" if cur else "Color/on-2", "Meta")
            d.set(mt, visible=False)
            sb.text("Meta", mt, "3:21")
            sb.bool("Show meta", mt, False)
            tr = d.instance("Song row trailing", c, name="Trailing")
            d.cmd("instance.setProperties", ids=[tr], properties={"Kind": "More", "Tone": "Current" if cur else "Default"})
            d.cmd("component.exposeInstance", id=tr)
            sb.bool("Show trailing", tr, True)
    sb.finish([{"Lead": "Cover", "Current": "False"}, {"Lead": "Cover", "Current": "True"},
               {"Lead": "Number", "Current": "True"}])

    # -- Collection row
    sb = SetBuilder(cc, "Collection row", "Rows.kt · CollectionRow", "CollectionRow",
                    "A collection in a list. Small 64dp with 48dp art, Medium 72 with 56, Large 92 with 64 and an 18/24 bold "
                    "title. Padding 16 start, 10 end, 8 above and below; 14 between. Trailing value (tabular, on-2) and/or "
                    "chevron (on-3). Hairline from 78, 86 or 94. Album: Cover/Album; Artist: Cover/Artist; Playlist and "
                    "Folder: Cover/Mosaic; Favourites: the tonal Icon box.")
    sizes = {"Small": (64, 48, 78), "Medium": (72, 56, 86), "Large": (92, 64, 94)}
    kinds = {"Album": ("Paper Lanterns", "Mira Okafor · 2025"), "Artist": ("Mira Okafor", "2 albums"),
             "Playlist": ("Long drive", "25 songs"), "Folder": ("English", "24 songs"), "Favourites": ("Favourites", "42 songs")}
    for kind in kinds:
        for size, (mh, art, sep) in sizes.items():
            c = sb.variant({"Kind": kind, "Size": size}, layout="h", w=379, pad=[8, 10, 8, 16], gap=14, cross="center",
                           sizing=("fixed", "hug"))
            d.set(c, minHeight=mh)
            hl = d.rect(c, name="Hairline", w=379 - sep - 20, h=1, fill="Color/line")
            k.absolute(hl, sep, 0, 379 - sep - 20, 1, {"horizontal": "leftRight", "vertical": "top"})
            d.set(hl, visible=False)
            sb.bool("Show hairline", hl, False)
            if kind == "Favourites":
                lead = d.instance("Icon box", c, name="Lead")
                d.set(lead, width=art, height=art)
            else:
                lead = _cover(d, k, c, art, kind={"Album": "Album", "Artist": "Artist"}.get(kind, "Mosaic"), name="Lead")
            txt = d.frame(c, name="Labels", layout="v", sizing=("fill", "hug"))
            ov = k.text(txt, "More from", "Scale/Caption", "Color/on-2", "Overline", fill=True, lines=1)
            d.set(ov, visible=False)
            t1 = k.text(txt, kinds[kind][0], "Scale/Row title large" if size == "Large" else "Scale/Row title", "Color/on", "Title", fill=True, lines=1)
            t2 = k.text(txt, kinds[kind][1], "Scale/Sub", "Color/on-2", "Subtitle", fill=True, lines=1)
            sb.text("Overline", ov, "More from")
            sb.bool("Show overline", ov, False)
            sb.text("Title", t1, kinds[kind][0])
            sb.text("Subtitle", t2, kinds[kind][1])
            sb.bool("Show subtitle", t2, True)
            vf = d.frame(c, name="Value", layout="h", pad=[0, 10, 0, 0], sizing=("hug", "hug"))
            vt = k.text(vf, "12", "Component/Row value", "Color/on-2", "Value text")
            d.set(vf, visible=False)
            sb.text("Value", vt, "12")
            sb.bool("Show value", vf, False)
            cf = d.frame(c, name="Chevron", layout="h", pad=[0, 6, 0, 0], sizing=("hug", "hug"))
            k.icon(cf, "chevron_right", color="Color/on-3", name="Chevron icon")
            d.set(cf, visible=False)
            sb.bool("Show chevron", cf, False)
    sb.finish([{"Kind": x, "Size": "Medium"} for x in kinds])

    # -- Settings row
    sb = SetBuilder(cc, "Settings row", "Controls.kt · SetRow, SetSwitchRow, SetRadioRow", "SetRow",
                    "Min 60dp, 20dp sides, 11 above and below, 16 between. Title 16.5/22 medium; summary 13.5/19 on-2 wraps. "
                    "Lead: a radio. Trail: a switch, a value in accent-ink, or a chevron in on-3. Backup's Save and Restore "
                    "rows are Control=None with a summary. Hairline from 60 with a radio, else 20.")
    for ctl in ("None", "Switch off", "Switch on", "Radio off", "Radio on", "Value", "Chevron"):
        radio = ctl.startswith("Radio")
        c = sb.variant({"Control": ctl}, layout="h", w=379, pad=[11, 20, 11, 20], gap=16, cross="center", sizing=("fixed", "hug"))
        d.set(c, minHeight=60)
        sep = 60 if radio else 20
        hl = d.rect(c, name="Hairline", w=379 - sep - 20, h=1, fill="Color/line")
        k.absolute(hl, sep, 0, 379 - sep - 20, 1, {"horizontal": "leftRight", "vertical": "top"})
        d.set(hl, visible=False)
        sb.bool("Show hairline", hl, False)
        if radio:
            r = d.instance("Radio", c, name="Radio")
            d.cmd("instance.setProperties", ids=[r], properties={"Selected": str(ctl == "Radio on")})
        txt = d.frame(c, name="Labels", layout="v", gap=2, sizing=("fill", "hug"))
        t1 = k.text(txt, "Dark" if radio else ("Save a backup" if ctl == "None" else "Lyrics"), "Component/Settings title", "Color/on", "Title", fill=True)
        t2 = k.text(txt, "Songs, playlists, favourites and settings in one file.", "Component/Settings summary", "Color/on-2", "Summary", fill=True)
        d.set(t2, visible=False)
        sb.text("Title", t1, "Save a backup")
        sb.text("Summary", t2, "Songs, playlists, favourites and settings in one file.")
        sb.bool("Show summary", t2, False)
        if ctl.startswith("Switch"):
            s = d.instance("Switch", c, name="Switch")
            d.cmd("instance.setProperties", ids=[s], properties={"Checked": str(ctl == "Switch on")})
        elif ctl == "Value":
            vt = k.text(c, "English", "Component/Settings value", "Color/accent-ink", "Value", lines=1)
            sb.text("Value", vt, "English")
        elif ctl == "Chevron":
            k.icon(c, "chevron_right", color="Color/on-3", name="Chevron")
    sb.finish()

    # -- Settings block
    sb = SetBuilder(cc, "Settings block", "Controls.kt · SetBlock", "SetBlock",
                    "A paragraph and its status inside a settings card, as ComfortSettings.kt fills SetBlock: the paragraph "
                    "(14/20 on-2, bold words in on), small print above the bar (10 above), a 10dp progress capsule (14 above, 8 "
                    "below), small print (6 above) and a text button 12 below, its label lined up with the paragraph "
                    "(offset(x = -12.dp)). Small print is 12.5/17 in on-2, as Settings' BlockSmall draws it (the kit's "
                    "SetBlockSmall would use on-3; no screen calls it). After row: the block follows a row in the same card, "
                    "so its top padding drops from 14 to 2. Small print alone (Backup's note): set its frame's top padding to 0.")
    for after in (False, True):
        c = sb.variant({"After row": str(after)}, layout="v", w=379, pad=[2 if after else 14, 20, 16, 20], sizing=("fixed", "hug"))
        t = k.text(c, "51 of 69 songs have lyrics saved on this phone. Saved lyrics work offline.", "Component/Settings block",
                   "Color/on-2", "Paragraph", fill=True)
        sa = d.frame(c, name="Small print above", layout="v", pad=[10, 0, 0, 0], sizing=("fill", "hug"))
        sat = k.text(sa, "Downloading sends each song's title, artist, album and length to lrclib.net.", "Component/Small print",
                     "Color/on-2", "Small print above text", fill=True)
        d.set(sa, visible=False)
        pr = d.frame(c, name="Progress", layout="v", pad=[14, 0, 8, 0], sizing=("fill", "hug"))
        pi = d.instance("Progress", pr, name="Progress bar")
        d.set(pi, sizingH="fill")
        smf = d.frame(c, name="Small print", layout="v", pad=[6, 0, 0, 0], sizing=("fill", "hug"))
        sm = k.text(smf, "Checked 23 of 69, found 17", "Component/Small print", "Color/on-2", "Small print text", fill=True)
        bt = d.frame(c, name="Button", layout="h", pad=[12, 0, 0, 0], sizing=("hug", "hug"))
        bi = d.instance("Button", bt, name="Text button")
        d.cmd("instance.setProperties", ids=[bi], properties={"Style": "Text", "Size": "Medium", "State": "Default", "Tone": "Default"})
        _set_text_prop(d, bi, "Label", "Download lyrics for all songs")
        d.set(bi, paddingLeft=0)
        sb.text("Text", t, "51 of 69 songs have lyrics saved on this phone. Saved lyrics work offline.")
        sb.bool("Show text", t, True)
        sb.text("Small print above", sat, "Downloading sends each song's title, artist, album and length to lrclib.net.")
        sb.bool("Show small print above", sa, False)
        sb.bool("Show progress", pr, True)
        sb.text("Small print", sm, "Checked 23 of 69, found 17")
        sb.bool("Show small print", smf, True)
        sb.bool("Show button", bt, True)
    sb.finish([{"After row": "False"}])

    # -- Key value
    c = cc.single("Key value", "Rows.kt · KeyValue", "KeyValue", layout="v", w=379, pad=[11, 20, 12, 20], sizing=("fixed", "hug"))
    hl = d.rect(c, name="Hairline", w=339, h=1, fill="Color/line")
    k.absolute(hl, 20, 0, 339, 1, {"horizontal": "leftRight", "vertical": "top"})
    d.set(hl, visible=False)
    a = k.text(c, "Album", "Component/Small print", "Color/on-2", "Label", fill=True)
    b = k.text(c, "Paper Lanterns", "Scale/Row title", "Color/on", "Value", fill=True)
    cc.finish_single("Key value", c, "Rows.kt · KeyValue", "KeyValue",
                     "A label (12.5/17 on-2) over a value (16/22 on): Now Playing's Credits. 20dp sides, 11 above, 12 below. "
                     "Show hairline on every row after the first in a group: hairlineAbove from 20dp to 20dp from the end.",
                     binds=[("Label", "text", a, "Album"), ("Value", "text", b, "Paper Lanterns"),
                            ("Show hairline", "boolean", hl, False)])

    # -- Section label
    sb = SetBuilder(cc, "Section label", "Groups.kt · Label", "Label",
                    "The name of the group below it, on the ground: 14/18 semibold on-2, 28dp in, 24 above (Tight: 14) and 9 "
                    "below. Small follows on the line in on-3; End sits at the right in on-3 (the queue's source, a view toggle).")
    for sp in ("Default", "Tight"):
        c = sb.variant({"Spacing": sp}, layout="h", w=411, pad=[24 if sp == "Default" else 14, 28, 9, 28], gap=8,
                       cross="max", sizing=("fixed", "hug"))
        t = k.text(c, "Start listening", "Scale/Label", "Color/on-2", "Label", lines=1)
        sm = k.text(c, "picked for today", "Component/Label small", "Color/on-3", "Small", lines=1)
        d.set(sm, visible=False)
        end = d.frame(c, name="End", layout="h", gap=4, main="max", cross="center", sizing=("fill", "hug"))
        ei = k.icon(end, "album", size=18, color="Color/on-3", name="End icon")
        sb.swap("End icon", ei, "Icon/album", preferred=("Icon/album", "Icon/sensors", "Icon/person", "Icon/folder"))
        et = k.text(end, "Paper Lanterns", "Scale/Label medium", "Color/on-3", "End text", lines=1)
        d.set(end, visible=False)
        sb.text("Label", t, "Start listening")
        sb.text("Small", sm, "picked for today")
        sb.bool("Show small", sm, False)
        sb.text("End", et, "Paper Lanterns")
        sb.bool("Show end", end, False)
    sb.finish()

    c = cc.single("Note", "Groups.kt · Note", "Note", layout="v", w=411, pad=[10, 28, 0, 28], sizing=("fixed", "hug"))
    t = k.text(c, "Attic reads music from the folders you choose and never leaves this phone.", "Component/Note", "Color/on-3", "Note text", fill=True)
    cc.finish_single("Note", c, "Groups.kt · Note", "Note", "Small print under a group: 13/18 on-3, 28dp in, 10 above.",
                     binds=[("Text", "text", t, "Attic reads music from the folders you choose and never leaves this phone.")])

    # -- Tile
    sb = SetBuilder(cc, "Tile", "Tiles.kt · Tile", "Tile",
                    "A collection as an object: superellipse art with its name 8dp under it (14/18 semibold; centred tiles "
                    "13.5/18 medium) and a 12.5/16 second line 1dp below. Album 132 (a shelf), Recent 112, More from 104, Grid "
                    "112 (a three-column filter grid), Artist 92 (n = 2.6, centred), Shortcut (Home's tray: 80dp art, two "
                    "lines, centred), Folder (four-across tray: 64dp mosaic). Now dims the cover under 22dp bars.")
    tiles = {"Album": (132, 132, "Album", False, 1, True), "Recent": (112, 112, "Album", False, 1, True),
             "More from": (104, 104, "Album", False, 1, True), "Grid": (112, 112, "Album", False, 1, True),
             "Artist": (92, 92, "Artist", True, 1, False), "Shortcut": (114, 80, "Album", True, 2, False),
             "Folder": (81, 64, "Mosaic", True, 1, True)}
    titles = {"Album": ("Paper Lanterns", "Mira Okafor"), "Recent": ("Lights Over the Water", "Mira Okafor"),
              "More from": ("Weather Report", "2022"), "Grid": ("Iravu Rail", "Kavya Raman"), "Artist": ("Kavya Raman", ""),
              "Shortcut": ("Paper Lanterns", ""), "Folder": ("English", "24 songs")}
    for kind, (w, art, ckind, centred, lines, sub) in tiles.items():
        for now in ((False, True) if kind in ("Album", "Recent") else (False,)):
            c = sb.variant({"Kind": kind, "Current": str(now)}, layout="v", w=w, cross="center" if centred else "min",
                           sizing=("fixed", "hug"))
            box = d.frame(c, name="Art", w=art, h=art)
            cv = _cover(d, k, box, art, kind=ckind, dimmed=now, name="Cover")
            if kind == "Shortcut":
                ib = d.instance("Icon box", box, name="Icon box", x=0, y=0)
                d.set(ib, width=art, height=art, visible=False)
                sb.bool("Show icon box", ib, False)
            if kind == "Grid":
                cover_image(d, cv, COVER[8])
            if now:
                b = d.instance("Playing bars", box, name="Playing bars", x=(art - 22) / 2, y=(art - 22) / 2)
                d.cmd("instance.setProperties", ids=[b], properties={"Size": "Tile", "Playing": "True"})
            txt = d.frame(c, name="Labels", layout="v", gap=1, pad=[8, 0, 0, 0], sizing=("fill", "hug"))
            t1 = k.text(txt, titles[kind][0], "Component/Tile title centred" if centred else "Component/Tile title", "Color/on",
                        "Title", fill=True, lines=lines, align="center" if centred else None)
            sb.text("Title", t1, titles[kind][0])
            t2 = k.text(txt, titles[kind][1] or "Mira Okafor", "Scale/Caption", "Color/on-2", "Subtitle", fill=True, lines=1,
                        align="center" if centred else None)
            d.set(t2, visible=sub)
            if sub:
                sb.text("Subtitle", t2, titles[kind][1])
    sb.finish([{"Kind": "Album", "Current": "False"}, {"Kind": "Recent", "Current": "True"}, {"Kind": "Artist", "Current": "False"},
               {"Kind": "Folder", "Current": "False"}])

    # -- Group
    sb = SetBuilder(cc, "Group card", "Groups.kt · Group", "Group",
                    "A card on the ground, radius 26, card colour, no shadow, 16dp in from the screen edges (379 wide on a "
                    "411 phone). Rows go in the Content slot with no gaps; give rows after the first their hairline. Pad is "
                    "`.group.pad` (14 sides, 18 top, 16 bottom) for a grid or tray.")
    for pad in (False, True):
        c = sb.variant({"Pad": str(pad)}, layout="v", w=379, fill="Color/card", clip=True, sizing=("fixed", "hug"),
                       pad=[18, 14, 16, 14] if pad else None)
        k.bind_radius(c, "group")
        slot = d.frame(c, name="Content", layout="v" if not pad else "h", gap=0 if not pad else 6, sizing=("fill", "hug"))
        if pad:
            for i, (title, img) in enumerate((("Favourites", None), ("Paper Lanterns", 0), ("Iravu Rail", 8))):
                t = d.instance("Tile", slot, name=f"Tile: {title}")
                d.cmd("instance.setProperties", ids=[t], properties={"Kind": "Shortcut", "Current": "False"})
                d.set(t, sizingH="fill")
                _set_text_prop(d, t, "Title", title)
                if img is None:
                    d.cmd("instance.setProperties", ids=[t], properties={"Show icon box": True})
                    _set_text_prop(d, t, "Title", "Favourites")
                else:
                    cover_image(d, d.child(t, "Cover"), COVER[img])
        else:
            for i, (title, sub) in enumerate((("Porch Light", "Mira Okafor"), ("Moth Letters", "Mira Okafor"), ("Second Bus Home", "Mira Okafor"))):
                r = d.instance("Song row", slot, name=f"Row: {title}")
                d.set(r, sizingH="fill")
                _set_text_prop(d, r, "Title", title)
                if i:
                    _set_text_prop_bool(d, r, "Show hairline", True)
        d.cmd("component.makeSlot", id=slot, name="Content")
    sb.finish([{"Pad": "False"}])

    # -- Browse tile
    sb = SetBuilder(cc, "Browse tile", "Tiles.kt · BrowseTile", "BrowseTile",
                    "Search's small card: min 128dp, 14dp padding, radius 26, card colour; 56dp art top-left and the name at "
                    "the bottom (16/21 semibold). Tinted is `.browse.tinted.wash`: the variant sets Color mode Tinted wash, "
                    "so it takes a trace of the example cover.")
    for tinted in (False, True):
        c = sb.variant({"Tinted": str(tinted)}, layout="v", w=183, pad=14, fill="Color/card", main="spaceBetween",
                       sizing=("fixed", "hug"))
        d.set(c, minHeight=128)
        k.bind_radius(c, "group")
        if tinted:
            d.set_mode(c, "Color", "Tinted wash")
        _cover(d, k, c, 56, kind="Mosaic" if not tinted else "Album", name="Art")
        txt = d.frame(c, name="Labels", layout="v", pad=[12, 0, 0, 0], sizing=("fill", "hug"))
        t1 = k.text(txt, "English" if not tinted else "Paper Lanterns", "Component/Browse title", "Color/on", "Title", fill=True, lines=1)
        t2 = k.text(txt, "24 songs", "Scale/Caption", "Color/on-2", "Subtitle", fill=True, lines=1)
        sb.text("Title", t1, "English")
        sb.text("Subtitle", t2, "24 songs")
    sb.finish()

    # -- Collection header
    sb = SetBuilder(cc, "Collection header", "Tiles.kt · CollectionHeader", "CollectionHeader",
                    "A collection's header, centred, 32dp sides, 6 above: the art (196 for an album or playlist, 148 for a "
                    "person; sheet 04 draws 176), the title 18 below (26/32 bold; 30/36 for a person, up to three lines), the "
                    "artist link in accent-ink with a chevron (Show byline: albums only; a playlist, folder or Favourites "
                    "has none), and the meta line in on-2, 4 below.")
    for kind in ("Album", "Person"):
        person = kind == "Person"
        c = sb.variant({"Kind": kind}, layout="v", w=411, pad=[6, 32, 0, 32], cross="center", sizing=("fixed", "hug"))
        _cover(d, k, c, 148 if person else 196, kind="Artist" if person else "Album", name="Art")
        tf = d.frame(c, name="Title block", layout="v", pad=[18, 0, 0, 0], cross="center", sizing=("fill", "hug"))
        t = k.text(tf, "Mira Okafor" if person else "Paper Lanterns", "Scale/Person title" if person else "Scale/Title", "Color/on",
                   "Title", fill=True, lines=3, align="center")
        sb.text("Title", t, "Paper Lanterns")
        if not person:
            by = d.frame(c, name="Byline", layout="h", gap=2, pad=[6, 0, 0, 0], cross="center", sizing=("hug", "hug"))
            bt = k.text(by, "Mira Okafor", "Component/Header byline", "Color/accent-ink", "Byline text", lines=1)
            k.icon(by, "chevron_right", size=18, color="Color/accent-ink", name="Chevron")
            sb.text("Byline", bt, "Mira Okafor")
            sb.bool("Show byline", by, True)
        mf = d.frame(c, name="Meta", layout="v", pad=[4, 0, 0, 0], cross="center", sizing=("fill", "hug"))
        m = k.text(mf, "8 songs, 30 min" if person else "Album · 2025", "Component/Header meta", "Color/on-2", "Meta text", fill=True, align="center")
        sb.text("Meta", m, "Album · 2025")
    sb.finish()

    # -- Recap entry
    c = cc.single("Recap entry", "ComfortHome.kt · RecapEntry", "RecapEntry", layout="h", w=379, pad=[8, 10, 8, 16], gap=14,
                  cross="center", fill="Color/card", sizing=("fixed", "hug"))
    d.set(c, minHeight=72)
    k.bind_radius(c, "group")
    m = _cover(d, k, c, 56, kind="Mosaic", name="Mosaic")
    for i, img in enumerate((0, 8, 9, 3)):
        cover_image(d, m, COVER[img], f"Art {i + 1}")
    txt = d.frame(c, name="Labels", layout="v", sizing=("fill", "hug"))
    t1 = k.text(txt, "Your 2026 so far", "Component/Recap entry title", "Color/accent-ink", "Title", fill=True, lines=1)
    t2 = k.text(txt, "18,420 minutes. See your top songs and cards to share.", "Scale/Sub", "Color/on-2", "Body", fill=True)
    cf = d.frame(c, name="Chevron", layout="h", pad=[0, 6, 0, 0], sizing=("hug", "hug"))
    k.icon(cf, "chevron_right", color="Color/on-3", name="Chevron icon")
    cc.finish_single("Recap entry", c, "ComfortHome.kt · RecapEntry", "RecapEntry",
                     "Home's way into Recap: a one-row group with the year's top covers as a mosaic, the headline in accent-ink "
                     "(\"Your 2026 so far\"; \"Your 2026 Recap\" from 24 December) and the minutes line, which wraps.",
                     binds=[("Title", "text", t1, "Your 2026 so far"), ("Body", "text", t2, "18,420 minutes. See your top songs and cards to share.")])

    cc.dark_preview()


def _set_text_prop_bool(d: Doc, inst: int, prop: str, value: bool):
    props = d.cmd("component.properties", id=inst)
    for p in props.get("properties", []):
        if p["name"].split("#")[0] == prop:
            d.cmd("instance.setProperties", ids=[inst], properties={p["name"]: value})
            return
    raise KeyError(f"{prop} on {inst}")


# ------------------------------------------------------------------------ Navigation
def _arc_cubic(cx, cy, r, a0, a1):
    """A circular arc from angle a0 to a1 (degrees, y down, clockwise) as cubic segments of at most 90 degrees."""
    import math
    out = []
    steps = max(1, int(math.ceil(abs(a1 - a0) / 90)))
    da = (a1 - a0) / steps
    for i in range(steps):
        t0, t1 = math.radians(a0 + i * da), math.radians(a0 + (i + 1) * da)
        kk = 4 / 3 * math.tan((t1 - t0) / 4)
        p0 = (cx + r * math.cos(t0), cy + r * math.sin(t0))
        p3 = (cx + r * math.cos(t1), cy + r * math.sin(t1))
        p1 = (p0[0] - kk * r * math.sin(t0), p0[1] + kk * r * math.cos(t0))
        p2 = (p3[0] + kk * r * math.sin(t1), p3[1] - kk * r * math.cos(t1))
        out.append(f"C{p1[0]:.3f} {p1[1]:.3f} {p2[0]:.3f} {p2[1]:.3f} {p3[0]:.3f} {p3[1]:.3f}")
    return " ".join(out)


def _circle(cx, cy, r):
    import math
    return f"M{cx + r:.3f} {cy:.3f} " + _arc_cubic(cx, cy, r, 0, 360) + " Z"


MARK_PATH = ("M0 51.5 L0 20.78 L36 0 L36 51.5 Z " + _circle(18, 33.5, 12) + " " + _circle(18, 33.5, 4.8) + " "
             + _circle(25.8, 15, 3.8))


def _navigation(d: Doc, k: K):
    cc = Components(d, "Components / Navigation",
                    "The dock is one floating card holding the mini player and the tabs; the large title sits centred in an "
                    "empty upper third and becomes a bar once scrolled. Status and gesture bars are drawn as the render "
                    "harness draws them. Source: ui/comfort/Bars.kt, Dock.kt, Controls.kt, ComfortOnboarding.kt.")

    # -- Status bar
    c = cc.single("Status bar", "storeShots ShotHarness.kt · SystemBars", "SystemBars", layout="h", w=411, h=40,
                  pad=[0, 26, 0, 26], cross="center", sizing=("fixed", "fixed"))
    k.text(c, "10:24", "Component/Status time", "Color/system/ink", "Time")
    d.frame(c, name="Spacer", h=1, sizing=("fill", "fixed"))
    g = d.frame(c, name="System icons", w=62, h=14)
    for i in range(4):
        bh = 14 * (0.35 + 0.65 * i / 3)
        d.rect(g, name=f"Signal bar {i + 1}", x=i * 4, y=14 - bh, w=2.8, h=bh, fill="Color/system/ink", radius=1)
    import math
    r = 14 * 1.15
    p0 = (25 + r * math.cos(math.radians(225)), 14 + r * math.sin(math.radians(225)))
    wifi = f"M25 14 L{p0[0]:.3f} {p0[1]:.3f} " + _arc_cubic(25, 14, r, 225, 315) + " Z"
    wv = d.vector(g, name="Wifi", path=wifi, fill="Color/system/ink")
    d.set(wv, x=p0[0], y=14 - r)
    bh = 14 * 0.72
    top = (14 - bh) / 2
    d.rect(g, name="Battery outline", x=40, y=top, w=19, h=bh, radius=2.5, stroke="Color/system/ink", stroke_w=1.2, strokeAlign="center")
    d.rect(g, name="Battery level", x=42, y=top + 2, w=(19 - 4) * 0.8, h=bh - 4, fill="Color/system/ink", radius=1)
    d.rect(g, name="Battery nub", x=60, y=5, w=1.6, h=4, fill="Color/system/ink", radius=1)
    cc.finish_single("Status bar", c, "storeShots ShotHarness.kt · SystemBars", "SystemBars",
                     "The render harness's status bar (40dp inset): 10:24 at 15sp medium, 26dp from the edges; signal, wifi and "
                     "battery in Color/system/ink (#1B1B1F light, white dark). On a phone the system draws its own.")

    c = cc.single("Gesture bar", "storeShots ShotHarness.kt · SystemBars", "SystemBars", w=411, h=24)
    hd = d.rect(c, name="Handle", x=151.5, y=10, w=108, h=4, fill="Color/system/gesture", radius=2)
    d.set(hd, constraints={"horizontal": "center", "vertical": "bottom"})
    cc.finish_single("Gesture bar", c, "storeShots ShotHarness.kt · SystemBars", "SystemBars",
                     "The 24dp navigation inset with the handle 108 x 4, 10dp from the bottom, in system/ink at 85 %. "
                     "Transparent: content shows through.")

    # -- Mark
    c = cc.single("Mark", "Artwork.kt · ComfortMark", "ComfortMark", w=20.97, h=30)
    mv = d.vector(c, name="Headroom", path=MARK_PATH, fill="Color/accent", w=20.97, h=30,
                  constraints={"horizontal": "scale", "vertical": "scale"})
    d.cmd("vector.fillRule", ids=[mv], rule="evenOdd")
    cc.finish_single("Mark", c, "Artwork.kt · ComfortMark", "ComfortMark",
                     "Headroom, the Attic mark (ui/expressive/Mark.kt): 30dp tall above Home's title, 24 elsewhere, in the accent "
                     "(the cover's colour inside Tinted). Width is 36/51.5 of the height.")

    # -- Hero
    sb = SetBuilder(cc, "Hero", "Bars.kt · Hero", "Hero",
                    "The expanded large title of a root screen, centred in an empty upper third: 228dp tall (Short 150) under "
                    "the status bar, 32dp sides, 40/46 bold. Mark puts the 30dp mark 14 above the title (Home only).")
    for mark, short in ((False, False), (True, False), (False, True)):
        c = sb.variant({"Mark": str(mark), "Short": str(short)}, layout="v", w=411, h=150 if short else 228, pad=[0, 32, 0, 32],
                       main="center", cross="center", sizing=("fixed", "fixed"))
        if mark:
            mf = d.frame(c, name="Mark space", layout="v", pad=[0, 0, 14, 0], cross="center", sizing=("hug", "hug"))
            d.instance("Mark", mf, name="Mark")
        t = k.text(c, "Home" if mark else "Library", "Scale/Hero", "Color/on", "Title", align="center")
        sf = d.frame(c, name="Subtitle", layout="v", pad=[6, 0, 0, 0], cross="center", sizing=("hug", "hug"))
        st = k.text(sf, "1,310 songs", "Component/Hero subtitle", "Color/on-2", "Subtitle text", align="center")
        d.set(sf, visible=False)
        sb.text("Title", t, "Library")
        sb.text("Subtitle", st, "1,310 songs")
        sb.bool("Show subtitle", sf, False)
    sb.finish()

    # -- Top bar
    sb = SetBuilder(cc, "Top bar", "Bars.kt · Bar", "Bar",
                    "56dp under the status bar, 8dp sides: back (arrow, or expand_more on Queue and Lyrics), the title 20/26 "
                    "bold (4dp after back, else 16dp in), then actions in on. Lift: content has scrolled under, so it takes the "
                    "ground and a hairline. Count is `.bar.count` (\"2 selected\") in accent-ink.")
    combos = [(b, l, False) for b in ("None", "Arrow", "Close") for l in (False, True)] + [("Arrow", False, True)]
    for back, lift, count in combos:
        c = sb.variant({"Back": back, "Lift": str(lift), "Count": str(count)}, layout="h", w=411, h=56, pad=[0, 8, 0, 8],
                       cross="center", fill="Color/ground" if lift else None, sizing=("fixed", "fixed"))
        if lift:
            d.set(c, stroke=V("Color/line"), strokeWeight=1, strokeAlign="inside", strokeSides=[False, False, True, False])
        if back != "None":
            b = d.instance("Icon button", c, name="Back")
            _swap_icon(d, b, "Icon/arrow_back" if back == "Arrow" else "Icon/expand_more")
        tf = d.frame(c, name="Title", layout="h", pad=[0, 0, 0, 4 if back != "None" else 16], sizing=("fill", "hug"))
        t = k.text(tf, "2 selected" if count else "Queue", "Scale/Bar title", "Color/accent-ink" if count else "Color/on",
                   "Title text", fill=True, lines=1)
        sb.text("Title", t, "Queue")
        for i, lig in ((1, "search"), (2, "more_vert")):
            a = d.instance("Icon button", c, name=f"Action {i}")
            _swap_icon(d, a, f"Icon/{lig}")
            d.set(a, visible=False)
            sb.bool(f"Show action {i}", a, False)
    sb.finish([{"Back": "Close", "Lift": "False", "Count": "False"}, {"Back": "Arrow", "Lift": "True", "Count": "False"}])

    # -- Sub-tabs, Tools
    sb = SetBuilder(cc, "Sub-tab row", "Bars.kt · SubTabs", "SubTabs",
                    "The whole SubTabs row: four Sub-tab instances (Components / Actions), exactly one option selected, 2dp apart. Lift is `.subtabs.lift`, pinned under a collapsed bar: full width, "
                    "the ground, a hairline under it, 14dp sides.")
    for lift in (False, True):
        c = sb.variant({"Lift": str(lift)}, layout="h", gap=2, pad=[0, 14, 0, 14] if lift else None, fill="Color/ground" if lift else None,
                       w=411 if lift else None, sizing=("fixed" if lift else "hug", "hug"))
        if lift:
            d.set(c, stroke=V("Color/line"), strokeWeight=1, strokeAlign="inside", strokeSides=[False, False, True, False])
        for i, lab in enumerate(("All", "Albums", "Artists", "Folders")):
            t = d.instance("Sub-tab", c, name=f"Sub-tab: {lab}")
            d.cmd("instance.setProperties", ids=[t], properties={"Selected": str(i == 0)})
            _set_text_prop(d, t, "Label", lab)
    sb.finish()

    sb = SetBuilder(cc, "Tools", "Bars.kt · Tools", "Tools",
                    "The 50dp row that closes the viewing area: 24dp start (the sub-tabs pulled 10 left), 8 end, 2 below; "
                    "icon buttons at the right, settings last. Home: sub-tabs and settings. Library: search, add, settings.")
    for lead in ("Sub-tabs", "None"):
        c = sb.variant({"Leading": lead}, layout="h", w=411, h=50, pad=[0, 8, 2, 14 if lead == "Sub-tabs" else 24], cross="center",
                       sizing=("fixed", "fixed"))
        lf = d.frame(c, name="Leading", layout="h", sizing=("fill", "hug"))
        if lead == "Sub-tabs":
            d.instance("Sub-tab row", lf, name="Sub-tabs")
        for nm, lig, show in (("Search", "search", lead == "None"), ("Add", "add", lead == "None"), ("Settings", "settings", True)):
            a = d.instance("Icon button", c, name=nm)
            _swap_icon(d, a, f"Icon/{lig}")
            if nm != "Settings":
                d.set(a, visible=show)
    sb.finish()

    # -- Tab, Tab bar
    sb = SetBuilder(cc, "Tab", "Dock.kt · TabBar", "TabBar",
                    "One tab of the dock: a 44dp pill, icon then label 7dp apart, centred. Selected: accent-soft with a filled "
                    "icon and bold label in on-accent-soft; else on-2.")
    for sel in (False, True):
        c = sb.variant({"Selected": str(sel)}, layout="h", w=121, h=44, gap=7, main="center", cross="center",
                       fill="Color/accent-soft" if sel else None, sizing=("fixed", "fixed"))
        k.bind_radius(c, "pill")
        ic = k.icon(c, "home", filled=sel, color="Color/on-accent-soft" if sel else "Color/on-2", name="Icon")
        t = k.text(c, "Home", "Component/Tab selected" if sel else "Component/Tab", "Color/on-accent-soft" if sel else "Color/on-2", "Label", lines=1)
        sb.text("Label", t, "Home")
        sb.swap("Icon", ic, "Icon/home (filled)" if sel else "Icon/home")
    sb.finish()

    tabs = (("Home", "home"), ("Search", "search"), ("Library", "library_music"))
    sb = SetBuilder(cc, "Tab bar", "Dock.kt · TabBar", "TabBar", "60dp, 8dp padding, three tabs sharing the width 4dp apart.")
    for tab, _ in tabs:
        c = sb.variant({"Tab": tab}, layout="h", w=387, h=60, pad=8, gap=4, sizing=("fixed", "fixed"))
        for lab, lig in tabs:
            t = d.instance("Tab", c, name=f"Tab: {lab}")
            sel = lab == tab
            d.cmd("instance.setProperties", ids=[t], properties={"Selected": str(sel)})
            _set_text_prop(d, t, "Label", lab)
            _swap_icon(d, t, f"Icon/{lig}" + (" (filled)" if sel else ""))
            d.set(t, sizingH="fill")
    sb.finish()

    # -- Mini player
    sb = SetBuilder(cc, "Mini player", "Dock.kt · MiniPlayer", "MiniPlayer",
                    "The current song as a 64dp row whose played part is filled (mini-fill over mini-base, the fill's right "
                    "corners 22): cover 44, title 15/20 semibold, artist 13/17 in mini-ink-2, the heart, then play or pause "
                    "(30dp, filled). 10dp start, 4 end, 12 between. Dock: the dock's first row (top corners 30), and it tints "
                    "itself: the variant sets Color mode Tinted. Alone (full-screen lyrics): all corners 30, a small play "
                    "button; Top (Now Playing's compact header): 60dp, square, no cover, 20dp start. Those two inherit the "
                    "washed palette of the screen around them. Played part shown at 1:18 of 3:52. Art=None is a file opened "
                    "from another app (playback/OpenWith.kt): no seed, so no tint (Color mode Base), the neutral note box for a "
                    "cover, and an empty artist line that keeps its height, so the title sits high (RowText draws the empty "
                    "subtitle). Set Title, set Artist to nothing, and set the Played width to the file's position.")
    combos = [("Dock", p, f, "Image") for p in (True, False) for f in (True, False)] \
        + [("Alone", p, False, "Image") for p in (True, False)] + [("Top", p, True, "Image") for p in (True, False)] \
        + [("Dock", p, False, "None") for p in (True, False)]
    for place, playing, fav, art in combos:
        w = 387 if place != "Top" else 411
        h = 60 if place == "Top" else 64
        c = sb.variant({"Placement": place, "Playing": str(playing), "Favourite": str(fav), "Art": art}, layout="h", w=w, h=h,
                       pad=[0, 4, 0, 20 if place == "Top" else 10], gap=12, cross="center", fill="Color/mini-base", clip=True,
                       sizing=("fixed", "fixed"))
        d.set(c, cornerRadius={"Dock": [30, 30, 0, 0], "Alone": 30, "Top": 0}[place])
        if place == "Dock":
            d.set_mode(c, "Color", "Tinted" if art == "Image" else "Base")
        pw = round(w * NOW_FRACTION, 1)
        pr = d.rect(c, name="Played", w=pw, h=h, fill="Color/mini-fill", cornerRadius=[0, 22, 22, 0])
        k.absolute(pr, 0, 0, pw, h, {"horizontal": "left", "vertical": "topBottom"})
        if place != "Top":
            _cover(d, k, c, 44, name="Cover", art=art)
        txt = d.frame(c, name="Labels", layout="v", sizing=("fill", "hug"))
        title = "Lights Over the Water" if art == "Image" else "band-practice"
        artist = "Mira Okafor" if art == "Image" else ""
        t1 = k.text(txt, title, "Component/Mini title", "Color/on", "Title", fill=True, lines=1)
        t2 = k.text(txt, artist, "Component/Mini artist", "Color/mini-ink-2", "Artist", fill=True, lines=1)
        sb.text("Title", t1, "Lights Over the Water")
        sb.text("Artist", t2, "Mira Okafor")
        if place == "Alone":
            pf = d.frame(c, name="Play", layout="h", pad=[0, 6, 0, 0], sizing=("hug", "hug"))
            p = d.instance("Play button", pf, name="Play button")
            d.cmd("instance.setProperties", ids=[p], properties={"Size": "Small", "Playing": str(playing)})
        else:
            hb = d.instance("Heart button", c, name="Heart")
            d.cmd("instance.setProperties", ids=[hb], properties={"Tonal": "False", "Favourite": str(fav)})
            pb = d.frame(c, name="Play or pause", w=48, h=48)
            ic = k.icon(pb, "pause" if playing else "play_arrow", size=30, filled=True, color="Color/on", name="Icon")
            d.set(ic, x=3, y=9 + glyph_drop(30))
    sb.finish([{"Placement": "Dock", "Playing": "True", "Favourite": "True", "Art": "Image"},
               {"Placement": "Dock", "Playing": "False", "Favourite": "False", "Art": "Image"},
               {"Placement": "Alone", "Playing": "True", "Favourite": "False", "Art": "Image"},
               {"Placement": "Dock", "Playing": "True", "Favourite": "False", "Art": "None"}])
    cc.preview.append(("Mini player", [{"Placement": "Dock", "Playing": "True", "Favourite": "True", "Art": "Image",
                                        "Title": "Calle de los Naranjos", "Artist": "Paloma Ferrer"}]))

    # -- Dock
    sb = SetBuilder(cc, "Dock", "Dock.kt · Dock", "Dock",
                    "One floating card (raised, radius 30, Shadow/Floating and the edge ring) 12dp in from the edges, the mini "
                    "player as its first row and the tabs as its second; with nothing queued it is the tab bar alone. Content "
                    "fades to the ground over the 36dp above the card. Bottom gap: max(22, navigation inset + 6) = 30 over the "
                    "renders' 24dp gesture inset. Tab=None is full-screen lyrics' `.dock.solo`.")
    combos = [(t, m) for t in ("Home", "Search", "Library") for m in (True, False)] + [("None", True)]
    for tab, mini in combos:
        hh = 28 + (64 if mini else 0) + (60 if tab != "None" else 0) + 30
        c = sb.variant({"Tab": tab, "Mini player": str(mini)}, layout="v", w=411, h=hh, pad=[28, 12, 30, 12], sizing=("fixed", "hug"))
        d.set(c, fills=[{"type": "linearGradient", "stops": [{"color": "#00000000", "position": 0}, {"color": "#000000", "position": 36 / hh}],
                         "from": [0.5, 0], "to": [0.5, 1]}])
        d.bind(c, "fills/0/stops/0/color", k.ids["Color/derived/ground-clear"])
        d.bind(c, "fills/0/stops/1/color", k.ids["Color/ground"])
        card = d.frame(c, name="Card", layout="v", fill="Color/raised", clip=True, stroke="Color/edge", stroke_w=1,
                       strokeAlign="inside", sizing=("fill", "hug"))
        k.bind_radius(card, "float")
        d.cmd("style.apply", ids=[card], style=d.effect_styles["Shadow/Floating"])
        if mini:
            m = d.instance("Mini player", card, name="Mini player")
            d.cmd("instance.setProperties", ids=[m], properties={"Placement": "Dock" if tab != "None" else "Alone", "Playing": "True",
                                                                  "Favourite": "True" if tab != "None" else "False"})
            d.set(m, sizingH="fill")
        if tab != "None":
            tb = d.instance("Tab bar", card, name="Tab bar")
            d.cmd("instance.setProperties", ids=[tb], properties={"Tab": tab})
            d.set(tb, sizingH="fill")
    sb.finish([{"Tab": "Home", "Mini player": "True"}, {"Tab": "Library", "Mini player": "False"}])

    # -- Action bar
    sb = SetBuilder(cc, "Action bar item", "Controls.kt · ActionBarItem", "ActionBarItem",
                    "An icon over a 12/15 label, 56dp tall, sharing the bar's width. Go is the accent action (accent-ink, bold "
                    "label); Danger in danger; Disabled 38 %.")
    for tone, state in (("Default", "Default"), ("Default", "Disabled"), ("Go", "Default"), ("Danger", "Default")):
        ink = {"Default": "Color/on", "Go": "Color/accent-ink", "Danger": "Color/danger"}[tone]
        c = sb.variant({"Tone": tone, "State": state}, layout="v", w=90, h=56, gap=3, main="center", cross="center", sizing=("fixed", "fixed"))
        if state == "Disabled":
            d.set(c, opacity=0.38)
        lig, lab = {"Default": ("add", "Add songs"), "Go": ("check", "Done"), "Danger": ("delete", "Delete")}[tone]
        ic = k.icon(c, lig, color=ink, name="Icon")
        t = k.text(c, lab, "Component/Action go" if tone == "Go" else "Component/Action", ink, "Label", lines=1)
        sb.text("Label", t, lab)
        sb.swap("Icon", ic, f"Icon/{lig}")
    sb.finish()

    c = cc.single("Action bar", "Controls.kt · ActionBar", "ActionBar", layout="h", w=387, h=68, pad=[0, 8, 0, 8], cross="center",
                  fill="Color/raised", stroke="Color/edge", stroke_w=1, strokeAlign="inside", sizing=("fixed", "fixed"))
    k.bind_radius(c, "float")
    d.cmd("style.apply", ids=[c], style=d.effect_styles["Shadow/Floating"])
    for tone, lab, lig in (("Default", "Add songs", "add"), ("Go", "Done", "check")):
        it = d.instance("Action bar item", c, name=f"Item: {lab}")
        d.cmd("instance.setProperties", ids=[it], properties={"Tone": tone, "State": "Default"})
        # A variant switch keeps the text and swap values, so set both (ComfortCollections.kt: Symbol.Add, Symbol.Check).
        _set_text_prop(d, it, "Label", lab)
        _swap_icon(d, it, f"Icon/{lig}")
        d.set(it, sizingH="fill")
    cc.finish_single("Action bar", c, "Controls.kt · ActionBar", "ActionBar",
                     "Replaces the dock in a mode (a playlist's reorder mode: Add songs, Done): a floating card 68dp tall, 12dp "
                     "in from the edges and 22 up, 8dp inside.")

    # -- Step pips
    sb = SetBuilder(cc, "Step pips", "ComfortOnboarding.kt · StepPips", "StepPips",
                    "Where the first run is: one 8dp pip per step, 6 apart; the current one a 22dp capsule. Steps up to the "
                    "current are accent, the rest fill-2. Width and colour tween over 240 ms.")
    for step in (1, 2, 3, 4):
        c = sb.variant({"Step": str(step)}, layout="h", gap=6, cross="center", sizing=("hug", "hug"))
        for i in range(1, 5):
            p = d.rect(c, name=f"Pip {i}", w=22 if i == step else 8, h=8, fill="Color/accent" if i <= step else "Color/fill-2")
            k.bind_radius(p, "pill")
    sb.finish([{"Step": "2"}])

    cc.dark_preview()


# ------------------------------------------------------------------------ Playback
def _playback(d: Doc, k: K):
    cc = Components(d, "Components / Playback",
                    "Everything you touch on Now Playing lives in one card, the deck. Now Playing, the queue and full-screen "
                    "lyrics are Tinted(wash = true): set Color mode Tinted wash on their screens. Shown here in Base. Source: "
                    "ui/comfort/NowPlayingParts.kt, ComfortLyrics.kt, ComfortQueue.kt.")

    # -- Seek bar
    sb = SetBuilder(cc, "Seek bar", "NowPlayingParts.kt · SeekBar", "SeekBar",
                    "A 48dp track (fill, radius 18) whose played part is the accent, never narrower than 64dp so at 0:00 it is "
                    "a thumb; a 3 x 16 tick in on-accent at 40 % sits 12dp inside the fill's end. Elapsed in on-accent inside "
                    "the fill, total in on-2 at the far end, 14/18 semibold tabular, 16dp in. For another position set the "
                    "width of the \"Played\" layer to 64 + (351 - 64) x fraction; the tick rides on its right edge.")
    for prog, frac, el in (("Start", 0, "0:00"), ("Playing", NOW_FRACTION, "1:18"), ("End", 0.95, "3:40")):
        c = sb.variant({"Progress": prog}, layout="h", w=351, h=48, pad=[0, 16, 0, 16], main="spaceBetween", cross="center",
                       fill="Color/fill", clip=True, sizing=("fixed", "fixed"))
        k.bind_radius(c, "control")
        pw = round(64 + (351 - 64) * frac, 1)
        # The tick is the played part's child, pinned to its right edge, so resizing "Played" carries the tick along.
        f = d.frame(c, name="Played", w=pw, h=48, fill="Color/accent")
        k.bind_radius(f, "control")
        k.absolute(f, 0, 0, pw, 48, {"horizontal": "left", "vertical": "topBottom"})
        tk = d.rect(f, name="Tick", w=3, h=16, x=pw - 12, y=16, fill="Color/derived/seek-tick", radius=2)
        d.set(tk, constraints={"horizontal": "right", "vertical": "center"})
        t1 = k.text(c, el, "Component/Seek time", "Color/on-accent", "Elapsed")
        t2 = k.text(c, "3:52", "Component/Seek time", "Color/on-2", "Total")
        sb.text("Elapsed", t1, el)
        sb.text("Total", t2, "3:52")
    sb.finish()

    sb = SetBuilder(cc, "Skip button", "NowPlayingParts.kt · SkipButton", "SkipButton",
                    "A 64dp target (superellipse ripple) with a 42dp filled glyph at weight 400 in on.")
    for dirn, lig in (("Previous", "skip_previous"), ("Next", "skip_next")):
        c = sb.variant({"Direction": dirn}, w=64, h=64)
        ic = k.icon(c, lig, size=42, w400=True, color="Color/on", name="Icon")
        d.set(ic, x=11, y=11 + glyph_drop(42))
    sb.finish()

    sb = SetBuilder(cc, "Transport", "NowPlayingParts.kt · Transport", "Transport",
                    "Previous, the 76dp play button, next, 26dp apart and centred, 14dp below the seek bar.")
    for playing in (True, False):
        c = sb.variant({"Playing": str(playing)}, layout="h", w=351, pad=[14, 0, 0, 0], gap=26, main="center", cross="center",
                       sizing=("fixed", "hug"))
        p = d.instance("Skip button", c, name="Previous")
        pl = d.instance("Play button", c, name="Play button")
        d.cmd("instance.setProperties", ids=[pl], properties={"Size": "Large", "Playing": str(playing)})
        n = d.instance("Skip button", c, name="Next")
        d.cmd("instance.setProperties", ids=[n], properties={"Direction": "Next"})
    sb.finish()

    c = cc.single("Toggles", "NowPlayingParts.kt · Toggles", "Toggles", layout="h", w=351, pad=[12, 10, 0, 10], main="spaceBetween",
                  cross="center", sizing=("fixed", "hug"))
    for nm, comp, lig, props in (("Shuffle", "Tonal icon button", "shuffle", {"Size": "Large", "Checked": "False", "State": "Default", "Container": "Fill"}),
                                 ("Repeat", "Tonal icon button", "repeat", {"Size": "Large", "Checked": "False", "State": "Default", "Container": "Fill"}),
                                 ("Favourite", "Heart button", None, {"Tonal": "True", "Favourite": "True"}),
                                 ("Queue", "Tonal icon button", "queue_music", {"Size": "Large", "Checked": "False", "State": "Default", "Container": "Fill"})):
        i = d.instance(comp, c, name=nm)
        d.cmd("instance.setProperties", ids=[i], properties=props)
        if lig:
            _swap_icon(d, i, f"Icon/{lig}")
        d.cmd("component.exposeInstance", id=i)
    cc.finish_single("Toggles", c, "NowPlayingParts.kt · Toggles", "Toggles",
                     "Shuffle, repeat (repeat_one when repeating one), favourite and queue as 54dp tonal toggles spread across "
                     "the deck, 12dp below the transport, 10dp in. The four nested instances are exposed: set Checked on them.")

    sb = SetBuilder(cc, "Deck", "NowPlayingParts.kt · Deck", "Deck",
                    "One card (radius 30, card colour, 14dp inside, 16dp in from the screen edges) holding the seek bar, the "
                    "transport and the toggles.")
    for playing in (True, False):
        c = sb.variant({"Playing": str(playing)}, layout="v", w=379, pad=14, fill="Color/card", sizing=("fixed", "hug"))
        k.bind_radius(c, "float")
        s = d.instance("Seek bar", c, name="Seek bar")
        d.cmd("instance.setProperties", ids=[s], properties={"Progress": "Playing", "Elapsed": "1:18"})
        t = d.instance("Transport", c, name="Transport")
        d.cmd("instance.setProperties", ids=[t], properties={"Playing": str(playing)})
        d.instance("Toggles", c, name="Toggles")
    sb.finish([{"Playing": "True"}])

    # -- Now Playing parts
    c = cc.single("Now playing header", "NowPlayingParts.kt · NowPlayingHeader", "NowPlayingHeader", layout="h", w=411, h=60,
                  pad=[0, 8, 0, 8], cross="center", sizing=("fixed", "fixed"))
    b = d.instance("Icon button", c, name="Close")
    _swap_icon(d, b, "Icon/expand_more")
    txt = d.frame(c, name="Labels", layout="v", pad=[0, 0, 0, 4], sizing=("fill", "hug"))
    k.text(txt, "Playing from", "Scale/Caption", "Color/on-2", "Overline", fill=True, lines=1)
    ctx_t = k.text(txt, "Paper Lanterns", "Component/Player context", "Color/on", "Context", fill=True, lines=1)
    mo = d.instance("Icon button", c, name="More")
    cc.finish_single("Now playing header", c, "NowPlayingParts.kt · NowPlayingHeader", "NowPlayingHeader",
                     "60dp under the status bar: close (expand_more), \"Playing from\" over the context, more options.",
                     binds=[("Context", "text", ctx_t, "Paper Lanterns")])

    sb = SetBuilder(cc, "Now playing cover", "NowPlayingParts.kt · NowPlayingCover", "NowPlayingCover",
                    "The cover as a large superellipse (n = 5), 30dp in from the edges, 4 below the header. Paused draws it at "
                    "86 %, centred, over 260 ms.")
    for paused in (False, True):
        c = sb.variant({"Paused": str(paused)}, w=411, h=355)
        side = 351 * (0.86 if paused else 1)
        cv = _cover(d, k, c, round(side, 2), name="Cover")
        d.set(cv, x=(411 - side) / 2, y=4 + (351 - side) / 2)
    sb.finish()

    c = cc.single("Now playing title", "NowPlayingParts.kt · NowPlayingTitle", "NowPlayingTitle", layout="v", w=411, pad=[18, 30, 0, 30],
                  gap=2, sizing=("fixed", "hug"))
    t1 = k.text(c, "Lights Over the Water", "Scale/Title", "Color/on", "Title", fill=True, lines=1)
    t2 = k.text(c, "Mira Okafor", "Component/Player artist", "Color/on-2", "Artist", fill=True, lines=1)
    cc.finish_single("Now playing title", c, "NowPlayingParts.kt · NowPlayingTitle", "NowPlayingTitle",
                     "The song (26/32 bold, one line) and the artist (16/22 on-2), which opens the artist. 30dp sides, 18 above.",
                     binds=[("Title", "text", t1, "Lights Over the Water"), ("Artist", "text", t2, "Mira Okafor")])

    c = cc.single("Now playing lyric", "NowPlayingParts.kt · NowPlayingLyric", "NowPlayingLyric", layout="h", w=411, pad=[10, 30, 0, 30],
                  gap=8, cross="center", sizing=("fixed", "hug"))
    k.icon(c, "lyrics", size=18, color="Color/accent-ink", name="Icon")
    ln = k.text(c, "carry what we couldn't say", "Component/Player lyric", "Color/accent-ink", "Lyric", fill=True, lines=1)
    cc.finish_single("Now playing lyric", c, "NowPlayingParts.kt · NowPlayingLyric", "NowPlayingLyric",
                     "The line being sung, in accent-ink after the 18dp lyrics glyph. Tap opens lyrics.",
                     binds=[("Line", "text", ln, "carry what we couldn't say")])

    # -- Lyrics line
    sb = SetBuilder(cc, "Lyrics line", "ComfortLyrics.kt · SyncedLine", "SyncedLine",
                    "Full-screen lyrics: 22/30 semibold to come (on-2) and sung (derived/lyric-sung, on-2 72 % of the way to "
                    "on-3), 14dp sides and 9 above and below (8 in this drawing: Compose trims the "
                    "text's 30dp line to the font's own 28, so a line is 46 tall); the line being sung grows to 27/34 bold in on, on an accent-soft "
                    "bed (radius 18, 18 above, 19 below) with 10dp of room around it. A blank line is the note glyph. 16dp in "
                    "from the screen edges.")
    for state in ("Sung", "Current", "Upcoming", "Blank"):
        cur = state == "Current"
        c = sb.variant({"State": state}, layout="v", w=379, pad=[10, 0, 10, 0] if cur else None, sizing=("fixed", "hug"))
        # SyncedLine's TextStyle has no lineHeightStyle, so Compose's default trims the first line's top and the last
        # line's bottom to the font's own height (Onest 22: 28, not 30). The 2dp it trims come off the 9dp pads here,
        # so a line is 46 tall and a wrapped one 76, as on the phone. The current line (27/34) has nothing to trim.
        inner = d.frame(c, name="Lyric bed", layout="v", pad=[18, 14, 19, 14] if cur else ([9, 14, 9, 14] if state == "Blank" else [8, 14, 8, 14]),
                        fill="Color/accent-soft" if cur else None, sizing=("fill", "hug"))
        if cur:
            k.bind_radius(inner, "control")
        if state == "Blank":
            bx = d.frame(inner, name="Note", layout="h", h=30, cross="center", sizing=("hug", "fixed"))
            k.icon(bx, "music_note", color="Color/on-2", name="Icon")
        else:
            txt = {"Sung": "paper lanterns over the water", "Current": "carry what we couldn't say",
                   "Upcoming": "let them drift until the morning"}[state]
            t = k.text(inner, txt, "Component/Lyric line current" if cur else "Component/Lyric line",
                       {"Sung": "Color/derived/lyric-sung", "Current": "Color/on", "Upcoming": "Color/on-2"}[state], "Line text", fill=True)
            sb.text("Line", t, txt)
    sb.finish()

    # -- Lyrics card
    sb = SetBuilder(cc, "Lyrics card", "NowPlayingParts.kt · LyricsCard", "LyricsCard",
                    "Under the deck on Now Playing: \"Lyrics\" (14/18 semibold on-2, 12 below) over four lines from the one "
                    "being sung (22/28 bold; current in on, the rest on-3, 8 below each) and a small tonal \"Open lyrics\"; "
                    "plain text clamped to four lines; or one sentence and at most one action (Download lyrics, 14 above), the "
                    "small spinner in its place while it runs. Radius 26, 20dp sides, 18 above, 20 below, 16 in from the edges.")
    lines = ["carry what we couldn't say", "let them drift until the morning", "let them burn the long way", "♪"]
    for state in ("Synced", "Plain", "Instrumental", "Not downloaded", "Not found", "Downloading"):
        c = sb.variant({"State": state}, layout="v", w=379, pad=[18, 20, 20, 20], fill="Color/card", sizing=("fixed", "hug"))
        k.bind_radius(c, "group")
        hd = d.frame(c, name="Heading", layout="v", pad=[0, 0, 12, 0], sizing=("fill", "hug"))
        k.text(hd, "Lyrics", "Scale/Label", "Color/on-2", "Title")
        if state == "Synced":
            for i, ln in enumerate(lines):
                lf = d.frame(c, name=f"Lyric {i + 1}", layout="v", pad=[0, 0, 8, 0], sizing=("fill", "hug"))
                k.text(lf, ln, "Scale/Lyric", "Color/on" if i == 0 else "Color/on-3", "Line text", fill=True)
        elif state == "Plain":
            lf = d.frame(c, name="Text block", layout="v", pad=[0, 0, 8, 0], sizing=("fill", "hug"))
            k.text(lf, "I left the porch light on for you\nthe moths are writing letters on the glass\nevery bus that turns the corner\n"
                       "sounds a little like you coming back", "Scale/Lyric", "Color/on", "Lyrics text", fill=True, lines=4)
        elif state == "Instrumental":
            r = d.frame(c, name="Message", layout="h", gap=14, cross="center", sizing=("fill", "hug"))
            ib = d.instance("Icon box", r, name="Icon box")
            d.cmd("instance.setProperties", ids=[ib], properties={"Style": "Neutral"})
            _swap_icon(d, ib, "Icon/music_note")
            d.set(ib, width=44, height=44)
            k.text(r, "This song is instrumental.", "Scale/Body", "Color/on-2", "Message", fill=True)
        else:
            msg = "No lyrics were found for this song." if state == "Not found" else "Download once and they stay on this phone."
            k.text(c, msg, "Scale/Body", "Color/on-2", "Message", fill=True)
            af = d.frame(c, name="Action", layout="h", pad=[14, 0, 0, 0], h=None, cross="center", sizing=("hug", "hug"))
            if state == "Downloading":
                sp = d.frame(af, name="Spinner box", layout="h", h=36, cross="center", sizing=("hug", "fixed"))
                s = d.instance("Spinner", sp, name="Spinner")
                d.cmd("instance.setProperties", ids=[s], properties={"Size": "Small"})
            else:
                b = d.instance("Button", af, name="Download lyrics")
                d.cmd("instance.setProperties", ids=[b], properties={"Style": "Filled", "Size": "Small", "State": "Default", "Tone": "Default"})
                _set_text_prop(d, b, "Label", "Download lyrics")
        if state in ("Synced", "Plain"):
            of = d.frame(c, name="Open", layout="h", pad=[6, 0, 0, 0], sizing=("hug", "hug"))
            b = d.instance("Button", of, name="Open lyrics")
            d.cmd("instance.setProperties", ids=[b], properties={"Style": "Tonal", "Size": "Small", "State": "Default", "Tone": "Default"})
            _set_text_prop(d, b, "Label", "Open lyrics")
    sb.finish([{"State": "Synced"}, {"State": "Not downloaded"}])

    # -- Queue row
    sb = SetBuilder(cc, "Queue row", "ComfortQueue.kt · QueuePage", "SongRow",
                    "Default: a song row with the drag handle, in the up-next group. Dragging: lifted 6dp in from the sides, on "
                    "raised with radius 18 and Shadow/Small, 10dp start. Current (`.q-now`, the song playing now): the mini player's filled row as "
                    "a group (radius 26, mini-base, the played part mini-fill), the current song with its bars, the second line "
                    "and the more button in mini-ink-2, 72dp. Swiping: the row moved aside over fill, with Remove in danger on "
                    "the side it uncovers.")
    for state in ("Default", "Dragging", "Current", "Swiping"):
        if state == "Default":
            c = sb.variant({"State": state}, layout="v", w=379, sizing=("fixed", "hug"))
            r = d.instance("Song row", c, name="Song row")
            _queue_row(d, r)
        elif state == "Dragging":
            c = sb.variant({"State": state}, layout="v", w=379, pad=[0, 6, 0, 6], sizing=("fixed", "hug"))
            r = d.instance("Song row", c, name="Song row")
            _queue_row(d, r)
            d.set(r, sizingH="fill", fill=V("Color/raised"), paddingLeft=10)
            k.bind_radius(r, "control")
            d.cmd("style.apply", ids=[r], style=d.effect_styles["Shadow/Small"])
        elif state == "Current":
            c = sb.variant({"State": state}, layout="v", w=379, fill="Color/mini-base", clip=True, sizing=("fixed", "hug"))
            k.bind_radius(c, "group")
            pw = round(379 * NOW_FRACTION, 1)
            pr = d.rect(c, name="Played", w=pw, h=72, fill="Color/mini-fill", cornerRadius=[0, 22, 22, 0])
            k.absolute(pr, 0, 0, pw, 72, {"horizontal": "left", "vertical": "topBottom"})
            r = d.instance("Song row", c, name="Song row")
            d.cmd("instance.setProperties", ids=[r], properties={"Lead": "Cover", "Current": "True"})
            d.set(r, sizingH="fill", minHeight=72)
            _set_text_prop(d, r, "Title", "Lights Over the Water")
            d.override(r, "Bed", visible=False)
            d.override(r, "Subtitle", fill="Color/mini-ink-2")
            tr = d.child(r, "Trailing")
            d.override(tr, "Glyph", fill="Color/mini-ink-2")
        else:
            c = sb.variant({"State": state}, w=379, h=64, fill="Color/fill", clip=True)
            rm = d.frame(c, name="Remove", layout="h", gap=6, pad=[0, 24, 0, 20], cross="center", sizing=("hug", "hug"))
            k.icon(rm, "do_not_disturb_on", size=20, color="Color/danger", name="Icon")
            k.text(rm, "Remove", "Component/Swipe action", "Color/danger", "Label")
            g = d.get(rm)
            d.set(rm, x=379 - g["width"], y=(64 - g["height"]) / 2, constraints={"horizontal": "right", "vertical": "center"})
            r = d.instance("Song row", c, name="Song row", x=-132, y=0)
            _queue_row(d, r, fill=False)
            d.set(r, fill=V("Color/card"), cornerRadius=[0, 18, 18, 0])
    sb.finish([{"State": "Current"}, {"State": "Dragging"}])

    c = cc.single("Queue clear", "ComfortQueue.kt · ClearFoot", "ClearFoot", layout="h", sizing=("hug", "hug"))
    b = d.instance("Button", c, name="Clear")
    d.cmd("instance.setProperties", ids=[b], properties={"Style": "Plain", "Size": "Medium", "State": "Default", "Tone": "Default"})
    _set_text_prop(d, b, "Label", "Clear")
    _set_text_prop_bool(d, b, "Show leading icon", True)
    _swap_icon(d, b, "Icon/clear_all", "Leading icon")
    d.set(b, fill=V("Color/raised"), stroke=V("Color/edge"), strokeWeight=1, strokeAlign="inside")
    d.cmd("style.apply", ids=[b], style=d.effect_styles["Shadow/Small"])
    cc.finish_single("Queue clear", c, "ComfortQueue.kt · ClearFoot", "ClearFoot",
                     "\"Clear\" at the bottom centre of the queue, 28dp over the gesture inset, over a fade to the ground: the "
                     "plain button on raised with Shadow/Small and the edge ring.")

    cc.dark_preview()


def _queue_row(d: Doc, r: int, fill: bool = True):
    d.cmd("instance.setProperties", ids=[r], properties={"Lead": "Cover", "Current": "False"})
    if fill:
        d.set(r, sizingH="fill")
    tr = d.child(r, "Trailing")
    d.cmd("instance.setProperties", ids=[tr], properties={"Kind": "Drag handle", "Tone": "Default"})
