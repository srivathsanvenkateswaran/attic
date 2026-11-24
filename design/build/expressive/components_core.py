"""Core components of Attic Expressive.

Pages: Components / Actions, Components / Lists and cards, Components / Navigation, Components / Playback.

Each set below is a main component set (or a single main component) with auto layout, named layers,
fills bound to Color/* variables, text on text styles and icons as Icon/* instances. Its description
names the Compose source; codeConnect links it to the composable in app.attic.player.ui.expressive.
Light and Dark come from variable modes. Sets marked "Cover mode" have Color mode Cover set on every
variant, so their instances take the cover scheme wherever they are placed (they are drawn inside
FromArt in code). Components with no mark follow the Color mode of the frame around them, so a
heart or play button inside a cover-mode frame takes the cover's colours, as in code.

Use `inst(d, "Button", parent, Style="Tonal", Size="Small")` to place one, `set_text(d, inst, "Label",
"Open lyrics")` for a text property and `set_cover(d, inst, 3)` to put demo cover 3 into every
"Picture" layer inside an instance (Mosaic covers take four indices). `set_progress(d, inst, fraction)` sets a
Progress/Linear, Progress/Wavy or Seek bar instance; `ring_arcs(d, play_inst, fraction)` turns a small play
control's ring.

Components / Actions
--------------------
Button                     ExpressiveButton (Buttons.kt)
  Style=Filled|Tonal|Outlined|Text|Text danger, Size=Small|Medium, Icon=None|Leading|Trailing,
  State=Enabled|Disabled (with or without a leading icon), Squared=False|True.  Props: Label (text), Leading icon,
  Trailing icon (swap)
Icon button                ExpressiveIconButton (Buttons.kt)
  Style=Standard|Filled|Tonal|Outlined, Size=Default|Medium, Width=Narrow|Default|Wide,
  State=Enabled|Disabled.  Props: Icon (swap)
Toggle button              ExpressiveToggleButton (Buttons.kt)
  Checked=False|True, Size=Default|Medium, Off style=Standard|Tonal.  Props: Icon, Icon when on (swaps; give the
  filled glyph to Icon when on)
Heart button               HeartButton (Buttons.kt)                Favourite=False|True
Play button                PlayButton (PlayControls.kt)
  Size=Small|Large|Extra large, Playing=False|True, Ring=False|True (Small), Tonal=False|True (Large)
Skip button                SkipButton (PlayControls.kt)            Direction=Previous|Next
Collection play button     CollectionPlayButton (ExpressiveCollections.kt)  Playing=False|True. Cover mode
Extended FAB               ExpressiveExtendedFab (Buttons.kt)      Expanded=True|False.  Props: Label, Icon
Split button               SplitButton (Buttons.kt), single component.  Props: Label, Leading icon
Connected button group/Segment   one segment of ConnectedButtonGroup (Navigation.kt)
  Position=First|Middle|Last, Selected=False|True, On cell=False|True.  Props: Label
Connected button group     ConnectedButtonGroup (Navigation.kt)
  Options=4|3|2, Selected=1..Options, On cell=False|True; nested segments are exposed (set Selected and On cell on
  the group: a segment switched on its own loses its width)
Filter chip                ExpressiveFilterChip (Navigation.kt)    Selected=False|True.  Props: Label
Switch                     ExpressiveSwitch (Surfaces.kt)          Checked=False|True
Radio                      ExpressiveRadio (Surfaces.kt)           Selected=False|True
Progress/Linear            LinearProgress (PlayControls.kt), single component. The Active layer's width is the fraction
Progress/Wavy              WavyLinearProgress (PlayControls.kt)    Running=True|False. The Active frame crops the wave

Components / Lists and cards
----------------------------
Cover                      Cover / ArtistCover / Mosaic / FavouritesArt (Cover.kt)
  Kind=Album (Size 48|56|64|72|112|120|148|208), Artist (28|48|56|88|112), Mosaic (56|64|72|148),
  Favourites (56|148).  Picture layers: "Picture" (Album, Artist), "Picture 1".."Picture 4" (Mosaic)
List row/Song              SongRow (Lists.kt)
  Lead=Cover|Number|Ranked cover|Ranked artist|None,
  State=Default|Current playing|Current paused|Lifted (Lifted on Cover only).
  Props: Title, Subtitle (text), Show subtitle (boolean), Trailing (swap)
List row/Drag handle       RowDragHandle (Lists.kt)
List row/Count             RowCount (Lists.kt).  Props: Count
List row/Collection        CollectionRow (Lists.kt)   Kind=Album|Artist|Playlist|Folder|Favourites, Mosaic=True|False
  (Playlist, Folder: False draws one cover, for fewer than four albums).  Props: Title, Subtitle; Lead exposed
List row/Setting           SetIconRow (ExpressiveSettings.kt) and SetRow (Surfaces.kt)
  Leading=Icon|None, Lines=One|Two, Trailing=None|Switch off|Switch on, Status line=False|True (Backup's line).
  Props: Title, Summary, Icon (swap), Status text
List row/Setting note      SetNote (Surfaces.kt), the Backup section's last cell.  Props: Text
List group/Cell            groupCell / ListGroup (Lists.kt)
  Position=Single|Top|Middle|Bottom, Surface=Default|Sheet.  Props: Row (swap)
Section header             SectionHeader (Navigation.kt)  Overline=False|True, Trailing=False|True (Recap's play
  control, 48 x 36 in the layout and drawn 10dp past it).
  Props: Title, Overline text
List label                 ListLabel (Navigation.kt).  Props: Label
Settings section           SettingsSection (Surfaces.kt).  Props: Label
Tile                       Tile (Tiles.kt)   Kind=Album|Folder|Artist|Recent|Spotlight, Now playing=False|True (Recent)
  Props: Title, Subtitle
Favourites pill            FavouritesPill (Tiles.kt)      Height=248|200.  Props: Count, Unit
Hero caption               HeroCaption (Tiles.kt).  Props: Title, Subtitle
Instant mix pill           InstantMixPill (Tiles.kt)      Now=False|True.  Props: Title
Recap entry                RecapCard (Tiles.kt), the Recap card on Home (named apart from Recap/Card).  Props: Title, Body
Spotlight                  Spotlight (Tiles.kt), single component
Browse tile                BrowseTile (Tiles.kt)          Size=Default|Folder.  Cover mode.  Props: Title, Subtitle

Components / Navigation
-----------------------
Navigation item            ShortNavigationBarItem in ExpressiveNavigationBar  Destination=Home|Search|Library,
  Selected=True|False (labels follow Destination)
Navigation bar             ExpressiveNavigationBar (Navigation.kt)  Selected=Home|Search|Library (88 tall: 64 + gesture area)
Top app bar/Root           RootAppBar (Navigation.kt)     Kind=Home|Plain, Scrolled=False|True (status bar inset included)
  Props: Title, Show search, Show grid toggle
Top app bar/Detail         DetailAppBar (Navigation.kt)   Surface=Plain|Scrolled|Over art.  Props: Title, Show action
  (hides the action alone), Action
Top app bar/Collection     CollectionBar (ExpressiveCollections.kt)  Collapsed=False|True.  Cover mode.  Props: Title
Search field               SearchField (Navigation.kt)    State=Empty|Query|Docked.  Props: Text
Step pips                  StepPips (ExpressiveOnboarding.kt)  Step=1|2|3|4
Top app bar/First run      the first run's bar (ExpressiveOnboarding.kt)  Step=1|2|3|4 (no back on step 1)
Chip row                   ChipRow of the Library filters (ExpressiveLibrary.kt)  Selected=None|Playlists|Albums|Artists|Songs
Mini player                ExpressiveMiniPlayer (ExpressiveShell.kt)  State=Playing|Paused, Title length=Default|Long. Cover mode.
  Props: Title, Artist
Status bar                 the render harness's status bar (storeShots ShotHarness.kt SystemBars), 411 x 40
Gesture bar                the render harness's gesture handle, 411 x 24

Components / Playback
---------------------
Wave                       NowPlayingWave (PlayControls.kt)  Playing=True|False
Seek bar                   SeekBar (PlayControls.kt)        Playing=True|False.  Cover mode. Active track width sets it
Seek times                 SeekTimes (PlayControls.kt).  Props: Elapsed, Total.  Cover mode
Transport                  Transport (PlayControls.kt)      Playing=True|False.  Cover mode
Now playing tools          Tools (ExpressiveNowPlaying.kt)  Shuffle=Off|On, Repeat=Off|All|One.  Cover mode
Now playing header         Header (ExpressiveNowPlaying.kt).  Props: Context, Context icon, Show context icon. Cover mode
Compact header             CompactHeader (ExpressiveNowPlaying.kt)  Playing=True|False.  Cover mode
Lyrics/Line                LyricLine (Surfaces.kt)          State=Sung|Current|Upcoming|Plain, Size=Card|Large.  Props: Text
Lyrics/Screen line         SyncedLine (ExpressiveLyrics.kt) State=Sung|Current|Upcoming.  Props: Text
Lyrics/Card                LyricsCard (ExpressiveNowPlaying.kt)
  State=Synced|Plain|Instrumental|Not downloaded|Not found|Downloading.  Cover mode
Info card/Credits          CreditsCard + InfoCard + KeyValue (ExpressiveNowPlaying.kt, Surfaces.kt).  Cover mode.
  Props: Show composer, Show genre (booleans; the demo library has neither tag)
Info card/Artist           ArtistCard (ExpressiveNowPlaying.kt).  Cover mode.  Props: Name
Key value                  KeyValue (Surfaces.kt).  Props: Label, Value
Queue bar                  QueueBar (ExpressiveQueue.kt).  Cover mode
Queue row                  the queue's rows (ExpressiveQueue.kt)  State=Default|Dragging|Now playing.  Cover mode
"""
from __future__ import annotations

import math

from figbuild.kit import Doc, V
from expressive.foundations import DocPage, hrow, vcol, note, bind_dim, bind_dimension_tokens, cover_path, shape_path

REFS: dict[str, str] = {}

PKG = "app.attic.player.ui.expressive"
SRC = "ui/expressive/"
MAX_SET_W = 1640
PROGRESS = 78.5 / 232  # the demo song is at 1:18.5 of 3:52


# ───────────────────────────────────────────────────────────────── small helpers
def vname(props: dict) -> str:
    return ", ".join(f"{k}={v}" for k, v in props.items())


def txt(d: Doc, parent, chars: str, style: str, color: str, name: str, *, fill=False, lines=None, align=None, **kw):
    t = d.text(parent, chars, style=style, color=color, name=name, **kw)
    extra = {}
    if fill:
        extra["sizingH"] = "fill"
    if lines:
        extra.update(maxLines=lines, textAutoResize="truncate" if lines == 1 else "height")
    if align:
        extra["textAlign"] = align
    if extra:
        d.set(t, **extra)
    return t


def frame(d: Doc, parent, name: str, *, layout=None, w=None, h=None, gap=None, pad=None, fill=None, radius=None,
          cross=None, main=None, sizing=None, clip=None, **kw):
    return d.frame(parent, name=name, layout=layout, w=w, h=h, gap=gap, pad=pad, fill=fill, radius=radius,
                   cross=cross, main=main, sizing=sizing, clip=clip, **kw)


def comp(d: Doc, name: str, *, layout=None, w=None, h=None, gap=None, pad=None, fill=None, radius=None,
         cross=None, main=None, sizing=None, clip=None, cover=False, **kw) -> int:
    c = d.frame(None, name=name, layout=layout, w=w, h=h, gap=gap, pad=pad, fill=fill, radius=radius,
                cross=cross, main=main, sizing=sizing, clip=clip, component=True, x=-4000, y=0, **kw)
    if cover:
        d.set_mode(c, "Color", "Cover")
    return c


def inst(d: Doc, comp_name, parent=None, *, name=None, sizing=None, x=None, y=None, **variant) -> int:
    """An instance of a component or set, switched to the variant given as keyword arguments (underscores in
    keyword names stand for spaces). In a frame without auto layout pass x and y: switching variants keeps
    the position the default variant has in its set."""
    props = {k.replace("_", " "): str(v) for k, v in variant.items()}
    i = d.instance(comp_name, parent, name=name, sizing=sizing, props=props or None)
    if x is not None or y is not None:
        d.set(i, x=x or 0, y=y or 0)
    return i


def set_text(d: Doc, instance: int, prop: str, value: str):
    d.cmd("instance.setProperties", ids=[instance], properties={prop: value})


def set_cover(d: Doc, instance: int, *indices: int):
    """Put demo covers (DemoCovers index) into the Picture layers of an instance, in layer order."""
    pics = [i for i in d.find(instance, name="Picture", limit=50)]
    for pid, idx in zip(pics, indices if len(indices) > 1 else indices * len(pics)):
        d.cmd("image.replace", ids=[pid], path=cover_path(idx))


def spacer(d: Doc, parent, name="Spacer", w=None, h=None, fill_h=False):
    s = d.frame(parent, name=name, w=w or 1, h=h or 1)
    if fill_h:
        d.set(s, sizingH="fill")
    return s


def arc_path(cx, cy, r, start_deg, sweep_deg):
    a0, a1 = math.radians(start_deg), math.radians(start_deg + sweep_deg)
    x0, y0 = cx + r * math.cos(a0), cy + r * math.sin(a0)
    x1, y1 = cx + r * math.cos(a1), cy + r * math.sin(a1)
    large = 1 if sweep_deg > 180 else 0
    return f"M{x0:.2f} {y0:.2f} A{r:.2f} {r:.2f} 0 {large} 1 {x1:.2f} {y1:.2f}"


def wave_path(start, end, cy, wavelength, amplitude, step=1.5):
    """drawWave in PlayControls.kt: a sine from start to end, sampled every 1.5 units."""
    pts, x = [], start
    while x <= end:
        pts.append((x, cy + amplitude * math.sin(2 * math.pi * ((x - start) / wavelength))))
        x += step
    if pts and pts[-1][0] < end:
        pts.append((end, cy + amplitude * math.sin(2 * math.pi * ((end - start) / wavelength))))
    return "M" + " L".join(f"{x:.2f} {y:.2f}" for x, y in pts)


def stroke_vec(d: Doc, parent, name, path, color, width):
    v = d.vector(parent, name=name, path=path, stroke=color, stroke_w=width, x=0, y=0)
    d.set(v, strokeCap="round", strokeJoin="round")
    return v


PREVIEW_FRACTION = 0.62    # where the spec page shows the progress bars


def seek_active(fraction: float, width: float = 363) -> float:
    """The Seek bar's Active track width for a fraction: the handle's centre is 10 + (width - 20) x fraction and the
    wave stops 6dp before it (4dp clear plus half the handle)."""
    return 10 + (width - 20) * fraction - 6


def ring_arcs(d: Doc, ring_parent: int, fraction: float):
    """Turn a small play control's ring to `fraction`: elapsed in primary from 12 o'clock, a 14° gap at each end, the
    rest in outline variant. The arcs are 3dp rings (inner 22 of 25); their ends sit 3.7° past the stroke's centre line,
    as the code's round caps do."""
    cap = 3.66
    sweep = fraction * 360
    prog = [i for i in d.find(ring_parent, name="Progress", limit=4) if d.get(i)["type"] == "ELLIPSE"][0]
    track = [i for i in d.find(ring_parent, name="Track", limit=4) if d.get(i)["type"] == "ELLIPSE"][0]
    d.set(prog, arcStart=-90 - cap, arcSweep=max(sweep + 2 * cap, 0.1), arcInner=0.88)
    d.set(track, arcStart=-90 + sweep + 14 - cap, arcSweep=max(360 - sweep - 28 + 2 * cap, 0.1), arcInner=0.88)


def set_progress(d: Doc, inst_id: int, fraction: float):
    """Show `fraction` on a Progress/Linear, Progress/Wavy or Seek bar instance (or one nested in `inst_id`)."""
    act = None
    for name in ("Active", "Active track"):
        for i in d.find(inst_id, name=name, limit=8):
            if d.get(i)["name"] == name:
                act = i
                break
        if act:
            break
    width = d.get(inst_id)["width"]
    if d.get(act)["name"] == "Active track":
        d.set(act, width=round(seek_active(fraction, width), 2))
        return
    if fraction >= 1:
        d.set(act, width=width)
        for nm in ("Track", "Stop"):
            for i in d.find(inst_id, name=nm, limit=4):
                d.set(i, visible=False)
    else:
        d.set(act, width=round(max(fraction * width, 1), 2))


# ───────────────────────────────────────────────────────────────── component sets on a spec page
class Spec:
    """Builds variants, combines them, adds properties, links code and lays out the spec card."""

    def __init__(self, d: Doc, ctx: dict, page: DocPage):
        self.d, self.ctx, self.page = d, ctx, page
        ctx.setdefault("components", {})

    def make(self, name: str, title: str, description: str, file: str, composable: str, variants: list,
             builder, *, props=(), cover=False, preview=True, preview_wrap=None, single=False, expose=()):
        """variants: list of dicts of variant properties (the first is the default).
        builder(props) -> (component id, {layer key: layer id}).
        props: (property name, type, layer key, default, preferred) tuples."""
        d = self.d
        made, layers = [], []
        for vp in variants:
            cid, lay = builder(vp)
            if cover:
                d.set_mode(cid, "Color", "Cover")
            if not single:
                d.set(cid, name=vname(vp))
            made.append(cid)
            layers.append(lay)
        desc = f"{description} Compose: {SRC}{file} · {composable}."
        if single:
            sid = made[0]
            d.set(sid, name=name, description=desc)
            d.register(name, sid)
        else:
            sid = d.combine(name, made, description=desc)
            # FigCraft keeps no description on a component set, so each variant carries it (Dev Mode shows it).
            d.set(made, description=desc)
            d.set(sid, fill=V("Color/surface"), maxWidth=None)
            if cover:
                d.set_mode(sid, "Color", "Cover")
            if d.get(sid)["width"] > MAX_SET_W:
                d.set(sid, sizingH="fixed", width=MAX_SET_W)
        names = {}
        owner = sid
        for pname, ptype, key, default, preferred in [(*p, None, None)[:5] if len(p) < 5 else p for p in props]:
            holders = [lay[key] for lay in layers if key in lay]
            if not holders:
                continue
            args = {"layer": holders[0]}
            if default is not None:
                args["default"] = default
            if preferred:
                args["preferred"] = preferred
            pn = d.prop(owner, pname, ptype, **args)
            names[pname] = pn
            field = {"text": "characters", "boolean": "visible", "instanceSwap": "mainComponent"}[ptype]
            for lid in holders[1:]:
                d.bind_prop(lid, pn, field)
        for key in expose:
            for lay in layers:
                if key in lay:
                    try:
                        d.cmd("component.exposeInstance", id=lay[key])
                    except Exception:
                        pass
        d.props[name] = list(names)
        # Tokens before anything places an instance (a later binding resets instances' overrides).
        bind_dimension_tokens(d, self.ctx, roots=[sid])
        d.cmd("codeConnect.set", id=sid, language="compose", importPath=PKG, name=composable)
        self.ctx["components"][name] = {"id": sid, "variants": dict(zip([vname(v) for v in variants], made)),
                                        "props": names, "source": f"{SRC}{file} · {composable}"}

        card = self.page.card(title, description, f"Compose: {SRC}{file} · {composable}")
        d.cmd("node.reparent", ids=[sid], parent=card)
        if preview:
            width = preview_wrap or min(MAX_SET_W, max(600, d.get(sid)["width"]))
            pv = hrow(d, card, "Dark preview", gap=24, wrap=width, pad=24, fill="Color/surface", radius=12)
            for m in d.mode_collections:
                d.set_mode(pv, m, "Dark")
            if cover:
                d.set_mode(pv, "Color", "Cover")
            label = d.text(pv, "Dark", style="Doc/Label", color="Color/on-surface-variant", name="Mode label")
            d.set(label, sizingH="fixed", width=width - 48)
            text_keys = [(names[p[0]], p[2]) for p in props if p[1] == "text" and p[0] in names]
            for cid, lay in zip(made, layers):
                i = d.instance(cid, pv)
                over = {pn: d.get(lay[key])["characters"] for pn, key in text_keys if key in lay}
                if over:
                    d.cmd("instance.setProperties", ids=[i], properties=over)
        self.page.done(card)
        return sid


# ───────────────────────────────────────────────────────────────── Actions
def build_actions(d: Doc, ctx: dict):
    page = DocPage(d, "Components / Actions", "Actions",
                   "Buttons, toggles, the play controls, the connected button group, chips, switches and progress. "
                   "Everything is fully round while off; squared off means on. Labelled buttons are 40 or 56dp tall; "
                   "Material reserves a 48dp touch height around a 40dp button and a 32dp chip, so leave 4 and 8dp around them. "
                   "Icon buttons, toggles, the heart and the small play control include their 48dp touch target.")
    S = Spec(d, ctx, page)

    # Button ------------------------------------------------------------------------------------
    colours = {
        "Filled": ("Color/primary", "Color/on-primary", None),
        "Tonal": ("Color/secondary-container", "Color/on-secondary-container", None),
        "Outlined": (None, "Color/on-surface-variant", "Color/outline-variant"),
        "Text": (None, "Color/primary", None),
        "Text danger": (None, "Color/error", None),
    }
    labels = {"Filled": "Download lyrics", "Tonal": "Open lyrics", "Outlined": "Reorder songs", "Text": "Clear",
              "Text danger": "Delete playlist"}

    def button(vp):
        variant, size, icon, state, squared = vp["Style"], vp["Size"], vp["Icon"], vp["State"], vp["Squared"]
        medium = size == "Medium"
        h = 56 if medium else 40
        side = 12 if variant.startswith("Text") else (24 if medium else 16)
        container, content, border = colours[variant]
        if state == "Disabled":
            content = "Color/state/disabled-content"
            container = "Color/state/disabled-container" if container else None
            border = "Color/state/disabled-container" if border else None
        c = comp(d, "Button", layout="h", h=h, gap=8, cross="center", main="center", sizing=("hug", "fixed"),
                 pad=[0, side - 4 if icon == "Trailing" else side, 0, side - 4 if icon == "Leading" else side],
                 fill=container, stroke=border, stroke_w=1 if border else None)
        d.set(c, minWidth=58)
        if squared == "True":
            bind_dim(d, c, "cornerRadius", "radius/large" if medium else "radius/medium")
        else:
            bind_dim(d, c, "cornerRadius", "radius/full")
        lay = {}
        isz = 24 if medium else 20
        if icon == "Leading":
            lay["lead"] = d.icon(c, "play_arrow", size=isz, color=content, filled=True, name="Leading icon")
        lay["label"] = txt(d, c, labels[variant], "Kit/Button medium" if medium else "Kit/Button", content, "Label")
        if icon == "Trailing":
            lay["trail"] = d.icon(c, "open_in_full", size=isz, color=content, name="Trailing icon")
        return c, lay

    vs = []
    for variant in colours:
        for size in ("Small", "Medium"):
            for icon in ("None", "Leading", "Trailing"):
                vs.append(dict(Style=variant, Size=size, Icon=icon, State="Enabled", Squared="False"))
    for variant in ("Filled", "Tonal", "Outlined", "Text"):
        for size in ("Small", "Medium"):
            for icon in ("None", "Leading"):
                vs.append(dict(Style=variant, Size=size, Icon=icon, State="Disabled", Squared="False"))
    for size in ("Small", "Medium"):
        vs.append(dict(Style="Filled", Size=size, Icon="Leading", State="Enabled", Squared="True"))
    S.make("Button", "Button",
           "A labelled button: filled for the one action a screen is about, tonal, outlined, text, or a text button in "
           "the error colour for a destructive confirm. Small is 40dp, Medium 56dp. Squared is a Play button that reads "
           "Pause (12 or 16dp corners). Disabled draws the container at 12 % and the label and glyph at 38 % of on-surface, "
           "with or without a leading glyph. A leading glyph is filled by default (play glyphs); pass leadingFilled = false "
           "for others, as Add songs, Download and Share do.",
           "Buttons.kt", "ExpressiveButton", vs, button,
           props=[("Label", "text", "label", None, None), ("Leading icon", "instanceSwap", "lead", None, None),
                  ("Trailing icon", "instanceSwap", "trail", None, None)])

    # Icon button --------------------------------------------------------------------------------
    ib_colours = {"Standard": (None, "Color/on-surface-variant", None),
                  "Filled": ("Color/primary", "Color/on-primary", None),
                  "Tonal": ("Color/secondary-container", "Color/on-secondary-container", None),
                  "Outlined": (None, "Color/on-surface-variant", "Color/outline-variant")}

    def ib_size(medium, width):
        h = 56 if medium else 40
        w = {"Narrow": 48 if medium else 32, "Default": h, "Wide": 72 if medium else 52}[width]
        return w, h

    def icon_button(vp, symbol="more_vert"):
        variant, medium, width, state = vp["Style"], vp["Size"] == "Medium", vp["Width"], vp["State"]
        w, h = ib_size(medium, width)
        container, content, border = ib_colours[variant]
        if medium:
            c = comp(d, "Icon button", layout="h", w=w, h=h, cross="center", main="center", fill=container,
                     stroke=border, stroke_w=1 if border else None)
            box = c
        else:
            c = comp(d, "Icon button", layout="h", w=max(48, w), h=48, cross="center", main="center")
            box = frame(d, c, "Container", layout="h", w=w, h=h, cross="center", main="center", fill=container,
                        stroke=border, stroke_w=1 if border else None)
        bind_dim(d, box, "cornerRadius", "radius/full")
        ic = d.icon(box, symbol, color=content, name="Icon")
        if state == "Disabled":
            d.set(ic, opacity=0.38)
        return c, {"icon": ic}

    vs = [dict(Style=v, Size=s, Width=w, State="Enabled") for v in ib_colours for s in ("Default", "Medium")
          for w in ("Default", "Narrow", "Wide")]
    vs += [dict(Style=v, Size="Default", Width="Default", State="Disabled") for v in ib_colours]
    S.make("Icon button", "Icon button",
           "A glyph in a 40dp container (56 with Medium) inside a 48dp touch target. Standard has no container and uses "
           "on-surface-variant; inside a toolbar or over art the tint changes (LocalStandardIconTint). Narrow and Wide "
           "are the kit's .ib.narrow and .ib.wide. Disabled draws the glyph at 38 %.",
           "Buttons.kt", "ExpressiveIconButton", vs, icon_button,
           props=[("Icon", "instanceSwap", "icon", None, None)])

    # Toggle button ------------------------------------------------------------------------------
    def toggle(vp, symbol="shuffle"):
        on, medium, tonal = vp["Checked"] == "True", vp["Size"] == "Medium", vp["Off style"] == "Tonal"
        h = 56 if medium else 40
        if on:
            container, content = "Color/primary", "Color/on-primary"
        elif tonal:
            container, content = "Color/secondary-container", "Color/on-secondary-container"
        else:
            container, content = None, "Color/on-surface-variant"
        if medium:
            c = comp(d, "Toggle button", layout="h", w=h, h=h, cross="center", main="center", fill=container)
            box = c
        else:
            c = comp(d, "Toggle button", layout="h", w=48, h=48, cross="center", main="center")
            box = frame(d, c, "Container", layout="h", w=h, h=h, cross="center", main="center", fill=container)
        bind_dim(d, box, "cornerRadius", ("radius/large" if medium else "radius/medium") if on else "radius/full")
        ic = d.icon(box, symbol, color=content, filled=on, name="Icon")
        # Two swap properties: the glyph while off and its filled form while on. One property for both would
        # reset the on variants to the outlined default and drop their colour.
        return c, {"icon_on" if on else "icon_off": ic}

    vs = [dict(Checked=ch, Size=s, **{"Off style": o}) for o in ("Standard", "Tonal") for s in ("Default", "Medium")
          for ch in ("False", "True")]
    S.make("Toggle button", "Toggle button",
           "An icon button that stays on: round while off, squared off (12 at 40dp, 16 at 56dp), filled with primary and "
           "its glyph filled while on. The corner morphs on fastSpatial, the colour on fastEffects. Tonal off style is the "
           "collection header's shuffle (tonalWhenOff). Used for shuffle and repeat, the Library grid switch and Add songs' tick.",
           "Buttons.kt", "ExpressiveToggleButton", vs, toggle,
           props=[("Icon", "instanceSwap", "icon_off", None, None),
                  ("Icon when on", "instanceSwap", "icon_on", None, None)])

    # Heart button -------------------------------------------------------------------------------
    def heart(vp):
        on = vp["Favourite"] == "True"
        c = comp(d, "Heart button", layout="h", w=48, h=48, cross="center", main="center")
        box = frame(d, c, "Container", layout="h", w=40, h=40, cross="center", main="center")
        bind_dim(d, box, "cornerRadius", "radius/full")
        ic = d.icon(box, "favorite", color="Color/primary" if on else "Color/on-surface-variant", filled=on, name="Icon")
        return c, {"icon": ic}

    S.make("Heart button", "Heart button",
           "The favourite: an outline, then filled in primary, with no container. It pops from 80 % to full size when it "
           "changes. It describes itself (\"Add to favourites\" / \"Remove from favourites\").",
           "Buttons.kt", "HeartButton", [dict(Favourite="False"), dict(Favourite="True")], heart)

    # Play button --------------------------------------------------------------------------------
    def play(vp):
        size, playing, ring, tonal = vp["Size"], vp["Playing"] == "True", vp["Ring"] == "True", vp["Tonal"] == "True"
        container = "Color/secondary-container" if tonal else "Color/primary"
        content = "Color/on-secondary-container" if tonal else "Color/on-primary"
        glyph = "pause" if playing else "play_arrow"
        if size == "Small":
            c = comp(d, "Play button", w=52, h=52)
            if ring:
                for nm, col in (("Track", "Color/outline-variant"), ("Progress", "Color/primary")):
                    e = d.ellipse(c, name=nm, w=50, h=50, x=1, y=1, fill=col)
                ring_arcs(d, c, PROGRESS)
            box = frame(d, c, "Container", layout="h", w=36, h=36, x=8, y=8, cross="center", main="center", fill=container)
            bind_dim(d, box, "cornerRadius", "radius/play-small-playing" if playing else "radius/full")
            d.icon(box, glyph, size=22, color=content, filled=True, name="Glyph")
        elif size == "Large":
            c = comp(d, "Play button", layout="h", w=56, h=56, cross="center", main="center", fill=container)
            bind_dim(d, c, "cornerRadius", "radius/play-large-playing" if playing else "radius/full")
            d.icon(c, glyph, size=32, color=content, filled=True, name="Glyph")
        else:
            c = comp(d, "Play button", layout="h", w=128 if playing else 152, h=80, cross="center", main="center", fill=container)
            bind_dim(d, c, "cornerRadius", "radius/extra-large" if playing else "radius/full")
            d.icon(c, glyph, size=44, color=content, filled=True, name="Glyph")
        return c, {}

    vs = [dict(Size="Small", Playing=p, Ring=r, Tonal="False") for r in ("True", "False") for p in ("False", "True")]
    vs += [dict(Size="Large", Playing=p, Ring="False", Tonal=t) for t in ("False", "True") for p in ("False", "True")]
    vs += [dict(Size="Extra large", Playing=p, Ring="False", Tonal="False") for p in ("False", "True")]
    S.make("Play button", "Play button",
           "Round while paused, squared off while playing; the glyph cross-fades. Small is 36dp in a 52dp box, with the "
           "progress ring where there is no seek bar (mini player, compact header, widget): elapsed in primary from the "
           "top, the rest in outline variant, a 14° gap at each end. The ring's Progress and Track layers are arcs: "
           "ring_arcs() in components_core.py sets them for a fraction (sweep = fraction x 360°). Large is 56dp (a section's play all, the lyrics bar). "
           "Extra large is Now Playing's: 152 x 80 paused, 128 x 80 playing.",
           "PlayControls.kt", "PlayButton", vs, play)

    # Skip button --------------------------------------------------------------------------------
    def skip(vp):
        c = comp(d, "Skip button", layout="h", w=76, h=80, cross="center", main="center", fill="Color/secondary-container")
        bind_dim(d, c, "cornerRadius", "radius/full")
        d.icon(c, "skip_previous" if vp["Direction"] == "Previous" else "skip_next", size=36,
               color="Color/on-secondary-container", filled=True, name="Glyph")
        return c, {}

    S.make("Skip button", "Skip button", "Previous and next in Now Playing: a 76 x 80 tonal pill with a 36dp filled glyph.",
           "PlayControls.kt", "SkipButton", [dict(Direction="Previous"), dict(Direction="Next")], skip, cover=True)

    # Collection play button --------------------------------------------------------------------
    def cplay(vp):
        playing = vp["Playing"] == "True"
        c = comp(d, "Collection play button", layout="v", w=159, h=144, gap=4, cross="center", main="center",
                 fill="Color/primary")
        bind_dim(d, c, "cornerRadius", "radius/extra-large" if playing else "radius/extra-extra-large")
        d.icon(c, "pause" if playing else "play_arrow", size=40, color="Color/on-primary", filled=True, name="Glyph")
        lab = txt(d, c, "Pause" if playing else "Play", "Kit/Play label", "Color/on-primary", "Label")
        return c, {"label": lab}

    S.make("Collection play button", "Collection play button",
           "The collection header's Play (.cplay): glyph over label, 48dp corners while idle, 28 (the cover's) while this "
           "collection plays, when it reads Pause. It fills the cluster beside the 208dp cover, above shuffle and add to "
           "playlist, so its size comes from there (159 x 144 on a 411dp screen).",
           "ExpressiveCollections.kt", "CollectionPlayButton", [dict(Playing="False"), dict(Playing="True")], cplay,
           cover=True)

    # Extended FAB ------------------------------------------------------------------------------
    def efab(vp):
        exp = vp["Expanded"] == "True"
        c = comp(d, "Extended FAB", layout="h", h=56, gap=12, cross="center", main="center",
                 pad=[0, 20, 0, 16] if exp else 0, fill="Color/primary-container", sizing=("hug", "fixed"))
        if not exp:
            d.set(c, sizingH="fixed", width=56)
        bind_dim(d, c, "cornerRadius", "radius/large")
        d.cmd("style.apply", ids=[c], style=d.effect_styles["Elevation/Level 3"])
        ic = d.icon(c, "add", color="Color/on-primary-container", name="Icon")
        lay = {"icon": ic}
        if exp:
            lay["label"] = txt(d, c, "New playlist", "Kit/FAB label", "Color/on-primary-container", "Label")
        return c, lay

    S.make("Extended FAB", "Extended FAB",
           "Primary container, 16dp corners, the 6dp shadow. It shrinks to its glyph once the list has scrolled (Library's "
           "playlist list).", "Buttons.kt", "ExpressiveExtendedFab", [dict(Expanded="True"), dict(Expanded="False")], efab,
           props=[("Label", "text", "label", None, None), ("Icon", "instanceSwap", "icon", None, None)])

    # Split button ------------------------------------------------------------------------------
    def split(vp):
        c = comp(d, "Split button", layout="h", h=40, gap=2, sizing=("hug", "fixed"))
        a = frame(d, c, "Action", layout="h", h=40, gap=8, pad=[0, 12, 0, 16], cross="center",
                  fill="Color/secondary-container", sizing=("hug", "fixed"), cornerRadius=[20, 4, 4, 20])
        ic = d.icon(a, "sort", size=20, color="Color/on-secondary-container", name="Leading icon")
        lab = txt(d, a, "Title", "Kit/Button", "Color/on-secondary-container", "Label")
        m = frame(d, c, "Menu", layout="h", h=40, pad=[0, 12, 0, 10], cross="center", fill="Color/secondary-container",
                  sizing=("hug", "fixed"), cornerRadius=[4, 20, 20, 4])
        d.icon(m, "keyboard_arrow_down", size=20, color="Color/on-secondary-container", name="Menu icon")
        return c, {"label": lab, "icon": ic}

    S.make("Split button", "Split button",
           "An action and its menu, 2dp apart: outer corners round, inner ones 4, on the tonal container. Library's song "
           "sort: the label cycles Title, Artist, Recently added; the arrow lists them.",
           "Buttons.kt", "SplitButton", [{}], split, single=True,
           props=[("Label", "text", "label", None, None), ("Leading icon", "instanceSwap", "icon", None, None)])

    # Connected button group --------------------------------------------------------------------
    def segment(vp):
        pos, sel, cell = vp["Position"], vp["Selected"] == "True", vp["On cell"] == "True"
        start = 20 if sel or pos == "First" else 8
        end = 20 if sel or pos == "Last" else 8
        fill = "Color/primary" if sel else ("Color/surface-container-highest" if cell else "Color/surface-container-high")
        content = "Color/on-primary" if sel else "Color/on-surface-variant"
        c = comp(d, "Segment", layout="h", w=120, h=40, gap=6, cross="center", main="center", fill=fill,
                 cornerRadius=[start, end, end, start])
        if sel:
            d.icon(c, "check", size=18, color=content, name="Check")
        lab = txt(d, c, "Albums", "Kit/Button" if sel else "Label/Large", content, "Label")
        return c, {"label": lab}

    vs = [dict(Position=p, Selected=s, **{"On cell": o}) for o in ("False", "True") for p in ("First", "Middle", "Last")
          for s in ("False", "True")]
    S.make("Connected button group/Segment", "Connected button group: segment",
           "One segment. Ends are round and inner corners 8; the selected segment goes fully round, fills with primary and "
           "shows a tick. On cell is the unselected colour on a surface container cell (Settings): surface container "
           "highest instead of high.", "Navigation.kt", "ConnectedButtonGroup", vs, segment,
           props=[("Label", "text", "label", None, None)])
    seg_set = d.comp["Connected button group/Segment"]
    seg_label = ctx["components"]["Connected button group/Segment"]["props"]["Label"]

    group_labels = {4: ["All", "Albums", "Artists", "Folders"], 3: ["Match phone", "Dark", "Light"],
                    2: ["Expressive", "Comfort"]}

    def group(vp):
        n, sel, cell = int(vp["Options"]), int(vp["Selected"]), vp["On cell"]
        # Row weights as a grid: one fr track per segment, the selected one 1.15fr (Modifier.weight in the code), so
        # the segments fill whatever width the group gets and the selected one stays wider.
        c = comp(d, "Connected button group", w=411, h=48)
        d.set(c, layout="grid", gridColumns=n, gridRows=1, columnGap=2, padding=[4, 16, 4, 16],
              columnTracks=["1.15fr" if i + 1 == sel else "1fr" for i in range(n)])
        lay = {}
        for i in range(n):
            pos = "First" if i == 0 else ("Last" if i == n - 1 else "Middle")
            s = inst(d, seg_set, c, name=f"Segment {i + 1}", Position=pos, Selected=str(i + 1 == sel), On_cell=cell)
            d.set(s, gridColumn=i, gridRow=0)
            d.set(s, sizingH="fill")
            set_text(d, s, seg_label, group_labels[n][i])
            lay[f"seg{i}"] = s
        return c, lay

    vs = [dict(Options=str(n), Selected=str(i), **{"On cell": cell}) for n, cell in
          (("4", "False"), ("3", "True"), ("2", "True"), ("4", "True"), ("3", "False"), ("2", "False"))
          for i in range(1, int(n) + 1)]
    S.make("Connected button group", "Connected button group",
           "A one-of-N switch that always has one selected: Home's filter (four), Settings' theme (three) and style (two) "
           "on a surface container cell, Recap's period (four, labels narrowed to 86). Segments are 40dp tall and 2dp "
           "apart; the selected one takes 1.15 of the others' width. The group is a one-row grid with one fr column per "
           "segment (1.15fr for the selected one, as Modifier.weight does), so the segments fill any width the group is "
           "given. The component includes the code's 16dp side and 4dp vertical padding. Choose Selected and On cell on "
           "the group, not on a segment. Recap's period widens the selected segment to 1.3: there the selected segment "
           "is set to a fixed width and the others share the rest. Edit a segment's label through the nested instance.",
           "Navigation.kt", "ConnectedButtonGroup", vs, group, expose=("seg0", "seg1", "seg2", "seg3"))

    # Filter chip -------------------------------------------------------------------------------
    def chip(vp):
        sel = vp["Selected"] == "True"
        c = comp(d, "Filter chip", layout="h", h=32, gap=8, cross="center", sizing=("hug", "fixed"),
                 pad=[0, 16, 0, 8 if sel else 16], fill="Color/secondary-container" if sel else None,
                 stroke=None if sel else "Color/outline-variant", stroke_w=None if sel else 1)
        bind_dim(d, c, "cornerRadius", "radius/small")
        if sel:
            d.icon(c, "check", size=18, color="Color/on-secondary-container", name="Check")
        lab = txt(d, c, "Albums", "Label/Large", "Color/on-secondary-container" if sel else "Color/on-surface-variant", "Label")
        return c, {"label": lab}

    S.make("Filter chip", "Filter chip",
           "A filter that can be cleared: 32dp, 8dp corners, an outline while off; tonal with a tick while on. The code "
           "shows the tick only when selected, so there is no chip with an icon while off. A one-of-N switch is a "
           "connected button group instead.", "Navigation.kt", "ExpressiveFilterChip",
           [dict(Selected="False"), dict(Selected="True")], chip, props=[("Label", "text", "label", None, None)])

    # Switch -------------------------------------------------------------------------------------
    def switch(vp):
        on = vp["Checked"] == "True"
        c = comp(d, "Switch", w=52, h=32)
        track = frame(d, c, "Track", w=52, h=32, x=0, y=0, fill="Color/primary" if on else "Color/surface-container-highest",
                      stroke=None if on else "Color/outline", stroke_w=None if on else 2)
        if not on:
            d.set(track, strokeAlign="inside")
        bind_dim(d, track, "cornerRadius", "radius/full")
        th = frame(d, track, "Thumb", layout="h", w=24, h=24, x=24 if on else 4, y=4, cross="center", main="center",
                   fill="Color/on-primary" if on else "Color/outline")
        bind_dim(d, th, "cornerRadius", "radius/full")
        d.icon(th, "check" if on else "close", size=16,
               color="Color/on-primary-container" if on else "Color/surface-container-highest", name="Thumb icon")
        return c, {}

    S.make("Switch", "Switch",
           "Material's switch with a tick in the thumb while on and a cross while off, 52 x 32. Every row that holds it is "
           "toggleable and passes no onCheckedChange, so the switch takes no 48dp touch target of its own: the row is the "
           "target. Settings' Autoplay row (4dp below the row's top) and the first run's lyrics row.", "Surfaces.kt", "ExpressiveSwitch",
           [dict(Checked="False"), dict(Checked="True")], switch)

    def radio(vp):
        on = vp["Selected"] == "True"
        c = comp(d, "Radio", w=48, h=48)
        col = "Color/primary" if on else "Color/on-surface-variant"
        ring = d.ellipse(c, name="Ring", w=18, h=18, x=15, y=15, fill="none")
        d.set(ring, stroke=V(col), strokeWeight=2, strokeAlign="center")
        if on:
            d.ellipse(c, name="Dot", w=12, h=12, x=18, y=18, fill=col)
        return c, {}

    S.make("Radio", "Radio", "Material's radio button, 20dp in a 48dp target: the language and sleep timer pickers' rows.",
           "Surfaces.kt", "ExpressiveRadio", [dict(Selected="False"), dict(Selected="True")], radio)

    # Progress ----------------------------------------------------------------------------------
    W = 347

    def track_parts(c, h):
        """The rest of the track (fills what Active leaves) and the stop dot at its end."""
        tr = d.rect(c, name="Track", w=W, h=4, fill="Color/secondary-container")
        bind_dim(d, tr, "cornerRadius", "radius/full")
        d.set(tr, sizingH="fill")
        stop = d.ellipse(c, name="Stop", w=4, h=4, fill="Color/primary")
        d.set(stop, absolute=True)
        d.set(stop, x=W - 4, y=(h - 4) / 2, constraints={"horizontal": "right", "vertical": "center"})

    def linear(vp):
        c = comp(d, "Progress/Linear", layout="h", w=W, h=4, gap=4, cross="center")
        a = d.rect(c, name="Active", w=round(PREVIEW_FRACTION * W, 2), h=4, fill="Color/primary")
        bind_dim(d, a, "cornerRadius", "radius/full")
        track_parts(c, 4)
        return c, {}

    S.make("Progress/Linear", "Linear progress",
           "Material's linear progress with a 4dp gap and the stop dot: saving lyrics for every song, analysis once it has "
           "stopped. Set the fraction with the Active layer's width (fraction x the bar's width); the track fills the "
           "rest. At 100 % hide Track and Stop. There is no Material slider in the app; the seek bar (Playback) is the "
           "one slider.", "PlayControls.kt", "LinearProgress", [{}], linear, single=True)

    def wavy(vp):
        run = vp["Running"] == "True"
        c = comp(d, "Progress/Wavy", layout="h", w=W, h=10, gap=4, cross="center")
        a = frame(d, c, "Active", w=round(PREVIEW_FRACTION * W, 2), h=10, clip=True)
        stroke_vec(d, a, "Wave", wave_path(1.2, W - 1.2, 5, 10, 2 if run else 0), "Color/primary", 2.4)
        track_parts(c, 10)
        return c, {}

    S.make("Progress/Wavy", "Wavy progress",
           "For a long job that is running (song analysis): the active part is a wave 2dp high and 10dp long that travels; "
           "it lies flat when the job holds. 10dp tall. Set the fraction with the Active frame's width, which crops the "
           "wave; the track fills the rest.", "PlayControls.kt", "WavyLinearProgress",
           [dict(Running="True"), dict(Running="False")], wavy)


# ───────────────────────────────────────────────────────────────── Lists and cards
COVER_SIZES = {"Album": [48, 56, 64, 72, 112, 120, 148, 208], "Artist": [28, 48, 56, 88, 112], "Mosaic": [56, 64, 72, 148],
               "Favourites": [56, 148]}


def cover_radius(size):
    """ExpressiveShapes.coverRadius."""
    for limit, tok in ((48, "radius/small"), (64, "radius/medium"), (128, "radius/large"), (180, "radius/large-increased")):
        if size <= limit:
            return tok
    return "radius/extra-large"


def build_lists(d: Doc, ctx: dict):
    page = DocPage(d, "Components / Lists and cards", "Lists and cards",
                   "Covers, list rows and their group cells, headers, tiles and the cards Home is built from. Rows are "
                   "transparent: they sit in group cells on surface container, 2dp apart, outer corners 20 and inner 4. "
                   "Put a cover into an instance with image.replace on its Picture layer (set_cover in this module).")
    S = Spec(d, ctx, page)

    # Cover --------------------------------------------------------------------------------------
    def cover(vp):
        kind, size = vp["Kind"], int(vp["Size"])
        scale = {"constraints": {"horizontal": "scale", "vertical": "scale"}}
        if kind == "Artist":
            c = comp(d, "Cover", w=size, h=size)
            m = d.vector(c, name="Mask", path=shape_path("Cookie 9", size), fill="Color/surface-container-highest",
                         x=0, y=0, w=size, h=size, **scale)
            d.set(m, isMask=True)
            d.rect(c, name="No art background", w=size, h=size, x=0, y=0, fill="Color/surface-container-highest", **scale)
        else:
            c = comp(d, "Cover", w=size, h=size, clip=True,
                     fill="Color/tertiary-container" if kind == "Favourites" else "Color/surface-container-highest")
            bind_dim(d, c, "cornerRadius", cover_radius(size))
        lay = {}
        if kind == "Favourites":
            g = d.icon(c, "favorite", size=size * 0.5, color="Color/on-tertiary-container", filled=True, name="Heart")
            d.set(g, x=size / 4, y=size / 4, constraints={"horizontal": "scale", "vertical": "scale"})
            return c, lay
        g = d.icon(c, "music_note", size=round(size * 0.46, 1), color="Color/on-surface-variant", name="No art")
        d.set(g, x=round(size * 0.27, 1), y=round(size * 0.27, 1), constraints={"horizontal": "scale", "vertical": "scale"})
        if kind == "Mosaic":
            half = size / 2
            for i, idx in enumerate((0, 8, 9, 3)):
                d.image(c, cover_path(idx), name=f"Picture {i + 1}", w=half, h=half, x=(i % 2) * half, y=(i // 2) * half)
        else:
            d.image(c, cover_path(11 if kind == "Artist" else 0), name="Picture", w=size, h=size, x=0, y=0)
        for pid in d.find(c, name="Picture", limit=8):
            d.set(pid, constraints={"horizontal": "scale", "vertical": "scale"})
        return c, lay

    vs = [dict(Kind=k, Size=str(s)) for k, sizes in COVER_SIZES.items() for s in sizes]
    S.make("Cover", "Cover",
           "Albums, playlists and folders are rounded squares whose corner follows their size (8 up to 48dp, 12 up to 64, "
           "16 up to 128, 20 up to 180, 28 above). Artists are the nine-lobed cookie at every size. A folder or playlist is "
           "a 2 x 2 mosaic of covers from different albums. Favourites is the filled heart on tertiary container, the one "
           "place that colour is spent. Artist 28 is the album header's byline. Without art the music-note placeholder "
           "shows: hide the Picture layer.",
           "Cover.kt", "Cover", vs, cover)

    # Row trailing parts ------------------------------------------------------------------------
    def drag(vp):
        c = comp(d, "List row/Drag handle", layout="h", w=48, h=48, cross="center", main="center")
        d.icon(c, "drag_handle", color="Color/on-surface-variant", name="Icon")
        return c, {}

    S.make("List row/Drag handle", "Drag handle",
           "Replaces the kebab in the queue and a playlist's reorder mode: \"Drag {title} to reorder\".",
           "Lists.kt", "RowDragHandle", [{}], drag, single=True)

    def count(vp):
        c = comp(d, "List row/Count", layout="h", pad=[0, 12, 0, 0], cross="center", sizing=("hug", "hug"))
        t = txt(d, c, "187 plays", "Kit/Figures", "Color/on-surface-variant", "Count")
        return c, {"count": t}

    S.make("List row/Count", "Row count", "A figure at the end of a row in tabular figures: Recap's \"187 plays\".",
           "Lists.kt", "RowCount", [{}], count, single=True, props=[("Count", "text", "count", None, None)])

    ib = d.comp["Icon button"]
    preferred_trailing = [d.comp["Icon button"], d.comp["List row/Drag handle"], d.comp["Toggle button"],
                          d.comp["List row/Count"], d.comp["Heart button"]]

    # Song row ----------------------------------------------------------------------------------
    def song_row(vp):
        lead, state = vp["Lead"], vp["State"]
        current = state.startswith("Current")
        playing = state == "Current playing"
        lifted = state == "Lifted"
        fill = "Color/surface-container-highest" if lifted else ("Color/secondary-container" if current else None)
        c = comp(d, "List row/Song", layout="h", w=379, gap=12, pad=[8, 4, 8, 12], cross="center", fill=fill,
                 sizing=("fixed", "hug"))
        d.set(c, minHeight=64)
        if lifted:
            bind_dim(d, c, "cornerRadius", "radius/large-increased")
            d.cmd("style.apply", ids=[c], style=d.effect_styles["Elevation/Level 3"])
        if lead.startswith("Ranked"):
            txt(d, c, "1", "Kit/Row rank", "Color/on-surface-variant", "Rank", align="center")
            d.set(d.find(c, name="Rank")[0], sizingH="fixed", width=18)
        if lead in ("Cover", "Ranked cover"):
            box = frame(d, c, "Lead", w=48, h=48)
            inst(d, "Cover", box, name="Cover", Kind="Album", Size="48", x=0, y=0)
            if current:
                v = d.rect(box, name="Veil", w=48, h=48, x=0, y=0, fill="Color/state/now-playing-veil")
                bind_dim(d, v, "cornerRadius", "radius/small")
                w_ = inst(d, "Wave", box, name="Wave", Playing=str(playing))
                d.set(w_, x=14, y=19)
        elif lead == "Ranked artist":
            inst(d, "Cover", c, name="Lead", Kind="Artist", Size="48")
        elif lead == "Number":
            box = frame(d, c, "Lead", layout="h", w=28, h=24, cross="center", main="center")
            if current:
                inst(d, "Wave", box, name="Wave", Playing=str(playing))
            else:
                txt(d, box, "1", "Kit/Row number", "Color/on-surface-variant", "Number")
        texts = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        title = txt(d, texts, "Lights Over the Water", "Kit/Row title current" if current else "Kit/Row title",
                    "Color/on-surface", "Title", fill=True, lines=1)
        sub = txt(d, texts, "Mira Okafor", "Body/Medium",
                  "Color/on-secondary-container" if current else "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        if lifted:
            tr = d.instance("List row/Drag handle", c, name="Trailing")
        else:
            tr = inst(d, ib, c, name="Trailing", Style="Standard", Size="Default", Width="Default", State="Enabled")
        # The lifted row keeps its drag handle out of the Trailing property, whose default is the kebab.
        return c, {"title": title, "sub": sub, ("lifted trail" if lifted else "trail"): tr}

    vs = [dict(Lead="Cover", State=s) for s in ("Default", "Current playing", "Current paused", "Lifted")]
    vs += [dict(Lead="Number", State=s) for s in ("Default", "Current playing", "Current paused")]
    vs += [dict(Lead="Ranked cover", State=s) for s in ("Default", "Current playing")]
    vs += [dict(Lead="Ranked artist", State="Default"), dict(Lead="None", State="Default")]
    # The wave component is needed inside rows; it lives on the Playback page but is built first.
    build_wave(d, ctx)
    S.make("List row/Song", "Song row",
           "A song: lead, title over subtitle, a trailing control. 64dp minimum, 12 start and 4 end. The current song gets "
           "the secondary container fill, a bold title and the wave over its cover (or in place of its number), moving "
           "while playing. Lifted is the row being dragged: 20dp corners, the highest container and the 6dp shadow. Swap "
           "Trailing for the drag handle, the Add toggle or a count. The code's row has no favourite mark; favourites show "
           "in the track menu and Now Playing.", "Lists.kt", "SongRow", vs, song_row,
           props=[("Title", "text", "title", None, None), ("Subtitle", "text", "sub", None, None),
                  ("Show subtitle", "boolean", "sub", True, None),
                  ("Trailing", "instanceSwap", "trail", None, preferred_trailing)])

    # Collection row ----------------------------------------------------------------------------
    rows = {"Album": ("Album", "Paper Lanterns", "Album, Mira Okafor"), "Artist": ("Artist", "Mira Okafor", "Artist"),
            "Playlist": ("Mosaic", "Late drive", "Playlist, 14 songs"), "Folder": ("Mosaic", "Late Night", "Playlist, 11 songs"),
            "Favourites": ("Favourites", "Favourites", "Playlist, 42 songs")}

    def coll_row(vp):
        kind, title_, sub_ = rows[vp["Kind"]]
        if vp["Mosaic"] == "False" and kind == "Mosaic":
            kind = "Album"
        c = comp(d, "List row/Collection", layout="h", w=379, gap=12, pad=[8, 4, 8, 12], cross="center",
                 sizing=("fixed", "hug"))
        d.set(c, minHeight=72)
        lead = inst(d, "Cover", c, name="Lead", Kind=kind, Size="56")
        texts = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        t = txt(d, texts, title_, "Kit/Row title", "Color/on-surface", "Title", fill=True, lines=1)
        s = txt(d, texts, sub_, "Body/Medium", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        spacer(d, c, "End space", w=8, h=8)
        return c, {"title": t, "sub": s, "lead": lead}

    S.make("List row/Collection", "Collection row",
           "A collection in Library's list: a 56dp lead whose shape says what it is, then title and subtitle. 72dp "
           "minimum. Playlists, folders and Favourites read \"Playlist, n songs\"; an album \"Album, {artist}\". "
           "A playlist or folder with songs from fewer than four albums shows one cover, not the mosaic: Mosaic=False. "
           "The Lead cover is exposed, so its picture layers show on the row's instances.",
           "Lists.kt", "CollectionRow",
           [dict(Kind=t, Mosaic=str(t in ("Playlist", "Folder"))) for t in rows]
           + [dict(Kind=t, Mosaic="False") for t in ("Playlist", "Folder")], coll_row,
           props=[("Title", "text", "title", None, None), ("Subtitle", "text", "sub", None, None)], expose=("lead",))

    # Setting row -------------------------------------------------------------------------------
    sw = d.comp["Switch"]

    def setting(vp):
        leading, lines, trailing, status = vp["Leading"], vp["Lines"], vp["Trailing"], vp["Status line"] == "True"
        two = lines == "Two"
        c = comp(d, "List row/Setting", layout="h", w=379, gap=16, pad=[12, 16, 12, 16],
                 cross="min" if two else "center", sizing=("fixed", "hug"))
        d.set(c, minHeight=64)
        lay = {}
        if leading == "Icon":
            circ = frame(d, c, "Icon circle", layout="h", w=40, h=40, cross="center", main="center",
                         fill="Color/secondary-container")
            bind_dim(d, circ, "cornerRadius", "radius/full")
            lay["icon"] = d.icon(circ, "autoplay", size=20, color="Color/on-secondary-container", name="Icon")
        texts = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        lay["title"] = txt(d, texts, "Autoplay" if leading == "Icon" else "Coil", "Kit/Setting title",
                           "Color/on-surface", "Title", fill=True)
        if two:
            lay["sum"] = txt(d, texts, "When a queue reaches its last song, radio keeps going with similar songs."
                             if leading == "Icon" else "Apache License 2.0", "Body/Medium",
                             "Color/on-surface-variant", "Summary", fill=True)
        if status:
            sbox = frame(d, texts, "Status box", layout="v", pad=[8, 0, 0, 0], sizing=("fill", "hug"))
            lay["status"] = txt(d, sbox, "Saving the backup…", "Body/Small", "Color/primary", "Status", fill=True)
        if trailing != "None":
            holder = frame(d, c, "Trailing", layout="v", pad=[4 if two else 0, 0, 0, 0], sizing=("hug", "hug"))
            inst(d, sw, holder, name="Switch", Checked=str(trailing == "Switch on"))
        return c, lay

    vs = [dict(Leading="Icon", Lines=l, Trailing=t, **{"Status line": "False"}) for l in ("Two", "One")
          for t in ("Switch on", "Switch off", "None")]
    vs += [dict(Leading="Icon", Lines="Two", Trailing="None", **{"Status line": "True"}),
           dict(Leading="None", Lines="Two", Trailing="None", **{"Status line": "False"})]
    S.make("List row/Setting", "Setting row",
           "Settings' rows (SetIconRow): a 40dp tonal circle holding a 20dp glyph, the title over a summary, an optional "
           "switch. Rows with more than one line start at the top; a one-line row is centred. Status is Backup's line in "
           "primary while a backup is saved or restored. Leading None is the kit's SetRow, as the licences list draws it. "
           "Put rows in group cells.",
           "ExpressiveSettings.kt", "SetIconRow", vs, setting,
           props=[("Title", "text", "title", None, None), ("Summary", "text", "sum", None, None),
                  ("Icon", "instanceSwap", "icon", None, None), ("Status text", "text", "status", None, None)])

    def set_note(vp):
        c = comp(d, "List row/Setting note", layout="v", w=379, pad=16, sizing=("fixed", "hug"))
        t = txt(d, c, "Song analysis isn't in the file, so a new phone analyses the songs again while it charges. If backup "
                      "is on in the phone's settings, Android also keeps a copy, encrypted with your screen lock.",
                "Body/Medium", "Color/on-surface-variant", "Note text", fill=True)
        return c, {"t": t}

    S.make("List row/Setting note", "Setting note",
           "A paragraph in a settings group, with room under it for progress, a status line and a text button: the "
           "Backup section's last cell.", "Surfaces.kt", "SetNote", [{}], set_note, single=True,
           props=[("Text", "text", "t", None, None)])

    # Group cell --------------------------------------------------------------------------------
    def cell(vp):
        pos, surf = vp["Position"], vp["Surface"]
        top = 20 if pos in ("Single", "Top") else 4
        bottom = 20 if pos in ("Single", "Bottom") else 4
        c = comp(d, "List group/Cell", layout="v", w=379, sizing=("fixed", "hug"), clip=True,
                 fill="Color/surface-container-high" if surf == "Sheet" else "Color/surface-container",
                 cornerRadius=[top, top, bottom, bottom])
        r = inst(d, "List row/Song", c, name="Row", Lead="Cover", State="Default")
        d.set(r, sizingH="fill")
        return c, {"row": r}

    vs = [dict(Position=p, Surface=s) for s in ("Default", "Sheet") for p in ("Single", "Top", "Middle", "Bottom")]
    S.make("List group/Cell", "Group cell",
           "One cell of a list group: clipped to its place in the group (outer corners 20, inner 4) on surface container, "
           "or surface container high inside a sheet. Stack cells 2dp apart, 16dp in from the screen edges; two groups "
           "are 12dp apart. Swap Row for any row.", "Lists.kt", "groupCell", vs, cell,
           props=[("Row", "instanceSwap", "row", None,
                   [d.comp["List row/Song"], d.comp["List row/Collection"], d.comp["List row/Setting"]])])

    # Section header, labels --------------------------------------------------------------------
    def section(vp):
        over, trail = vp["Overline"] == "True", vp["Trailing"] == "True"
        c = comp(d, "Section header", layout="h", w=411, gap=8, pad=[28, 16, 12, 16], cross="max", sizing=("fixed", "hug"))
        col = frame(d, c, "Text column", layout="v", gap=2, sizing=("fill", "hug"))
        lay = {}
        if over:
            lay["over"] = txt(d, col, "Picked at random from your songs", "Body/Medium", "Color/on-surface-variant",
                              "Overline", fill=True)
        lay["title"] = txt(d, col, "Start listening" if over else "Instant mixes", "Title/Large Emphasized",
                           "Color/on-surface", "Title", fill=True)
        if trail:
            # Recap's Top songs control (`.sec .pp{margin:-10px -8px -10px 0}`): the 56dp button lays out as 48 x 36 and
            # draws 10dp above and below that box, so the header keeps the height it has without it.
            box = frame(d, c, "Trailing", w=48, h=36, clip=False)
            inst(d, "Play button", box, name="Play", Size="Large", Playing="False", Ring="False", Tonal="False", x=0, y=-10)
        return c, lay

    S.make("Section header", "Section header",
           "A section's title in the emphasised title style, an optional overline above it and an optional trailing "
           "control, bottom-aligned with the title. 28dp above, 12 below, 16 at the sides (included). Trailing is Recap's "
           "Top songs play control: the 56dp button takes a 48 x 36 box and reaches 10dp above and below it and 8dp "
           "into the end padding, as the code's layout modifier does.",
           "Navigation.kt", "SectionHeader",
           [dict(Overline=o, Trailing=t) for o in ("False", "True") for t in ("False", "True")], section,
           props=[("Title", "text", "title", None, None), ("Overline text", "text", "over", None, None)])

    def list_label(vp):
        c = comp(d, "List label", layout="h", w=411, pad=[20, 32, 8, 32], sizing=("fixed", "hug"))
        t = txt(d, c, "Up next, 9 songs", "Kit/Button", "Color/on-surface-variant", "Label", fill=True)
        return c, {"label": t}

    S.make("List label", "List label", "A small heading directly above a group: \"Up next, 9 songs\", \"Now playing\".",
           "Navigation.kt", "ListLabel", [{}], list_label, single=True, props=[("Label", "text", "label", None, None)])

    def set_section(vp):
        c = comp(d, "Settings section", layout="h", w=411, pad=[24, 32, 8, 32], sizing=("fixed", "hug"))
        t = txt(d, c, "Appearance", "Kit/Button", "Color/primary", "Label", fill=True)
        return c, {"label": t}

    S.make("Settings section", "Settings section", "A settings section heading in primary, 24dp above, 32 in from the edge.",
           "Surfaces.kt", "SettingsSection", [{}], set_section, single=True, props=[("Label", "text", "label", None, None)])

    # Tile --------------------------------------------------------------------------------------
    tiles = {"Album": (148, "Album", "Weather Report", "Mira Okafor", 11), "Folder": (148, "Mosaic", "Late Night", "11 songs", None),
             "Artist": (112, "Artist", "Kavya Raman", None, 8), "Recent": (112, "Album", "Porch Light", "Mira Okafor", 0),
             "Spotlight": (120, "Album", "Weather Report", "2022", 11)}

    def tile(vp):
        typ, now = vp["Kind"], vp["Now playing"] == "True"
        w, kind, title_, sub_, idx = tiles[typ]
        artist = typ == "Artist"
        c = comp(d, "Tile", layout="v", w=w, sizing=("fixed", "hug"), cross="center" if artist else "min")
        cv = inst(d, "Cover", c, name="Art", Kind=kind, Size=str(w))
        if idx is not None and kind != "Mosaic":
            set_cover(d, cv, idx)
        spacer(d, c, "Gap", w=8, h=8)
        tr = frame(d, c, "Title row", layout="h", gap=6, cross="center", sizing=("fill", "hug"))
        if now:
            inst(d, "Wave", tr, name="Wave", Playing="True")
        t = txt(d, tr, title_, "Kit/Tile title", "Color/primary" if now else "Color/on-surface", "Title",
                lines=1, align="center" if artist else None)
        d.set(t, sizingH="fill")
        lay = {"title": t}
        if sub_:
            lay["sub"] = txt(d, c, sub_, "Body/Small", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        return c, lay

    vs = [dict(Kind=t, **{"Now playing": "False"}) for t in ("Album", "Folder", "Artist", "Recent", "Spotlight")]
    vs.insert(4, dict(Kind="Recent", **{"Now playing": "True"}))
    S.make("Tile", "Tile",
           "A cover with its title and subtitle under it, in a shelf or grid: 148dp albums and folders, 112dp artists "
           "(text centred under the cookie) and recently played songs, 120dp in the spotlight. Now playing turns the "
           "title primary with the wave before it. In a grid the tile takes the column's width.",
           "Tiles.kt", "Tile", vs, tile,
           props=[("Title", "text", "title", None, None), ("Subtitle", "text", "sub", None, None)])

    # Favourites pill, hero caption, mix pill -----------------------------------------------------
    def fav_pill(vp):
        h = int(vp["Height"])
        c = comp(d, "Favourites pill", layout="v", w=64, h=h, pad=[18, 0, 16, 0], cross="center", main="spaceBetween",
                 fill="Color/tertiary-container")
        bind_dim(d, c, "cornerRadius", "radius/extra-large-increased")
        d.icon(c, "favorite", size=28, color="Color/on-tertiary-container", filled=True, name="Heart")
        col = frame(d, c, "Count", layout="v", cross="center", sizing=("hug", "hug"))
        n = txt(d, col, "42", "Kit/Favourites count", "Color/on-tertiary-container", "Count")
        u = txt(d, col, "songs", "Label/Small", "Color/on-tertiary-container", "Unit")
        return c, {"n": n, "u": u}

    S.make("Favourites pill", "Favourites pill",
           "Favourites pinned beside Home's carousel: a 64dp pill on tertiary container with the heart and the song count. "
           "248dp beside the carousel and its caption, 200 when there are no other places.",
           "Tiles.kt", "FavouritesPill", [dict(Height="248"), dict(Height="200")], fav_pill,
           props=[("Count", "text", "n", None, None), ("Unit", "text", "u", None, None)])

    def caption(vp):
        c = comp(d, "Hero caption", layout="v", w=200, pad=[10, 4, 0, 4], sizing=("fixed", "hug"))
        t = txt(d, c, "Paper Lanterns", "Kit/Caption title", "Color/on-surface", "Title", fill=True, lines=1)
        s = txt(d, c, "Mira Okafor, 2025", "Body/Small", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        return c, {"t": t, "s": s}

    S.make("Hero caption", "Hero caption",
           "Names the item in the carousel's large slot, under it; it cross-fades when the large item changes. The carousel "
           "itself is Material's multi-browse carousel: items 200dp tall, 8dp apart, 28dp corners.",
           "Tiles.kt", "HeroCaption", [{}], caption, single=True,
           props=[("Title", "text", "t", None, None), ("Subtitle", "text", "s", None, None)])

    def mix(vp):
        now = vp["Now"] == "True"
        container = "Color/secondary-container" if now else "Color/surface-container-high"
        c = comp(d, "Instant mix pill", layout="h", h=56, gap=10, pad=[0, 20, 0, 8], cross="center", fill=container,
                 sizing=("hug", "fixed"))
        bind_dim(d, c, "cornerRadius", "radius/full")
        disc = frame(d, c, "Disc", w=40, h=40, clip=True, fill="Color/surface-container-highest")
        bind_dim(d, disc, "cornerRadius", "radius/full")
        d.image(disc, cover_path(0), name="Picture", w=40, h=40, x=0, y=0)
        d.ellipse(disc, name="Hole", w=8, h=8, x=16, y=16, fill=container)
        t = txt(d, c, "Porch Light", "Kit/Button", "Color/on-surface", "Title")
        return c, {"t": t}

    S.make("Instant mix pill", "Instant mix pill",
           "A song's cover as a disc and its title: a tap starts radio from that song, a long press opens the track menu. "
           "Now is the seed of the radio that is playing. Home lays them in three rows that scroll sideways together, 8dp apart.",
           "Tiles.kt", "InstantMixPill", [dict(Now="False"), dict(Now="True")], mix,
           props=[("Title", "text", "t", None, None)])

    def recap(vp):
        c = comp(d, "Recap entry", layout="v", w=411, pad=[16, 16, 0, 16], sizing=("fixed", "hug"))
        card = frame(d, c, "Card", layout="h", pad=[20, 20, 20, 24], gap=16, cross="center", fill="Color/primary-container",
                     sizing=("fill", "hug"))
        bind_dim(d, card, "cornerRadius", "radius/extra-large")
        col = frame(d, card, "Text column", layout="v", gap=6, sizing=("fill", "hug"))
        t = txt(d, col, "Your 2026 so far", "Kit/Recap card title", "Color/on-primary-container", "Title", fill=True)
        b = txt(d, col, "18,420 minutes. See your top songs and cards to share.", "Body/Medium",
                "Color/on-primary-container", "Body", fill=True)
        inst(d, "Cover", card, name="Mosaic", Kind="Mosaic", Size="72")
        return c, {"t": t, "b": b}

    S.make("Recap entry", "Recap entry",
           "Home's way into Recap, open all year: headline and body on the primary container, a mosaic of top songs' covers "
           "at the end. \"Your 2026 so far\" until the year ends, then \"Your 2026 Recap\". Includes the code's 16dp margins.",
           "Tiles.kt", "RecapCard", [{}], recap, single=True,
           props=[("Title", "text", "t", None, None), ("Body", "text", "b", None, None)])

    def spot(vp):
        c = comp(d, "Spotlight", layout="v", w=411, pad=[28, 16, 0, 16], sizing=("fixed", "hug"))
        box = frame(d, c, "Block", layout="v", pad=[16, 0, 16, 0], fill="Color/surface-container", sizing=("fill", "hug"),
                    clip=True)
        bind_dim(d, box, "cornerRadius", "radius/extra-large")
        head = frame(d, box, "Head", layout="h", pad=[0, 8, 16, 16], gap=12, cross="center", sizing=("fill", "hug"))
        inst(d, "Cover", head, name="Artist", Kind="Artist", Size="56")
        col = frame(d, head, "Text column", layout="v", sizing=("fill", "hug"))
        txt(d, col, "More from", "Label/Medium", "Color/on-surface-variant", "Overline")
        txt(d, col, "Mira Okafor", "Title/Large Emphasized", "Color/on-surface", "Name", fill=True, lines=1)
        inst(d, ib, head, name="Open", Style="Standard", Size="Default", Width="Default", State="Enabled")
        shelf = frame(d, box, "Shelf", layout="h", gap=12, pad=[0, 16, 0, 16], sizing=("fill", "hug"), clip=True)
        for i, (name_, year, idx) in enumerate([("Paper Lanterns", "2025", 0), ("Weather Report", "2022", 11)]):
            t = inst(d, "Tile", shelf, name=f"Album {i + 1}", Kind="Spotlight", Now_playing="False")
            set_cover(d, t, idx)
            props = ctx["components"]["Tile"]["props"]
            d.cmd("instance.setProperties", ids=[t], properties={props["Title"]: name_, props["Subtitle"]: year})
        return c, {}

    S.make("Spotlight", "Spotlight",
           "Home's \"More from\" block: a contained block on surface container with its own head (cookie, overline, name, "
           "arrow) and a shelf of the artist's albums under it.", "Tiles.kt", "Spotlight", [{}], spot, single=True)
    # The arrow in the spotlight head is ArrowForward.
    sp = d.comp["Spotlight"]
    for lid in d.find(sp, name="Open", limit=2):
        d.cmd("instance.swap", ids=[d.find(lid, name="Icon", limit=1)[0]], component=d.comp["Icon/arrow_forward"])

    def browse(vp):
        large = vp["Size"] == "Folder"
        c = comp(d, "Browse tile", layout="h", w=185.5, h=88 if large else 72, gap=8, cross="center",
                 pad=[0, 12 if large else 8, 0, 16], fill="Color/secondary-container")
        bind_dim(d, c, "cornerRadius", "radius/large-increased")
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        t = txt(d, col, "Late Night" if large else "Paper Lanterns", "Kit/Browse title large" if large else "Kit/Browse title",
                "Color/on-secondary-container", "Title", fill=True, lines=1 if large else 2)
        lay = {"t": t}
        if large:
            s = txt(d, col, "11 songs", "Label/Medium", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
            d.set(col, gap=2)
            lay["s"] = s
            cv = inst(d, "Cover", c, name="Picture frame", Kind="Mosaic", Size="64")
            d.set(cv, cornerRadius=16)
        else:
            inst(d, "Cover", c, name="Picture frame", Kind="Album", Size="56")
        return c, lay

    S.make("Browse tile", "Browse tile",
           "Search's browse grid: title and picture on the secondary container of the tile's own cover scheme (each tile "
           "runs FromArt with its cover's seed; this file shows the example seed). Folder is the 88dp tile with an 18sp "
           "bold title, the song count and a 64dp mosaic with 16dp corners.",
           "Tiles.kt", "BrowseTile", [dict(Size="Folder"), dict(Size="Default")], browse, cover=True,
           props=[("Title", "text", "t", None, None), ("Subtitle", "text", "s", None, None)])


# ───────────────────────────────────────────────────────────────── Navigation
def build_navigation(d: Doc, ctx: dict):
    page = DocPage(d, "Components / Navigation", "Navigation",
                   "The navigation bar, app bars, the search field, filter rows, the mini player and the phone's own bars. "
                   "Bars that run under the status bar include its 40dp inset, so a screen stacks them from the top edge "
                   "and lays the status bar instance over them.")
    S = Spec(d, ctx, page)
    ib = d.comp["Icon button"]
    ib_v = dict(Style="Standard", Size="Default", Width="Default", State="Enabled")

    def swap_icon(instance, lig, filled=False):
        icon = d.find(instance, name="Icon", limit=1)[0]
        d.cmd("instance.swap", ids=[icon], component=d.comp[f"Icon/{lig}" + (" (filled)" if filled else "")])

    # Navigation item and bar ---------------------------------------------------------------------
    dests = {"Home": "home", "Search": "search", "Library": "library_music"}

    def nav_item(vp):
        dest, sel = vp["Destination"], vp["Selected"] == "True"
        c = comp(d, "Navigation item", layout="v", w=137, h=64, gap=4, pad=[6, 0, 6, 0], cross="center")
        ind = frame(d, c, "Indicator", layout="h", w=56, h=32, cross="center", main="center",
                    fill="Color/secondary-container" if sel else None)
        bind_dim(d, ind, "cornerRadius", "radius/full")
        d.icon(ind, dests[dest], color="Color/on-secondary-container" if sel else "Color/on-surface-variant",
               filled=sel, name="Icon")
        lab = txt(d, c, dest, "Kit/Nav label selected" if sel else "Label/Medium",
                  "Color/on-surface" if sel else "Color/on-surface-variant", "Label")
        return c, {"label": lab}

    S.make("Navigation item", "Navigation item",
           "One destination of the navigation bar (Material's ShortNavigationBarItem): a 56 x 32 pill indicator in "
           "secondary container behind the icon, the icon filled and the label bold while selected.",
           "Navigation.kt", "ExpressiveNavigationBar",
           [dict(Destination=dd, Selected=s) for dd in dests for s in ("True", "False")], nav_item)

    def nav_bar(vp):
        c = comp(d, "Navigation bar", layout="h", w=411, h=88, pad=[0, 0, 24, 0], fill="Color/surface-container")
        for dest in dests:
            i = inst(d, "Navigation item", c, name=f"Item {dest}", Destination=dest, Selected=str(dest == vp["Selected"]))
            d.set(i, sizingH="fill")
        return c, {}

    S.make("Navigation bar", "Navigation bar",
           "Material's short navigation bar on surface container: Home, Search, Library. 64dp, plus the 24dp gesture area it "
           "pads for itself (88 in all); the gesture bar draws over that area. The mini player floats 8dp above it.",
           "Navigation.kt", "ExpressiveNavigationBar", [dict(Selected=s) for s in dests], nav_bar)

    # Top app bars --------------------------------------------------------------------------------
    def root_bar(vp):
        home, scrolled = vp["Kind"] == "Home", vp["Scrolled"] == "True"
        c = comp(d, "Top app bar/Root", layout="v", w=411, sizing=("fixed", "hug"),
                 fill="Color/surface-container" if scrolled else None)
        spacer(d, c, "Status bar inset", w=411, h=40)
        bar = frame(d, c, "Bar", layout="h", w=411, h=64 if scrolled else 72, pad=[0, 8, 0, 16], cross="center")
        tg = frame(d, bar, "Title group", layout="h", gap=10, cross="center", sizing=("fill", "hug"))
        if home:
            mh = 24 if scrolled else 32
            mark = frame(d, tg, "Mark", w=round(mh * 36 / 51.5, 2), h=mh)
            d.vector(mark, name="Headroom", path=MARK_PATH, fill="Color/primary", x=0, y=0, w=round(mh * 36 / 51.5, 2), h=mh,
                     constraints={"horizontal": "scale", "vertical": "scale"})
        t = txt(d, tg, "Attic" if home else "Library", "Title/Large Emphasized" if scrolled else "Kit/Root title",
                "Color/on-surface", "Title", fill=True, lines=1)
        acts = frame(d, bar, "Actions", layout="h", sizing=("hug", "hug"), cross="center")
        lay = {"title": t}
        if not home:
            s = inst(d, ib, acts, name="Search", **ib_v)
            swap_icon(s, "search")
            g = inst(d, "Toggle button", acts, name="Grid toggle", Checked="False", Size="Default", Off_style="Standard")
            swap_icon(g, "grid_view")
            lay.update(search=s, grid=g)
        st = inst(d, ib, acts, name="Settings", **ib_v)
        swap_icon(st, "settings")
        return c, lay

    S.make("Top app bar/Root", "Root app bar",
           "A tab root's bar: one row, a large rounded title (36/44) and the actions, settings last. As the list scrolls the "
           "title shrinks to 22/28, Home's mark from 32 to 24dp, the row from 72 to 64dp, and the bar takes surface container "
           "under the status bar. Home carries the mark; Library adds search and the grid toggle; Search keeps settings only.",
           "Navigation.kt", "RootAppBar",
           [dict(Kind=t, Scrolled=s) for t in ("Home", "Plain") for s in ("False", "True")], root_bar,
           props=[("Title", "text", "title", None, None), ("Show search", "boolean", "search", True, None),
                  ("Show grid toggle", "boolean", "grid", True, None)])

    def detail_bar(vp):
        surf = vp["Surface"]
        over = surf == "Over art"
        c = comp(d, "Top app bar/Detail", layout="v", w=411, sizing=("fixed", "hug"),
                 fill="Color/surface-container" if surf == "Scrolled" else None)
        spacer(d, c, "Status bar inset", w=411, h=40)
        bar = frame(d, c, "Bar", layout="h", w=411, h=64, pad=[0, 4, 0, 4], cross="center")

        def button(parent, name, lig):
            holder = parent
            if over:
                holder = frame(d, parent, f"{name} scrim", layout="h", fill="Color/state/over-art-scrim", sizing=("hug", "hug"))
                bind_dim(d, holder, "cornerRadius", "radius/full")
            b = inst(d, ib, holder, name=name, **ib_v)
            swap_icon(b, lig)
            if over:
                d.override(b, "Glyph", fill="Color/on-surface")
            return b, holder

        button(bar, "Back", "arrow_back")
        tbox = frame(d, bar, "Title box", layout="h", pad=[0, 4, 0, 4], sizing=("fill", "hug"))
        t = txt(d, tbox, "Settings", "Kit/Bar title", "Color/on-surface", "Title", fill=True, lines=1)
        if over:
            d.set(t, visible=False)
        a, holder = button(bar, "Action", "more_vert")
        # Show action hides the action alone (its scrim over art), never the bar that holds Back.
        return c, {"title": t, "action": a, "show": holder if over else a}

    S.make("Top app bar/Detail", "Detail app bar",
           "Back, a 22/28 title and actions. Scrolled takes surface container under the status bar. Over art leaves it "
           "transparent with scrims behind the buttons (surface at 62 %) and hides the title until the header has scrolled "
           "away. Settings and licences use Plain, then Scrolled once their big title has gone.",
           "Navigation.kt", "DetailAppBar", [dict(Surface=s) for s in ("Plain", "Scrolled", "Over art")], detail_bar,
           props=[("Title", "text", "title", None, None), ("Show action", "boolean", "show", True, None),
                  ("Action", "instanceSwap", "action", None, None)])

    def coll_bar(vp):
        col = vp["Collapsed"] == "True"
        c = comp(d, "Top app bar/Collection", layout="v", w=411, sizing=("fixed", "hug"),
                 fill="Color/surface-container" if col else None)
        spacer(d, c, "Status bar inset", w=411, h=40)
        bar = frame(d, c, "Bar", layout="h", w=411, h=64, gap=4, pad=[0, 4, 0, 4], cross="center")
        b = inst(d, ib, bar, name="Back", **ib_v)
        swap_icon(b, "arrow_back")
        tbox = frame(d, bar, "Title box", layout="h", pad=[0, 4, 0, 4], sizing=("fill", "hug"))
        t = txt(d, tbox, "Paper Lanterns", "Kit/Bar title", "Color/on-surface", "Title", fill=True, lines=1)
        if not col:
            d.set(t, visible=False)
        if col:
            inst(d, "Play button", bar, name="Play", Size="Small", Playing="False", Ring="False", Tonal="False")
        m = inst(d, ib, bar, name="Options", **ib_v)
        return c, {"title": t}

    S.make("Top app bar/Collection", "Collection bar",
           "The bar over an album, artist or playlist header, in the header's cover scheme. Over the header it holds back "
           "and the overflow; once the title has scrolled under it, it takes surface container, the title (narrowing before "
           "it truncates) and the small play control, which scales in from 60 %.",
           "ExpressiveCollections.kt", "CollectionBar", [dict(Collapsed="False"), dict(Collapsed="True")], coll_bar,
           cover=True, props=[("Title", "text", "title", None, None)])

    # Search field --------------------------------------------------------------------------------
    def search(vp):
        state = vp["State"]
        c = comp(d, "Search field", layout="h", w=379, h=56, cross="center",
                 pad=[0, 4, 0, 4 if state == "Docked" else 16], fill="Color/surface-container-high")
        bind_dim(d, c, "cornerRadius", "radius/full")
        if state == "Docked":
            b = inst(d, ib, c, name="Back", **ib_v)
            swap_icon(b, "arrow_back")
            d.override(b, "Glyph", fill="Color/on-surface")
            spacer(d, c, "Gap", w=4, h=4)
        else:
            d.icon(c, "search", color="Color/on-surface", name="Search icon")
            spacer(d, c, "Gap", w=12, h=12)
        empty = state == "Empty"
        t = txt(d, c, "Search your music" if empty else "ka", "Body/Large",
                "Color/on-surface-variant" if empty else "Color/on-surface", "Query", fill=True, lines=1)
        if empty:
            spacer(d, c, "End", w=12, h=12)
        else:
            x = inst(d, ib, c, name="Clear", **ib_v)
            swap_icon(x, "close")
        return c, {"t": t}

    S.make("Search field", "Search field",
           "A 56dp pill on surface container high: the search glyph, the query (placeholder \"Search your music\") and a "
           "clear button once there is text. Docked is the search view's bar: back leads in place of the glyph.",
           "Navigation.kt", "SearchField", [dict(State=s) for s in ("Empty", "Query", "Docked")], search,
           props=[("Text", "text", "t", None, None)])

    # Chip row ------------------------------------------------------------------------------------
    chip_set = d.comp["Filter chip"]
    chip_label = ctx["components"]["Filter chip"]["props"]["Label"]

    def chip_row(vp):
        c = comp(d, "Chip row", layout="h", w=411, gap=8, pad=[0, 16, 0, 16], sizing=("fixed", "hug"), clip=True)
        for lab in ("Playlists", "Albums", "Artists", "Songs"):
            ch = inst(d, chip_set, c, name=f"Chip {lab}", Selected=str(lab == vp["Selected"]))
            set_text(d, ch, chip_label, lab)
        return c, {}

    S.make("Chip row", "Chip row",
           "Library's filters: chips 8dp apart with 16dp side padding, scrolling sideways. At most one is on; tapping it "
           "again clears it.", "Navigation.kt", "ChipRow",
           [dict(Selected=s) for s in ("None", "Playlists", "Albums", "Artists", "Songs")], chip_row)

    # First run: step pips and its bar ---------------------------------------------------------
    def pips(vp):
        pos = int(vp["Step"])
        c = comp(d, "Step pips", layout="h", gap=6, cross="center", sizing=("hug", "hug"))
        for i in range(1, 5):
            p_ = d.rect(c, name=f"Pip {i}", w=24 if i == pos else 8, h=8,
                        fill="Color/primary" if i <= pos else "Color/surface-container-highest")
            bind_dim(d, p_, "cornerRadius", "radius/full")
        return c, {}

    S.make("Step pips", "Step pips",
           "Where the first run is: one 8dp pip per step, 6dp apart; steps done and the current one in primary, the "
           "current one a 24dp pill. Width on fastSpatial, colour on defaultEffects. Read out as \"Step 2 of 4\".",
           "ExpressiveOnboarding.kt", "StepPips", [dict(Step=str(i)) for i in range(1, 5)], pips)

    def ob_bar(vp):
        step = int(vp["Step"])
        c = comp(d, "Top app bar/First run", layout="h", w=411, h=64, pad=[0, 4, 0, 4], cross="center")
        if step > 1:
            b = inst(d, ib, c, name="Back", **ib_v)
            swap_icon(b, "arrow_back")
        else:
            spacer(d, c, "No back", w=48, h=48)
        spacer(d, c, "Space", h=1, fill_h=True)
        pbox = frame(d, c, "Pips box", layout="h", pad=[0, 16, 0, 0], sizing=("hug", "hug"))
        inst(d, "Step pips", pbox, name="Step pips", Step=str(step))
        return c, {}

    S.make("Top app bar/First run", "First run bar",
           "The first run's bar under the status bar: back (none on the first step) and the step pips 16dp from the edge.",
           "ExpressiveOnboarding.kt", "ExpressiveOnboarding", [dict(Step=str(i)) for i in range(1, 5)], ob_bar)

    # Mini player ---------------------------------------------------------------------------------
    def mini(vp):
        playing, long = vp["State"] == "Playing", vp["Title length"] == "Long"
        c = comp(d, "Mini player", layout="h", w=387, h=64, pad=[0, 6, 0, 8], cross="center", fill="Color/surface-container-high",
                 clip=True)
        bind_dim(d, c, "cornerRadius", "radius/full")
        cv = inst(d, "Cover", c, name="Cover", Kind="Album", Size="48")
        bind_dim(d, cv, "cornerRadius", "radius/large")
        col = frame(d, c, "Text column", layout="v", pad=[0, 0, 0, 12], sizing=("fill", "hug"))
        t = txt(d, col, "Small Hours, Big Sky and Everything After It" if long else "Lights Over the Water", "Kit/Button",
                "Color/on-surface", "Title", fill=True, lines=1)
        a = txt(d, col, "Mira Okafor", "Body/Small", "Color/on-surface-variant", "Artist", fill=True, lines=1)
        h = inst(d, "Heart button", c, name="Heart", Favourite="True")
        p = inst(d, "Play button", c, name="Play", Size="Small", Playing=str(playing), Ring="True", Tonal="False")
        return c, {"t": t, "a": a, "heart": h, "play": p}

    S.make("Mini player", "Mini player",
           "A 64dp pill on the cover scheme's surface container high, 12dp in from the screen edges and 8dp above the "
           "navigation bar: the cover (16dp corners), title over artist, the heart and the play control with its progress "
           "ring. Tap opens Now Playing; swiping the title sideways past 28 % skips. A long title ellipsises; a long artist "
           "line scrolls after 2.5 s.", "ExpressiveShell.kt", "ExpressiveMiniPlayer",
           [dict(State=s, **{"Title length": t}) for t in ("Default", "Long") for s in ("Playing", "Paused")], mini, cover=True,
           props=[("Title", "text", "t", None, None), ("Artist", "text", "a", None, None)], expose=("heart",))

    # System bars ---------------------------------------------------------------------------------
    def status(vp):
        c = comp(d, "Status bar", layout="h", w=411, h=40, pad=[0, 26, 0, 26], cross="center")
        txt(d, c, "10:24", "Kit/Status bar time", "System/status-bar-ink", "Time")
        spacer(d, c, "Space", h=1, fill_h=True)
        g = frame(d, c, "System icons", w=62, h=14)
        ink = "System/status-bar-ink"
        for i in range(4):
            bh = 14 * (0.35 + 0.65 * i / 3)
            d.rect(g, name=f"Signal bar {i + 1}", w=2.8, h=round(bh, 2), x=i * 4, y=round(14 - bh, 2), radius=1, fill=ink)
        r = 14 * 1.15
        x0, y0 = 25 + r * math.cos(math.radians(225)), 14 + r * math.sin(math.radians(225))
        x1, y1 = 25 + r * math.cos(math.radians(315)), 14 + r * math.sin(math.radians(315))
        d.vector(g, name="Wifi", path=f"M25 14 L{x0:.2f} {y0:.2f} A{r:.2f} {r:.2f} 0 0 1 {x1:.2f} {y1:.2f} Z", fill=ink, x=0, y=0)
        bh = 14 * 0.72
        top = (14 - bh) / 2
        o = d.rect(g, name="Battery outline", w=19, h=round(bh, 2), x=40, y=round(top, 2), radius=2.5, fill="none")
        d.set(o, stroke=V(ink), strokeWeight=1.2, strokeAlign="center")
        d.rect(g, name="Battery level", w=12, h=round(bh - 4, 2), x=42, y=round(top + 2, 2), radius=1, fill=ink)
        d.rect(g, name="Battery nub", w=1.6, h=4, x=60, y=5, radius=1, fill=ink)
        return c, {}

    S.make("Status bar", "Status bar",
           "The phone's status bar as the JVM render harness draws it: the clock at 10:24, signal, wifi and battery, in ink "
           "that follows the theme (System collection). It sits over the top 40dp of every screen.",
           "../../../storeShots/kotlin/app/attic/player/storeshots/ShotHarness.kt", "SystemBars", [{}], status, single=True)

    def gesture(vp):
        c = comp(d, "Gesture bar", w=411, h=24)
        hnd = d.rect(c, name="Handle", w=108, h=4, x=151.5, y=10, fill="System/gesture-handle", radius=2,
                     constraints={"horizontal": "center", "vertical": "bottom"})
        return c, {}

    S.make("Gesture bar", "Gesture bar",
           "The gesture handle: 108 x 4, 10dp above the bottom edge, the status bar ink at 85 %. It sits over the bottom "
           "24dp of every screen, over the navigation bar's own gesture area.",
           "../../../storeShots/kotlin/app/attic/player/storeshots/ShotHarness.kt", "SystemBars", [{}], gesture, single=True)


# ───────────────────────────────────────────────────────────────── Playback
def build_wave(d: Doc, ctx: dict):
    """NowPlayingWave, built before the rows that use it; its spec card goes on the Playback page later."""
    if "Wave" in d.comp:
        return

    def wave(vp):
        playing = vp["Playing"] == "True"
        c = comp(d, "Wave", w=20, h=10)
        stroke_vec(d, c, "Wave line", wave_path(1.2, 18.8, 5, 10, 2 if playing else 0), "Color/primary", 2.4)
        return c, {}

    made = []
    for vp in (dict(Playing="True"), dict(Playing="False")):
        cid, _ = wave(vp)
        d.set(cid, name=vname(vp))
        made.append(cid)
    sid = d.combine("Wave", made, description="The \"now playing\" mark: a short wave 20 x 10dp that travels while playing "
                                              "and lies flat when paused. It sits over the current row's cover or in place "
                                              f"of its number, and before a tile's title. Compose: {SRC}PlayControls.kt · NowPlayingWave.")
    d.set(sid, fill=V("Color/surface"))
    d.set(made, description="The \"now playing\" mark: a short wave 20 x 10dp that travels while playing and lies flat "
                            "when paused. It sits over the current row's cover or in place of its number, and before a "
                            f"tile's title. Compose: {SRC}PlayControls.kt · NowPlayingWave.")
    d.cmd("codeConnect.set", id=sid, language="compose", importPath=PKG, name="NowPlayingWave")
    ctx.setdefault("components", {})["Wave"] = {"id": sid, "variants": dict(zip(["Playing=True", "Playing=False"], made)),
                                                "props": {}, "source": f"{SRC}PlayControls.kt · NowPlayingWave"}
    ctx["wave_pending"] = sid


def build_playback(d: Doc, ctx: dict):
    page = DocPage(d, "Components / Playback", "Playback",
                   "The player's parts. Playing is a wavy line and a squared-off control; paused is a flat line and a "
                   "round one. Everything here sits inside FromArt in code, so these components carry Color mode Cover.")
    S = Spec(d, ctx, page)
    ib = d.comp["Icon button"]
    ib_v = dict(Style="Standard", Size="Default", Width="Default", State="Enabled")

    def swap_icon(instance, lig, filled=False):
        icon = d.find(instance, name="Icon", limit=1)[0]
        d.cmd("instance.swap", ids=[icon], component=d.comp[f"Icon/{lig}" + (" (filled)" if filled else "")])

    # Wave: the set exists already (rows use it); give it its card here.
    sid = ctx.pop("wave_pending", None)
    if sid:
        card = page.card("Wave", "A short wave, 20 x 10dp, stroke 2.4: one wavelength of 10dp, 2dp high, moving one "
                                 "wavelength a second while playing; flat when paused.",
                         f"Compose: {SRC}PlayControls.kt · NowPlayingWave")
        d.cmd("node.reparent", ids=[sid], parent=card)
        pv = hrow(d, card, "Dark preview", gap=24, pad=24, fill="Color/surface", radius=12)
        for m in d.mode_collections:
            d.set_mode(pv, m, "Dark")
        for cid in ctx["components"]["Wave"]["variants"].values():
            d.instance(cid, pv)
        page.done(card)

    W = 363

    def seek(vp):
        playing = vp["Playing"] == "True"
        c = comp(d, "Seek bar", layout="h", w=W, h=44, gap=4, cross="center")
        act = frame(d, c, "Active track", w=round(seek_active(PROGRESS, W), 2), h=44, clip=True)
        stroke_vec(d, act, "Wave", wave_path(2, W, 22, 32, 3.5 if playing else 0), "Color/primary", 4)
        h = d.rect(c, name="Handle", w=4, h=28, radius=2, fill="Color/primary")
        r = d.rect(c, name="Rest of track", w=W, h=4, fill="Color/outline-variant")
        bind_dim(d, r, "cornerRadius", "radius/full")
        d.set(r, sizingH="fill")
        return c, {}

    S.make("Seek bar", "Seek bar",
           "The seek bar: a wavy active track while playing (wavelength 32dp, 3.5dp high, travelling), flat when paused or "
           "while dragging; a 4 x 28dp bar handle with 6dp clear either side; the rest of the track in outline variant. Tap "
           "to jump, drag to scrub. 44dp tall; shown at 1:18 of 3:52. The Active track frame's width sets the position "
           "(seek_active() in components_core.py: 4 + fraction x (width - 20)); the handle and the rest follow it.", "PlayControls.kt", "SeekBar",
           [dict(Playing="True"), dict(Playing="False")], seek, cover=True)

    def times(vp):
        c = comp(d, "Seek times", layout="h", w=W, main="spaceBetween", sizing=("fixed", "hug"))
        e = txt(d, c, "1:18", "Kit/Figures", "Color/on-surface-variant", "Elapsed")
        t = txt(d, c, "3:52", "Kit/Figures", "Color/on-surface-variant", "Total")
        return c, {"e": e, "t": t}

    S.make("Seek times", "Seek times", "Elapsed and total time under the seek bar, in tabular figures. The elapsed time "
                                       "follows a drag.", "PlayControls.kt", "SeekTimes", [{}], times, single=True, cover=True,
           props=[("Elapsed", "text", "e", None, None), ("Total", "text", "t", None, None)])

    def transport(vp):
        playing = vp["Playing"]
        c = comp(d, "Transport", layout="h", w=W, h=80, gap=8, cross="center", main="center")
        inst(d, "Skip button", c, name="Previous", Direction="Previous")
        inst(d, "Play button", c, name="Play", Size="Extra large", Playing=playing, Ring="False", Tonal="False")
        inst(d, "Skip button", c, name="Next", Direction="Next")
        return c, {}

    S.make("Transport", "Transport", "Now Playing's transport: previous, the wide play control, next, 8dp apart and centred.",
           "PlayControls.kt", "Transport", [dict(Playing="True"), dict(Playing="False")], transport, cover=True)

    def tools(vp):
        sh, rp = vp["Shuffle"] == "On", vp["Repeat"]
        c = comp(d, "Now playing tools", layout="h", w=W, h=56, pad=[0, 12, 0, 12], cross="center", main="spaceBetween",
                 fill="Color/surface-container")
        bind_dim(d, c, "cornerRadius", "radius/full")
        s = inst(d, "Toggle button", c, name="Shuffle", Checked=str(sh), Size="Default", Off_style="Standard")
        swap_icon(s, "shuffle", filled=sh)
        r = inst(d, "Toggle button", c, name="Repeat", Checked=str(rp != "Off"), Size="Default", Off_style="Standard")
        swap_icon(r, "repeat_one" if rp == "One" else "repeat", filled=rp != "Off")
        h = inst(d, "Heart button", c, name="Heart", Favourite="True")
        q = inst(d, ib, c, name="Queue", **ib_v)
        swap_icon(q, "queue_music")
        return c, {"heart": h}

    S.make("Now playing tools", "Now Playing tools",
           "Shuffle, repeat, favourite and the queue on one 56dp pill on surface container. On toggles square off and fill; "
           "repeat cycles off, all, one (repeat_one's glyph).", "ExpressiveNowPlaying.kt", "Tools",
           [dict(Shuffle=s, Repeat=r) for s in ("Off", "On") for r in ("Off", "All", "One")], tools, cover=True,
           expose=("heart",))

    def np_header(vp):
        c = comp(d, "Now playing header", layout="h", w=411, h=64, gap=4, pad=[0, 8, 0, 8], cross="center")
        b = inst(d, ib, c, name="Close", **ib_v)
        swap_icon(b, "keyboard_arrow_down")
        col = frame(d, c, "Context", layout="v", sizing=("fill", "hug"))
        txt(d, col, "Playing from", "Label/Medium", "Color/on-surface-variant", "Overline")
        row = frame(d, col, "Context row", layout="h", gap=6, cross="center", sizing=("fill", "hug"))
        ic = d.icon(row, "album", size=18, color="Color/primary", name="Context icon")
        t = txt(d, row, "Paper Lanterns", "Kit/Context", "Color/on-surface", "Context name", fill=True, lines=1)
        inst(d, ib, c, name="More", **ib_v)
        return c, {"t": t, "ic": ic}

    S.make("Now playing header", "Now Playing header",
           "Close, \"Playing from\" over the context with a glyph for its kind (album, person, folder, playlist_play, "
           "favorite, sensors for radio, none otherwise), and the track menu.", "ExpressiveNowPlaying.kt", "Header",
           [{}], np_header, single=True, cover=True,
           props=[("Context", "text", "t", None, None), ("Context icon", "instanceSwap", "ic", None, None),
                  ("Show context icon", "boolean", "ic", True, None)])

    def compact(vp):
        c = comp(d, "Compact header", layout="v", w=411, sizing=("fixed", "hug"), fill="Color/surface-container-high",
                 cornerRadius=[0, 0, 28, 28])
        spacer(d, c, "Status bar inset", w=411, h=40)
        row = frame(d, c, "Bar", layout="h", w=411, h=72, gap=6, pad=[0, 10, 0, 24], cross="center")
        col = frame(d, row, "Text column", layout="v", sizing=("fill", "hug"))
        t = txt(d, col, "Lights Over the Water", "Kit/Compact title", "Color/on-surface", "Title", fill=True, lines=1)
        a = txt(d, col, "Mira Okafor", "Body/Small", "Color/on-surface-variant", "Artist", fill=True, lines=1)
        inst(d, "Heart button", row, name="Heart", Favourite="True")
        inst(d, "Play button", row, name="Play", Size="Small", Playing=vp["Playing"], Ring="True", Tonal="False")
        return c, {"t": t, "a": a}

    S.make("Compact header", "Compact header",
           "Takes over once the title has scrolled away: title, artist, favourite and the small play control with its ring, "
           "on surface container high with 28dp bottom corners, over the status bar. It slides down from the top.",
           "ExpressiveNowPlaying.kt", "CompactHeader", [dict(Playing="True"), dict(Playing="False")], compact, cover=True,
           props=[("Title", "text", "t", None, None), ("Artist", "text", "a", None, None)])

    # Lyrics -------------------------------------------------------------------------------------
    line_colour = {"Sung": "Color/on-surface-variant", "Current": "Color/on-surface", "Upcoming": "Color/outline",
                   "Plain": "Color/on-surface"}
    line_text = {"Sung": "the moths are writing letters on the glass", "Current": "carry what we couldn't say",
                 "Upcoming": "let them drift until the morning",
                 "Plain": "I left the porch light on for you, the moths are writing letters on the glass"}

    def lyric_line(vp):
        large = vp["Size"] == "Large"
        c = comp(d, "Lyrics/Line", layout="v", w=363 if large else 339, pad=[0, 0, 16 if large else 10, 0],
                 sizing=("fixed", "hug"))
        t = txt(d, c, line_text[vp["State"]], "Kit/Lyric large" if large else "Title/Large Emphasized",
                line_colour[vp["State"]], "Line text", fill=True, lines=2 if not large else None)
        return c, {"t": t}

    S.make("Lyrics/Line", "Lyric line",
           "A lyric line in the Now Playing card: 22/28 emphasised, two lines at most there (four for plain lyrics), 10dp "
           "below. Sung lines are on-surface-variant, the current one on-surface, those to come outline; plain (unsynced) "
           "text is on-surface. Large is 28/36 with 16 below.", "Surfaces.kt", "LyricLine",
           [dict(State=s, Size=z) for z in ("Card", "Large") for s in ("Current", "Upcoming", "Sung", "Plain")], lyric_line,
           cover=True, props=[("Text", "text", "t", None, None)])

    def screen_line(vp):
        st = vp["State"]
        cur = st == "Current"
        c = comp(d, "Lyrics/Screen line", layout="v", w=363, pad=[4 if cur else 0, 0, 20 if cur else 14, 0],
                 sizing=("fixed", "hug"))
        t = txt(d, c, line_text[st], "Kit/Lyric current" if cur else "Kit/Lyric line",
                {"Current": "Color/primary", "Sung": "Color/outline", "Upcoming": "Color/on-surface-variant"}[st], "Line text",
                fill=True)
        return c, {"t": t}

    S.make("Lyrics/Screen line", "Lyric line, full screen",
           "One timed line on the lyrics screen: 24/32 medium in on-surface-variant to come, outline once sung; the line "
           "being sung 34/40, emphasised, in primary, with 4dp more above and 20 below. Size, weight and spacing move on the "
           "default spatial spring, colour on default effects.", "ExpressiveLyrics.kt", "SyncedLine",
           [dict(State=s) for s in ("Current", "Sung", "Upcoming")], screen_line, cover=True,
           props=[("Text", "text", "t", None, None)])

    line_set = d.comp["Lyrics/Line"]
    line_prop = ctx["components"]["Lyrics/Line"]["props"]["Text"]
    lyric_lines = ["carry what we couldn't say", "let them drift until the morning", "let them burn the long way",
                   "I counted boats along the harbour"]

    def info_card(parent_comp_name, title=None, symbol=None):
        c = comp(d, parent_comp_name, layout="v", w=379, pad=20, fill="Color/surface-container", sizing=("fixed", "hug"))
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        if title:
            head = frame(d, c, "Heading", layout="h", gap=8, pad=[0, 0, 14, 0], cross="center", sizing=("hug", "hug"))
            if symbol:
                d.icon(head, symbol, size=20, color="Color/on-surface-variant", name="Heading icon")
            txt(d, head, title, "Kit/Button", "Color/on-surface-variant", "Heading")
        return c

    def lyrics_card(vp):
        st = vp["State"]
        c = info_card("Lyrics/Card", "Lyrics", "lyrics")
        lay = {}
        if st == "Synced":
            for i, line in enumerate(lyric_lines):
                li = inst(d, line_set, c, name=f"Lyric {i + 1}", State="Current" if i == 0 else "Upcoming", Size="Card")
                d.set(li, sizingH="fill")
                set_text(d, li, line_prop, line)
        elif st == "Plain":
            li = inst(d, line_set, c, name="Plain lyrics", State="Plain", Size="Card")
            d.set(li, sizingH="fill")
            set_text(d, li, line_prop, line_text["Plain"])
        else:
            msg = {"Instrumental": "This song is instrumental.", "Not downloaded": "Download once and they stay on this phone.",
                   "Downloading": "Download once and they stay on this phone.",
                   "Not found": "No lyrics were found for this song."}[st]
            txt(d, c, msg, "Body/Medium", "Color/on-surface-variant", "Note", fill=True)
        if st in ("Synced", "Plain"):
            holder = frame(d, c, "Open lyrics", layout="v", pad=[6 if st == "Synced" else 16, 0, 0, 0], sizing=("hug", "hug"))
            b = inst(d, "Button", holder, name="Open lyrics button", Style="Tonal", Size="Small", Icon="Trailing",
                     State="Enabled", Squared="False")
            set_text(d, b, ctx["components"]["Button"]["props"]["Label"], "Open lyrics")
        elif st in ("Not downloaded", "Not found"):
            holder = frame(d, c, "Download", layout="v", pad=[16, 0, 0, 0], sizing=("hug", "hug"))
            b = inst(d, "Button", holder, name="Download button", Style="Filled" if st == "Not downloaded" else "Outlined",
                     Size="Small", Icon="Leading", State="Enabled", Squared="False")
            bp = ctx["components"]["Button"]["props"]
            d.cmd("instance.setProperties", ids=[b], properties={bp["Label"]: "Download lyrics"})
            lead = d.find(b, name="Leading icon", limit=1)[0]
            d.cmd("instance.swap", ids=[lead], component=d.comp["Icon/download" if st == "Not downloaded" else "Icon/refresh"])
        elif st == "Downloading":
            holder = frame(d, c, "Loading", w=48, h=48)
            sh = d.instance("Shape/Burst", holder, name="Loading indicator")
            d.set(sh, x=5, y=5, width=38, height=38)
        return c, lay

    S.make("Lyrics/Card", "Lyrics card",
           "The lyrics card under the player. Its container and \"Lyrics\" label stay put; the body is four lines from the "
           "one being sung (two lines each at most), the plain text, a note, or the download action. \"Open lyrics\" shows "
           "only when there is text. Downloading shows the loading indicator (drawn mid-morph as the burst). A retry after "
           "nothing was found is outlined; the first download is the page's one filled button.",
           "ExpressiveNowPlaying.kt", "LyricsCard",
           [dict(State=s) for s in ("Synced", "Plain", "Instrumental", "Not downloaded", "Not found", "Downloading")],
           lyrics_card, cover=True)

    def keyvalue(vp):
        c = comp(d, "Key value", layout="v", pad=[8, 0, 8, 0], w=339, sizing=("fixed", "hug"))
        l = txt(d, c, "Performed by", "Label/Medium", "Color/on-surface-variant", "Label", fill=True)
        v = txt(d, c, "Mira Okafor", "Body/Large", "Color/on-surface", "Value", fill=True)
        return c, {"l": l, "v": v}

    S.make("Key value", "Key value", "A label over its value, as in Credits.", "Surfaces.kt", "KeyValue", [{}], keyvalue,
           single=True, props=[("Label", "text", "l", None, None), ("Value", "text", "v", None, None)])
    kv_props = ctx["components"]["Key value"]["props"]

    def credits(vp):
        c = info_card("Info card/Credits", "Credits")
        lay = {}
        for lab, val in (("Performed by", "Mira Okafor"), ("Composer", "Mira Okafor"), ("Album", "Paper Lanterns"),
                         ("Year", "2025"), ("Genre", "Folk")):
            k = d.instance("Key value", c, name=lab)
            d.set(k, sizingH="fill")
            d.cmd("instance.setProperties", ids=[k], properties={kv_props["Label"]: lab, kv_props["Value"]: val})
            lay[lab] = k
        return c, lay

    S.make("Info card/Credits", "Credits card",
           "A card under the player (.np-card): surface container, 28dp corners, 20dp padding and a small heading. Credits "
           "lists performed by, then the composer, album, year and genre when the tags have them. The demo library has no "
           "composer or genre tags, so the renders show neither; turn them on with Show composer and Show genre.",
           "ExpressiveNowPlaying.kt", "CreditsCard", [{}], credits, single=True, cover=True,
           props=[("Show composer", "boolean", "Composer", True, None), ("Show genre", "boolean", "Genre", True, None)])

    def artist_card(vp):
        c = comp(d, "Info card/Artist", layout="h", w=379, gap=16, pad=20, cross="center", fill="Color/surface-container",
                 sizing=("fixed", "hug"))
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        a = inst(d, "Cover", c, name="Artist", Kind="Artist", Size="88")
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        txt(d, col, "About the artist", "Label/Medium", "Color/on-surface-variant", "Overline")
        nbox = frame(d, col, "Name box", layout="v", pad=[2, 0, 0, 0], sizing=("fill", "hug"))
        n = txt(d, nbox, "Mira Okafor", "Title/Large Emphasized", "Color/on-surface", "Name", fill=True)
        txt(d, col, "14 songs in your library", "Body/Medium", "Color/on-surface-variant", "Count", fill=True)
        d.icon(c, "arrow_forward", color="Color/on-surface", name="Open")
        return c, {"n": n}

    S.make("Info card/Artist", "About the artist",
           "The artist in the cookie beside their name and song count; opens the artist.", "ExpressiveNowPlaying.kt",
           "ArtistCard", [{}], artist_card, single=True, cover=True, props=[("Name", "text", "n", None, None)])

    def queue_bar(vp):
        c = comp(d, "Queue bar", layout="h", w=411, h=64, gap=4, pad=[0, 8, 0, 8], cross="center")
        b = inst(d, ib, c, name="Close", **ib_v)
        swap_icon(b, "keyboard_arrow_down")
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        txt(d, col, "Queue", "Title/Large Emphasized", "Color/on-surface", "Title")
        row = frame(d, col, "Context row", layout="h", gap=6, cross="center", sizing=("fill", "hug"))
        d.icon(row, "album", size=16, color="Color/primary", name="Context icon")
        t = txt(d, row, "Paper Lanterns", "Label/Medium", "Color/on-surface-variant", "Context name", fill=True, lines=1)
        return c, {"t": t}

    S.make("Queue bar", "Queue bar", "Close, \"Queue\", and under it what the songs come from with the glyph for its kind.",
           "ExpressiveQueue.kt", "QueueBar", [{}], queue_bar, single=True, cover=True,
           props=[("Context", "text", "t", None, None)])

    song_props = ctx["components"]["List row/Song"]["props"]

    def queue_row(vp):
        st = vp["State"]
        now = st == "Now playing"
        lifted = st == "Dragging"
        top = bottom = 20 if (now or lifted) else 4
        c = comp(d, "Queue row", layout="v", w=379, sizing=("fixed", "hug"), clip=not lifted,
                 fill="Color/surface-container", cornerRadius=[top, top, bottom, bottom])
        if lifted:
            d.cmd("style.apply", ids=[c], style=d.effect_styles["Elevation/Level 3"])
        r = inst(d, "List row/Song", c, name="Row", Lead="Cover",
                 State="Current playing" if now else ("Lifted" if lifted else "Default"))
        d.set(r, sizingH="fill")
        if not now:
            d.cmd("instance.setProperties", ids=[r], properties={song_props["Title"]: "Porch Light"})
            set_cover(d, r, 0)
        if st == "Default":
            tr = d.find(r, name="Trailing", limit=1)[0]
            d.cmd("instance.swap", ids=[tr], component=d.comp["List row/Drag handle"])
        return c, {}

    S.make("Queue row", "Queue row",
           "The queue's rows. Now playing is its own single cell: the current row, its menu on the kebab. Up next rows sit "
           "in a group with a drag handle in place of the kebab; Dragging is a row held: all four corners 20, the highest "
           "container and the 6dp shadow, unclipped. Rows swipe either way to remove.", "ExpressiveQueue.kt", "QueuePage",
           [dict(State=s) for s in ("Default", "Dragging", "Now playing")], queue_row, cover=True)


# The Headroom mark (Mark.kt AtticMarkVector), cropped to its bounding box: 36 x 51.5.
MARK_PATH = ("M0 51.5 L0 20.78 L36 0 L36 51.5 Z M6 33.5 a12 12 0 1 0 24 0 a12 12 0 1 0 -24 0 Z "
             "M13.2 33.5 a4.8 4.8 0 1 0 9.6 0 a4.8 4.8 0 1 0 -9.6 0 Z M22 15 a3.8 3.8 0 1 0 7.6 0 a3.8 3.8 0 1 0 -7.6 0 Z")


def build(d: Doc, ctx: dict):
    build_actions(d, ctx)
    build_lists(d, ctx)
    build_navigation(d, ctx)
    build_playback(d, ctx)
