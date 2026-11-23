"""Sheets, menus, dialogs, the snackbar and the scrim of Attic Expressive. Page: Components / Overlays.

Owner: playback screens agent. Built from the core components (components_core.py) and placed with its
helpers: `inst(...)`, `set_text(...)`, `set_cover(...)`. Light and Dark come from variable modes. None of these
sets carries a Color mode of its own: an overlay takes the scheme of the screen it opens over, so a sheet over
Now Playing gets Color mode Cover on its instance (the code wraps it in FromCover), except where noted.

Component sets (properties in brackets)
---------------------------------------
Scrim                       ModalBottomSheet's scrim and the platform dialog dim, 411 x 914
  Kind=Sheet (scrim at 32 %) | Dialog (60 %)
Sheet/Drag handle           SheetHandle (Surfaces.kt): the 32 x 4 handle with 16 above and 12 below, 411 wide
Sheet                       ExpressiveSheet (Surfaces.kt): surface container low, 28dp top corners, the handle, a
  slot "Content", the 32dp foot and the 24dp navigation bar inset.  [Content (slot), Show foot (boolean; off for
  Add songs)]
Sheet/Header                SheetHeader (Surfaces.kt)  Art=Cover|None  [Title, Subtitle, Show subtitle]
Sheet/Title                 TitleBlock (ExpressiveSheets.kt)  Second line=True|False  [Title, Subtitle]
Sheet/Quick action          QuickAction (ExpressiveSheets.kt), one 80dp tonal block  [Label, Icon]
Sheet/Quick actions         the track menu's ButtonGroup of three quick actions; nested actions exposed
Sheet/Menu row              MenuCell + CellSymbol (ExpressiveSheets.kt)
  Lead=Icon|Icon on|Album|Artist|None, Lines=One|Two, Selected=False|True, Trailing=None|Radio
  [Label, Supporting, Icon, Icon when on]
Sheet/Picker row            PickerRow (ExpressiveSheets.kt)  Lead=New|Mosaic|Cover, Checked=False|True  [Title, Subtitle]
Sheet/Track menu            ExpressiveTrackMenu  Opened from=Library|Playlist|Now playing|Open with,
  Sleep timer=None|Off|Running.  The Now playing variants carry Color mode Cover.
Sheet/Sleep timer           ExpressiveSleepTimerPicker (Off chosen).  Color mode Cover
Sheet/Language picker       ExpressiveLanguagePicker (English chosen)
Sheet/Playlist picker       ExpressivePlaylistPicker
Sheet/Add songs             ExpressiveAddSongs, 92 % of the screen (843dp), its list clipped
Text field                  DialogTextField (Surfaces.kt)  State=Empty|Filled  [Label, Value]
Dialog                      ExpressiveDialog (Surfaces.kt)
  Content=Message|Text field, Buttons=Two|One, Confirm=Default|Destructive|Disabled
  [Title, Body, Show body, Confirm label, Dismiss label]; the text field is exposed
Menu/Item                   ExpressiveMenuItem (Surfaces.kt)  Danger=False|True, Leading=Icon|Space  [Label, Icon]
Menu                        ExpressiveMenu (Surfaces.kt): surface container, 16dp corners, Elevation/Level 2, a slot
  "Items" holding Menu/Item instances (default: Rename, Delete playlist)
Snackbar                    ExpressiveSnackbar (Surfaces.kt): inverse surface, 8dp corners, Elevation/Level 3  [Message]

Composites (the Sheet/<picker> and track menu sets) are Sheet instances whose Content slot holds Sheet/Header or
Sheet/Title, Sheet/Quick actions and List group/Cell instances (Surface=Sheet) whose Row is swapped to a
Sheet/Menu row or Sheet/Picker row. On a screen, override the nested instances by their layer names
(`Header`, `Favourite`, `Go to album`, `Choice <label>` ...); `nested(d, inst, name)` finds them.

ctx["overlays"] = {set name: {"id", "variants", "props"}} for the screens modules (the settings agent's restore
dialogs use Dialog: Content=Message, Buttons=Two or One).
"""
from __future__ import annotations

from figbuild.kit import Doc, V
from expressive.foundations import DocPage, hrow, vcol, bind_dim
from expressive.components_core import Spec, comp, frame, txt, inst, set_text, set_cover, spacer, vname

REFS: dict[str, str] = {}

PAGE = "Components / Overlays"
SHEETS = "ExpressiveSheets.kt"
SURF = "Surfaces.kt"


# ───────────────────────────────────────────────────────────────── helpers other modules may use
def nested(d: Doc, root: int, name: str) -> int:
    """The first layer named exactly `name` under `root` (inside instances too)."""
    return d.child(root, name)


def set_nested_text(d: Doc, root: int, path: str, text: str):
    """Override a text layer inside an instance by a path of layer names ("Header/Title"). Use it for layers in a
    composite sheet's Content slot, where instance.setProperties on the nested instance has no effect."""
    node = root
    for part in path.split("/"):
        node = d.child(node, part)
    d.set(node, characters=text)


def props_of(ctx: dict, set_name: str) -> dict:
    """{property label: property key} of a component set, for instance.setProperties."""
    return ctx["components"][set_name]["props"]


def set_props(d: Doc, ctx: dict, instance: int, set_name: str, **values):
    """Set text, boolean and swap properties by label (underscores stand for spaces), and variant properties by
    name, on an instance of `set_name`."""
    keys = props_of(ctx, set_name)
    out = {}
    for k, v in values.items():
        label = k.replace("_", " ")
        key = keys.get(label, label)
        out[key] = v if isinstance(v, bool) else str(v)
    d.cmd("instance.setProperties", ids=[instance], properties=out)


def swap_glyph(d: Doc, instance: int, ligature: str, filled=False, layer="Icon"):
    """Swap the Icon/* instance named `layer` inside `instance`."""
    ic = d.find(instance, name=layer, limit=1)[0]
    d.cmd("instance.swap", ids=[ic], component=d.comp[f"Icon/{ligature}" + (" (filled)" if filled else "")])
    return ic


def set_glyph(d: Doc, ctx: dict, instance: int, set_name: str, ligature: str, filled=False, color=None):
    """Set an instance's glyph through its swap property: "Icon when on" for a filled glyph, else "Icon". A swap
    drops the colour the variant gave the glyph, so `color` (a Color variable) is applied again."""
    keys = props_of(ctx, set_name)
    key = keys["Icon when on" if filled and "Icon when on" in keys else "Icon"]
    comp_id = d.comp[f"Icon/{ligature}" + (" (filled)" if filled else "")]
    d.cmd("instance.setProperties", ids=[instance], properties={key: comp_id})
    if color:
        icon = d.find(instance, name="Icon", limit=1)[0]
        d.set(d.find(icon, name="Glyph", limit=1)[0], fill=V(color))


def set_mosaic(d: Doc, instance: int, *indices: int):
    """Put four demo covers into the Picture 1..4 layers of a mosaic inside `instance`, in that order."""
    from expressive.foundations import cover_path
    for i, idx in enumerate(indices, 1):
        d.cmd("image.replace", ids=[d.child(instance, f"Picture {i}")], path=cover_path(idx))


def fill_slot(d: Doc, slot: int, items: list[int]):
    """Move layers (already built elsewhere) into a slot, in order, replacing its default content."""
    old = [c["id"] for c in d.get(slot, depth=1).get("children", [])]
    d.cmd("node.reparent", ids=items, parent=slot)
    if old:
        d.cmd("edit.delete", ids=old)


def wallpaper(d: Doc, root: int):
    """Take `root` and every instance under it back to Color mode Wallpaper (cover-mode sets set their own)."""
    ids = [root] + d.find(root, type="INSTANCE", limit=2000)
    d.set_mode(ids, "Color", "Wallpaper")


def cover_mode(d: Doc, ids):
    d.set_mode(ids if isinstance(ids, list) else [ids], "Color", "Cover")


# ───────────────────────────────────────────────────────────────── build
def build(d: Doc, ctx: dict):
    page = DocPage(d, PAGE, "Overlays",
                   "Bottom sheets, dialogs, the menu, the snackbar and the scrims behind them. Every sheet is Material's "
                   "modal sheet on surface container low with 28dp top corners and the 32 x 4 handle; its groups take "
                   "surface container high cells. A sheet opened over Now Playing, the queue or lyrics takes the cover's "
                   "scheme (Color mode Cover on its instance); anywhere else the wallpaper's. Robolectric draws no "
                   "shadows, so the reference renders show the menu and snackbar flat; their effect styles come from "
                   "the code. Share has no sheet of Attic's own: the track menu's Share row hands the file to Android's "
                   "share sheet.")
    S = Spec(d, ctx, page)
    C = ctx["components"]
    ib_v = dict(Style="Standard", Size="Default", Width="Default", State="Enabled")

    def variant_id(set_name: str, **vp) -> int:
        return C[set_name]["variants"][vname({k.replace("_", " "): v for k, v in vp.items()})]

    def cell(parent, position: str, row_set: str, name: str, **vp) -> tuple[int, int]:
        """A List group/Cell (Surface=Sheet) whose Row is swapped to a variant of `row_set`. Returns (cell, row)."""
        c = inst(d, "List group/Cell", parent, name=f"{name} cell", Position=position, Surface="Sheet")
        row = d.find(c, name="Row", limit=1)[0]
        vid = variant_id(row_set, **vp)
        d.cmd("instance.swap", ids=[row], component=vid)
        # The swap keeps the song row's 64dp minimum height; take the new row's own.
        d.set(row, name=name, sizingH="fill", minHeight=d.get(vid).get("minHeight") or 0)
        return c, row

    def group(parent, name: str) -> int:
        return frame(d, parent, name, layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))

    def positions(n):
        return ["Single"] if n == 1 else ["Top"] + ["Middle"] * (n - 2) + ["Bottom"]

    # Scrim ---------------------------------------------------------------------------------------
    def scrim(vp):
        c = comp(d, "Scrim", w=411, h=914)
        r = d.rect(c, name="Dim", w=411, h=914, x=0, y=0, fill="Color/scrim",
                   constraints={"horizontal": "leftRight", "vertical": "topBottom"})
        d.set(r, opacity=0.32 if vp["Kind"] == "Sheet" else 0.6)
        return c, {}

    S.make("Scrim", "Scrim",
           "What dims the screen under an overlay: the scrim colour at 32 % under a bottom sheet (Material's "
           "ModalBottomSheet), at 60 % under a dialog (the platform's dialog dim, as the renders show it). Stretch it "
           "over the whole screen, status and gesture bars included.",
           SURF, "ExpressiveSheet", [dict(Kind="Sheet"), dict(Kind="Dialog")], scrim, preview=False)

    # Drag handle and the sheet ---------------------------------------------------------------------
    def handle(vp):
        c = comp(d, "Sheet/Drag handle", layout="v", w=411, h=32, pad=[16, 0, 12, 0], cross="center")
        p = d.rect(c, name="Handle", w=32, h=4, fill="Color/outline")
        bind_dim(d, p, "cornerRadius", "radius/full")
        return c, {}

    S.make("Sheet/Drag handle", "Sheet drag handle", "The sheet's handle: 32 x 4 in outline, 16dp above and 12 below.",
           SURF, "SheetHandle", [{}], handle, single=True)

    def sheet(vp):
        c = comp(d, "Sheet", layout="v", w=411, sizing=("fixed", "hug"), fill="Color/surface-container-low",
                 cornerRadius=[28, 28, 0, 0], clip=True)
        h = d.instance("Sheet/Drag handle", c, name="Drag handle")
        d.set(h, sizingH="fill")
        slot = frame(d, c, "Content", layout="v", sizing=("fill", "hug"))
        txt(d, slot, "Put a Sheet/Header or Sheet/Title here, then the groups.", "Body/Medium",
            "Color/on-surface-variant", "Placeholder", fill=True)
        d.set(d.find(slot, name="Placeholder", limit=1)[0], textAutoResize="height")
        foot = spacer(d, c, "Foot", w=411, h=32)
        d.set(foot, sizingH="fill")
        # ModalBottomSheet pads its content by the window's bottom inset (BottomSheetDefaults.windowInsets): the
        # navigation bar, which the sheet's container runs under.
        nav = spacer(d, c, "Navigation bar inset", w=411, h=24)
        d.set(nav, sizingH="fill")
        return c, {"slot": slot, "foot": foot}

    S.make("Sheet", "Bottom sheet",
           "Material's modal bottom sheet as the kit sets it: surface container low, 28dp top corners, the handle, the "
           "content, then 32dp under the last row (off for Add songs, whose list runs to the foot). It opens fully "
           "(skipPartiallyExpanded) and its height follows the content. Its container runs under the navigation bar and "
           "its content stops above it (the 24dp Navigation bar inset, BottomSheetDefaults.windowInsets); the reference "
           "renders, whose sheet windows have no insets, leave it out. Inside, a group's cells are surface container "
           "high (LocalGroupCellColor): use List group/Cell with Surface=Sheet. Fill the Content slot.",
           SURF, "ExpressiveSheet", [{}], sheet, single=True,
           props=[("Show foot", "boolean", "foot", True, None)])
    slot_layer = {}

    def make_slot(set_name, slot_id):
        d.cmd("component.makeSlot", id=slot_id, name="Content")
    sheet_id = d.comp["Sheet"]
    make_slot("Sheet", d.find(sheet_id, name="Content", limit=1)[0])
    C["Sheet"]["props"]["Content"] = "Content"

    # Header and title ------------------------------------------------------------------------------
    def header(vp):
        art = vp["Art"] == "Cover"
        c = comp(d, "Sheet/Header", layout="h", w=411, gap=16, pad=[4, 24, 12, 24], cross="center", sizing=("fixed", "hug"))
        cv = inst(d, "Cover", c, name="Cover", Kind="Album", Size="56")
        if not art:
            d.set(d.find(cv, name="Picture", limit=1)[0], visible=False)
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        t = txt(d, col, "Lights Over the Water", "Kit/Sheet header title", "Color/on-surface", "Title", fill=True, lines=1)
        s = txt(d, col, "Mira Okafor", "Body/Medium", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        return c, {"t": t, "s": s}

    S.make("Sheet/Header", "Sheet header",
           "The track menu's head: the song's 56dp cover (12dp corners), the title in 16/22 semibold over the artist. "
           "Art None is a song with no cover (one opened from another app): the music-note placeholder, and no artist "
           "line when the file has none.", SURF, "SheetHeader",
           [dict(Art="Cover"), dict(Art="None")], header,
           props=[("Title", "text", "t", None, None), ("Subtitle", "text", "s", None, None),
                  ("Show subtitle", "boolean", "s", True, None)])

    def title_block(vp):
        sub = vp["Second line"] == "True"
        c = comp(d, "Sheet/Title", layout="v", w=411, gap=0, pad=[4, 24, 16 if sub else 12, 24], sizing=("fixed", "hug"))
        t = txt(d, c, "Song language" if sub else "Sleep timer", "Headline/Small Emphasized", "Color/on-surface", "Title",
                fill=True)
        lay = {"t": t}
        if sub:
            lay["s"] = txt(d, c, "Porch Light", "Body/Medium", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        return c, lay

    S.make("Sheet/Title", "Sheet title",
           "A picker's title in the emphasised headline (24/32), 24dp in, with the line naming what it acts on under it: "
           "the song, the playlist and its count, or what the sleep timer is doing. The line sits 10dp up into the "
           "title's 12dp bottom margin and has 16 under it.", SHEETS, "TitleBlock",
           [{"Second line": "True"}, {"Second line": "False"}], title_block,
           props=[("Title", "text", "t", None, None), ("Subtitle", "text", "s", None, None)])

    # Quick actions ---------------------------------------------------------------------------------
    def quick(vp):
        c = comp(d, "Sheet/Quick action", layout="v", w=121, h=80, gap=6, cross="center", main="center",
                 fill="Color/secondary-container")
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        ic = d.icon(c, "sensors", color="Color/on-secondary-container", name="Icon")
        lab = txt(d, c, "Start radio", "Kit/Quick action", "Color/on-secondary-container", "Label", fill=True, lines=1,
                  align="center")
        return c, {"l": lab, "i": ic}

    S.make("Sheet/Quick action", "Quick action",
           "One of the track menu's three play actions: an 80dp tonal block with 28dp corners, the glyph over a 12/16 "
           "semibold label, 6dp apart. In a button group the blocks share the width and squeeze their neighbours as "
           "one is pressed.", SHEETS, "QuickAction", [{}], quick, single=True,
           props=[("Label", "text", "l", None, None), ("Icon", "instanceSwap", "i", None, None)])
    qa_props = C["Sheet/Quick action"]["props"]

    def quick_group(vp):
        c = comp(d, "Sheet/Quick actions", layout="h", w=411, gap=8, pad=[4, 16, 12, 16], sizing=("fixed", "hug"))
        lay = {}
        for i, (lab, lig) in enumerate((("Start radio", "sensors"), ("Play next", "playlist_play"),
                                        ("Add to queue", "queue_music"))):
            q = d.instance("Sheet/Quick action", c, name=lab)
            d.set(q, sizingH="fill")
            set_text(d, q, qa_props["Label"], lab)
            set_glyph(d, ctx, q, "Sheet/Quick action", lig, color="Color/on-secondary-container")
            lay[f"q{i}"] = q
        return c, lay

    S.make("Sheet/Quick actions", "Quick actions",
           "Start radio, Play next and Add to queue as one button group (ButtonGroup, fill = true): 8dp apart, 16dp from "
           "the sheet's sides, 4 above and 12 below.", SHEETS, "ExpressiveTrackMenu", [{}], quick_group, single=True,
           expose=("q0", "q1", "q2"))

    # Menu row --------------------------------------------------------------------------------------
    row_defaults = {
        ("Icon", "One"): ("Add to playlist", None, "playlist_add"),
        ("Icon on", "One"): ("Remove from favourites", None, "favorite"),
        ("Icon", "Two"): ("Sleep timer", "23 min left", "playlist_add"),
        ("Icon on", "Two"): ("Sleep timer", "23 min left", "favorite"),
        ("Album", "Two"): ("Go to album", "Paper Lanterns", None),
        ("Artist", "Two"): ("Go to artist", "Mira Okafor", None),
        ("None", "One"): ("15 minutes", None, None),
    }

    def menu_row(vp):
        lead, lines, sel, trail = vp["Lead"], vp["Lines"], vp["Selected"] == "True", vp["Trailing"]
        two = lines == "Two"
        label, sup, glyph = row_defaults[(lead, lines)]
        if sel:
            label = "Off"
        c = comp(d, "Sheet/Menu row", layout="h", w=379, gap=16, pad=[0, 16, 0, 16], cross="center",
                 sizing=("fixed", "hug"), fill="Color/secondary-container" if sel else None)
        d.set(c, minHeight=64 if two else 56)
        lay = {}
        if lead in ("Icon", "Icon on"):
            box = frame(d, c, "Lead", layout="h", w=40, h=40, cross="center", main="center")
            on = lead == "Icon on"
            lay["icon_on" if on else "icon"] = d.icon(box, glyph, color="Color/primary" if on else "Color/on-surface-variant",
                                                      filled=on, name="Icon")
        elif lead in ("Album", "Artist"):
            box = frame(d, c, "Lead", w=40, h=40)
            cv = inst(d, "Cover", box, name="Cover", Kind=lead, Size="48", x=0, y=0)
            d.set(cv, width=40, height=40)
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        lay["l"] = txt(d, col, label, "Kit/Play label" if sel else "Body/Large", "Color/on-surface", "Label", fill=True, lines=1)
        if two:
            lay["s"] = txt(d, col, sup, "Body/Medium", "Color/on-surface-variant", "Supporting", fill=True, lines=1)
        if trail == "Radio":
            # Material's RadioButton without onClick has no 48dp touch target: 20dp plus 2 each side.
            box = frame(d, c, "Trailing", w=24, h=24)
            inst(d, "Radio", box, name="Radio", Selected=str(sel), x=-12, y=-12)
        return c, lay

    vs = [dict(Lead="Icon", Lines="One", Selected="False", Trailing="None"),
          dict(Lead="Icon on", Lines="One", Selected="False", Trailing="None"),
          dict(Lead="Icon", Lines="Two", Selected="False", Trailing="None"),
          dict(Lead="Icon on", Lines="Two", Selected="False", Trailing="None"),
          dict(Lead="Album", Lines="Two", Selected="False", Trailing="None"),
          dict(Lead="Artist", Lines="Two", Selected="False", Trailing="None"),
          dict(Lead="None", Lines="One", Selected="False", Trailing="Radio"),
          dict(Lead="None", Lines="One", Selected="True", Trailing="Radio")]
    S.make("Sheet/Menu row", "Sheet menu row",
           "A row in a sheet's group: a 40dp lead (a 24dp glyph, or the album's 40dp cover or the artist's cookie) so every "
           "label starts on the 72dp keyline, the label in body large, and an optional line under it naming where the row "
           "leads (64dp tall instead of 56). Icon on is a toggle that is on: the glyph filled, in primary (Remove from "
           "favourites, a running sleep timer). A picker's rows have no lead and a radio; the chosen one takes the tonal "
           "fill and a semibold label. Put rows in List group/Cell (Surface=Sheet) by swapping its Row.",
           SHEETS, "MenuCell", vs, menu_row,
           props=[("Label", "text", "l", None, None), ("Supporting", "text", "s", None, None),
                  ("Icon", "instanceSwap", "icon", None, None), ("Icon when on", "instanceSwap", "icon_on", None, None)])

    # Picker row ------------------------------------------------------------------------------------
    def picker_row(vp):
        new, checked = vp["Lead"] == "New", vp["Checked"] == "True"
        c = comp(d, "Sheet/Picker row", layout="h", w=379, gap=12, pad=[8, 4, 8, 12], cross="center", sizing=("fixed", "hug"))
        d.set(c, minHeight=72)
        if new:
            box = frame(d, c, "Lead", layout="h", w=56, h=56, cross="center", main="center", fill="Color/primary-container")
            bind_dim(d, box, "cornerRadius", "radius/medium")
            d.icon(box, "add", color="Color/on-primary-container", name="Icon")
        elif vp["Lead"] == "Mosaic":
            cv = inst(d, "Cover", c, name="Lead", Kind="Mosaic", Size="56")
            set_mosaic(d, cv, 10, 8, 3, 6)
        else:
            cv = inst(d, "Cover", c, name="Lead", Kind="Album", Size="56")
            set_cover(d, cv, 1)
        col = frame(d, c, "Text column", layout="v", sizing=("fill", "hug"))
        lay = {"t": txt(d, col, "New playlist" if new else ("Long drive" if vp["Lead"] == "Mosaic" else "Focus"), "Kit/Row title", "Color/on-surface", "Title",
                        fill=True, lines=1)}
        if not new:
            lay["s"] = txt(d, col, "12 songs" if vp["Lead"] == "Mosaic" else "4 songs", "Body/Medium", "Color/on-surface-variant", "Subtitle", fill=True, lines=1)
        if checked:
            box = frame(d, c, "Trailing", layout="h", pad=[0, 12, 0, 0], sizing=("hug", "hug"))
            d.icon(box, "check", color="Color/primary", name="Check")
        return c, lay

    S.make("Sheet/Picker row", "Playlist picker row",
           "A row of the playlist picker: a 56dp lead, the name over its song count, and a tick in primary when the "
           "playlist already holds what is being added. Mosaic draws one cover (Lead=Cover) when the playlist has art from fewer than four albums. New is the picker's first cell: the add glyph on primary "
           "container (12dp corners) and no second line.", SHEETS, "PickerRow",
           [dict(Lead="New", Checked="False"), dict(Lead="Mosaic", Checked="False"), dict(Lead="Mosaic", Checked="True"),
            dict(Lead="Cover", Checked="False")],
           picker_row, props=[("Title", "text", "t", None, None), ("Subtitle", "text", "s", None, None)])

    row_props = C["Sheet/Menu row"]["props"]
    pick_props = C["Sheet/Picker row"]["props"]

    def sheet_shell(c, *, foot=True):
        """A Sheet instance filling the composite `c`; returns (sheet instance, its Content slot)."""
        s = d.instance("Sheet", c, name="Sheet")
        d.set(s, sizingH="fill")
        if not foot:
            d.cmd("instance.setProperties", ids=[s], properties={C["Sheet"]["props"]["Show foot"]: False})
        return s, d.find(s, name="Content", limit=1)[0]

    def holder():
        """A scratch frame to build slot content in before it moves into the slot."""
        return frame(d, None, "Slot content", layout="v", w=411, sizing=("fixed", "hug"), x=-6000, y=0)

    def menu_cells(parent, rows):
        """rows: [(layer name, vp dict, {prop: value}, glyph or None, filled)]"""
        for pos, (nm, vp, values, glyph, filled) in zip(positions(len(rows)), rows):
            _, r = cell(parent, pos, "Sheet/Menu row", nm, **vp)
            if values:
                d.cmd("instance.setProperties", ids=[r], properties={row_props[k]: v for k, v in values.items()})
            if glyph:
                set_glyph(d, ctx, r, "Sheet/Menu row", glyph, filled,
                          "Color/primary" if filled else "Color/on-surface-variant")

    ICON1 = dict(Lead="Icon", Lines="One", Selected="False", Trailing="None")
    ICON_ON1 = dict(Lead="Icon on", Lines="One", Selected="False", Trailing="None")
    ICON2 = dict(Lead="Icon", Lines="Two", Selected="False", Trailing="None")
    ICON_ON2 = dict(Lead="Icon on", Lines="Two", Selected="False", Trailing="None")
    ALBUM = dict(Lead="Album", Lines="Two", Selected="False", Trailing="None")
    ARTIST = dict(Lead="Artist", Lines="Two", Selected="False", Trailing="None")

    def radio_row(selected):
        return dict(Lead="None", Lines="One", Selected=str(selected), Trailing="Radio")

    # Track menu ------------------------------------------------------------------------------------
    def track_menu(vp):
        src, timer = vp["Opened from"], vp["Sleep timer"]
        c = comp(d, "Sheet/Track menu", layout="v", w=411, sizing=("fixed", "hug"))
        if src == "Now playing":
            d.set_mode(c, "Color", "Cover")
        s, slot = sheet_shell(c)
        h = holder()
        hd = inst(d, "Sheet/Header", h, name="Header", Art="None" if src == "Open with" else "Cover")
        d.set(hd, sizingH="fill")
        if src == "Open with":
            d.cmd("instance.setProperties", ids=[hd], properties={C["Sheet/Header"]["props"]["Title"]: "band-practice",
                                                                  C["Sheet/Header"]["props"]["Show subtitle"]: False})
        if timer != "None":
            g = group(h, "Sleep timer group")
            running = timer == "Running"
            menu_cells(g, [("Sleep timer", ICON_ON2 if running else ICON1,
                            {"Label": "Sleep timer", **({"Supporting": "23 min left"} if running else {})},
                            "bedtime", running)])
            spacer(d, h, "Group spacing", w=411, h=12)
        if src != "Open with":
            qa = d.instance("Sheet/Quick actions", h, name="Quick actions")
            d.set(qa, sizingH="fill")
            liked = src != "Library"
            g = group(h, "Filing group")
            rows = [("Favourite", ICON_ON1 if liked else ICON1,
                     {"Label": "Remove from favourites" if liked else "Add to favourites"}, "favorite", liked),
                    ("Add to playlist", ICON1, {"Label": "Add to playlist"}, "playlist_add", False),
                    ("Language", ICON1, {"Label": "Language: English"}, "language", False)]
            if src == "Playlist":
                rows.append(("Remove from this playlist", ICON1, {"Label": "Remove from this playlist"},
                             "do_not_disturb_on", False))
            menu_cells(g, rows)
            spacer(d, h, "Group spacing 2", w=411, h=12)
            g2 = group(h, "Going group")
            menu_cells(g2, [("Go to album", ALBUM, {"Label": "Go to album", "Supporting": "Paper Lanterns"}, None, False),
                            ("Go to artist", ARTIST, {"Label": "Go to artist", "Supporting": "Mira Okafor"}, None, False),
                            ("Share", ICON1, {"Label": "Share"}, "share", False)])
        kids = [k["id"] for k in d.get(h, depth=1)["children"]]
        fill_slot(d, slot, kids)
        for k in kids:
            d.set(k, sizingH="fill")
        d.cmd("edit.delete", ids=[h])
        set_cover(d, c, 0)
        return c, {}

    vs = [{"Opened from": "Library", "Sleep timer": "None"}, {"Opened from": "Playlist", "Sleep timer": "None"},
          {"Opened from": "Now playing", "Sleep timer": "Off"}, {"Opened from": "Now playing", "Sleep timer": "Running"},
          {"Opened from": "Open with", "Sleep timer": "Off"}]
    S.make("Sheet/Track menu", "Track menu",
           "The song's sheet: its header; from Now Playing, the sleep timer in a group of its own (running: the filled "
           "moon in primary and the time left); the three quick actions; filing (favourite, add to playlist, its "
           "language, and out of this playlist when opened from one); then going elsewhere (its album and artist, each "
           "by its cover, and Share, which hands the file to Android's share sheet). A song opened from another app "
           "stops after the sleep timer. The Now playing variants carry Color mode Cover, as the code wraps the sheet "
           "in FromCover there. Override a row through its layer name (Favourite, Language, Go to album, ...).",
           SHEETS, "ExpressiveTrackMenu", vs, track_menu)

    # Pickers ---------------------------------------------------------------------------------------
    def picker(name, title, subtitle, choices, chosen, *, cover=False):
        def b(vp):
            c = comp(d, name, layout="v", w=411, sizing=("fixed", "hug"))
            if cover:
                d.set_mode(c, "Color", "Cover")
            s, slot = sheet_shell(c)
            h = holder()
            tb = inst(d, "Sheet/Title", h, name="Title", Second_line=str(subtitle is not None))
            tp = C["Sheet/Title"]["props"]
            d.cmd("instance.setProperties", ids=[tb], properties={tp["Title"]: title, **({tp["Subtitle"]: subtitle} if subtitle else {})})
            g = group(h, "Choices")
            menu_cells(g, [(f"Choice {ch}", radio_row(ch == chosen), {"Label": ch}, None, False) for ch in choices])
            kids = [k["id"] for k in d.get(h, depth=1)["children"]]
            fill_slot(d, slot, kids)
            for k in kids:
                d.set(k, sizingH="fill")
            d.cmd("edit.delete", ids=[h])
            return c, {}
        return b

    S.make("Sheet/Sleep timer", "Sleep timer sheet",
           "Opened from Sleep timer in the track menu over Now Playing, in place of the menu: the title (with what the "
           "timer is doing under it while it runs) and the choices as one single-choice group. A choice sets the timer "
           "and closes the sheet. Color mode Cover, as it only opens over the player.",
           SHEETS, "ExpressiveSleepTimerPicker", [{}],
           picker("Sheet/Sleep timer", "Sleep timer", None,
                  ["15 minutes", "30 minutes", "45 minutes", "1 hour", "End of this song", "Off"], "Off", cover=True),
           single=True)

    S.make("Sheet/Language picker", "Language picker",
           "Song language, with the song under the title, then the seven choices as one single-choice group. The chosen "
           "row takes the tonal fill and its radio is on; an unset language (Unknown) has nothing chosen.",
           SHEETS, "ExpressiveLanguagePicker", [{}],
           picker("Sheet/Language picker", "Song language", "Porch Light",
                  ["Tamil", "Hindi", "Telugu", "Malayalam", "Kannada", "English", "No vocals"], "English"),
           single=True)

    def playlist_picker(vp):
        c = comp(d, "Sheet/Playlist picker", layout="v", w=411, sizing=("fixed", "hug"))
        s, slot = sheet_shell(c)
        h = holder()
        tb = inst(d, "Sheet/Title", h, name="Title", Second_line="True")
        tp = C["Sheet/Title"]["props"]
        d.cmd("instance.setProperties", ids=[tb], properties={tp["Title"]: "Add to playlist", tp["Subtitle"]: "Porch Light"})
        g = group(h, "New playlist group")
        _, r = cell(g, "Single", "Sheet/Picker row", "New playlist", Lead="New", Checked="False")
        d.cmd("instance.setProperties", ids=[r], properties={pick_props["Title"]: "New playlist"})
        spacer(d, h, "Group spacing", w=411, h=12)
        g2 = group(h, "Playlists")
        for pos, (nm, count, chk, covers) in zip(positions(3), [("Long drive", "12 songs", False, (10, 8, 3, 6)),
                                                                 ("Sunday cleaning", "5 songs", True, (3, 9, 2, 7)),
                                                                 ("Focus", "4 songs", False, (1,))]):
            _, r = cell(g2, pos, "Sheet/Picker row", nm, Lead="Mosaic" if len(covers) == 4 else "Cover", Checked=str(chk))
            d.cmd("instance.setProperties", ids=[r], properties={pick_props["Title"]: nm, pick_props["Subtitle"]: count})
            if len(covers) == 4:
                set_mosaic(d, r, *covers)
            else:
                set_cover(d, r, covers[0])
        kids = [k["id"] for k in d.get(h, depth=1)["children"]]
        fill_slot(d, slot, kids)
        for k in kids:
            d.set(k, sizingH="fill")
        d.cmd("edit.delete", ids=[h])
        return c, {}

    S.make("Sheet/Playlist picker", "Playlist picker",
           "Add to playlist: the title with the song (or \"n songs\") under it, \"New playlist\" as a cell of its own, "
           "then each playlist with its mosaic, name and count, ticked when it already holds the song. New playlist "
           "opens the name dialog over the picker.", SHEETS, "ExpressivePlaylistPicker", [{}], playlist_picker, single=True)

    song_props = C["List row/Song"]["props"]
    tog_props = C["Toggle button"]["props"]
    add_rows = [("404 Heartbeat", "Velvet Circuit", 10, False), ("Aadhi Neend", "Ruhaan Sethi", 9, False),
                ("Afterglow Protocol", "Velvet Circuit", 10, False), ("Auto Rickshaw Waltz", "Tide & Tamarind", 7, True),
                ("Azulejo", "Paloma Ferrer", 2, False), ("Baadal Ka Khat", "Ruhaan Sethi", 6, False),
                ("Bheegi Sadak", "Ruhaan Sethi", 6, True), ("Bijli Gayi", "Ruhaan Sethi", 6, False),
                ("Brushes", "Odile Marchetti", 4, False), ("Calle de los Naranjos", "Paloma Ferrer", 2, False),
                ("Chai Ka Doosra Cup", "Ruhaan Sethi", 9, False)]

    def add_songs(vp):
        c = comp(d, "Sheet/Add songs", layout="v", w=411, h=843, clip=True)
        s, slot = sheet_shell(c, foot=False)
        d.set(s, sizingV="fill")
        h = holder()
        tb = inst(d, "Sheet/Title", h, name="Title", Second_line="True")
        tp = C["Sheet/Title"]["props"]
        d.cmd("instance.setProperties", ids=[tb], properties={tp["Title"]: "Add songs", tp["Subtitle"]: "Long drive, 12 songs"})
        sbox = frame(d, h, "Search box", layout="v", pad=[0, 16, 12, 16], sizing=("fill", "hug"))
        sf = inst(d, "Search field", sbox, name="Search field", State="Empty")
        d.set(sf, sizingH="fill")
        d.cmd("instance.setProperties", ids=[sf], properties={C["Search field"]["props"]["Text"]: "Find a song"})
        g = group(h, "Songs")
        n = len(add_rows)
        for i, (title, artist, cov, added) in enumerate(add_rows):
            pos = "Top" if i == 0 else "Middle"
            cl = inst(d, "List group/Cell", g, name=f"{title} cell", Position=pos, Surface="Sheet")
            r = d.find(cl, name="Row", limit=1)[0]
            d.set(r, name=title)
            d.cmd("instance.setProperties", ids=[r], properties={song_props["Title"]: title, song_props["Subtitle"]: artist})
            set_cover(d, r, cov)
            tr = d.find(r, name="Trailing", limit=1)[0]
            d.cmd("instance.swap", ids=[tr], component=C["Toggle button"]["variants"][
                vname({"Checked": str(added), "Size": "Default", "Off style": "Standard"})])
            swap_glyph(d, tr, "check" if added else "add", filled=added)
        kids = [k["id"] for k in d.get(h, depth=1)["children"]]
        fill_slot(d, slot, kids)
        for k in kids:
            d.set(k, sizingH="fill")
        d.cmd("edit.delete", ids=[h])
        return c, {}

    S.make("Sheet/Add songs", "Add songs",
           "Add songs to a playlist: 92 % of the screen, the title with the playlist and its count, the pill search field "
           "(\"Find a song\"), then every matching song with an add toggle that squares off and fills with a tick once "
           "the song is in the playlist. The sheet stays open; its list runs to the foot, so the sheet has no 32dp foot.",
           SHEETS, "ExpressiveAddSongs", [{}], add_songs, single=True)

    # Text field and dialog -------------------------------------------------------------------------
    def text_field(vp):
        filled = vp["State"] == "Filled"
        c = comp(d, "Text field", w=363, h=64)
        box = frame(d, c, "Container", layout="h", w=363, h=56, x=0, y=8, pad=[0, 16, 0, 16], cross="center",
                    stroke="Color/outline", stroke_w=1, constraints={"horizontal": "leftRight", "vertical": "top"})
        bind_dim(d, box, "cornerRadius", "radius/extra-small")
        lay = {}
        if filled:
            lay["v"] = txt(d, box, "Road trip", "Body/Large", "Color/on-surface", "Value", fill=True, lines=1)
            lab = frame(d, c, "Floating label", layout="h", x=12, y=0, pad=[0, 4, 0, 4], fill="Color/surface-container-high",
                        sizing=("hug", "hug"))
            lay["l"] = txt(d, lab, "Playlist name", "Body/Small", "Color/on-surface-variant", "Label")
        else:
            lay["l"] = txt(d, box, "Playlist name", "Body/Large", "Color/on-surface-variant", "Label", fill=True, lines=1)
        return c, lay

    S.make("Text field", "Text field",
           "Material's outlined text field as a dialog uses it: 56dp, 4dp corners, a 1dp outline, 8dp above for the label "
           "to float into. Empty shows the label inside; once there is text the label floats onto the outline at 12/16 "
           "(its backing is the dialog's surface container high, so the line breaks around it). Focused, the outline "
           "turns primary at 2dp; the renders show the field unfocused.",
           SURF, "DialogTextField", [dict(State="Empty"), dict(State="Filled")], text_field,
           props=[("Label", "text", "l", None, None), ("Value", "text", "v", None, None)])
    tf_props = C["Text field"]["props"]
    btn_props = C["Button"]["props"]

    def dialog(vp):
        field, one, confirm = vp["Content"] == "Text field", vp["Buttons"] == "One", vp["Confirm"]
        c = comp(d, "Dialog", layout="v", w=411 if field else 391, pad=24, fill="Color/surface-container-high",
                 sizing=("fixed", "hug"))
        bind_dim(d, c, "cornerRadius", "radius/extra-large")
        tbox = frame(d, c, "Title box", layout="v", pad=[0, 0, 16, 0], sizing=("fill", "hug"))
        title = {"Destructive": "Delete Long drive?", "Disabled": "New playlist"}.get(
            confirm, "Name this playlist" if field else ("Couldn't restore" if one else "Restore this backup?"))
        lay = {"t": txt(d, tbox, title, "Headline/Small Emphasized", "Color/on-surface", "Title", fill=True)}
        bbox = frame(d, c, "Body box", layout="v", pad=[0, 0, 24, 0], sizing=("fill", "hug"))
        if field:
            f = inst(d, "Text field", bbox, name="Text field", State="Empty" if confirm == "Disabled" else "Filled")
            d.set(f, sizingH="fill")
            lay["field"] = f
        else:
            body = {"Destructive": "The playlist goes away. The songs stay in your library."}.get(
                confirm, "This file isn't an Attic backup." if one else
                "Made November 2, 2026, with 42 favourites, 3 playlists, 4,870 plays and lyrics for 51 songs. They are "
                "added to what is on this phone, and nothing here is deleted. Theme, style and autoplay change to the "
                "backup's.")
            lay["b"] = txt(d, bbox, body, "Body/Medium", "Color/on-surface-variant", "Body", fill=True)
            d.set(lay["b"], textAutoResize="height")
            lay["bb"] = bbox
        row = frame(d, c, "Buttons", layout="h", gap=8, main="max", cross="center", sizing=("fill", "hug"))
        if not one:
            dm = inst(d, "Button", row, name="Dismiss", Style="Text", Size="Small", Icon="None", State="Enabled",
                      Squared="False")
            set_text(d, dm, btn_props["Label"], "Cancel")
            lay["dm"] = dm
        cf = inst(d, "Button", row, name="Confirm", Style="Text danger" if confirm == "Destructive" else "Text",
                  Size="Small", Icon="None", State="Disabled" if confirm == "Disabled" else "Enabled", Squared="False")
        set_text(d, cf, btn_props["Label"], {"Destructive": "Delete playlist", "Disabled": "Create"}.get(
            confirm, "Create" if field else ("Close" if one else "Restore")))
        lay["cf"] = cf
        return c, lay

    vs = [dict(Content="Message", Buttons="Two", Confirm="Default"),
          dict(Content="Message", Buttons="Two", Confirm="Destructive"),
          dict(Content="Message", Buttons="One", Confirm="Default"),
          dict(Content="Text field", Buttons="Two", Confirm="Disabled"),
          dict(Content="Text field", Buttons="Two", Confirm="Default")]
    S.make("Dialog", "Dialog",
           "Material's alert dialog as the kit sets it: surface container high, 28dp corners, 24dp padding, the title in "
           "the emphasised headline (24/32), the body in body medium on-surface-variant, then text buttons at the end, 8dp "
           "apart: Cancel, then the confirm. Destructive puts the confirm in the error colour (Delete playlist). The "
           "playlist name dialog holds the text field in place of the body, and its confirm waits for a name (Disabled). "
           "Width follows the content between 280 and the window (the renders: 411 with a field, 391 for Delete, 280 "
           "for a short message): set the instance's width. It sits in the middle of the screen over the 60 % scrim.",
           SURF, "ExpressiveDialog", vs, dialog,
           props=[("Title", "text", "t", None, None), ("Body", "text", "b", None, None),
                  ("Show body", "boolean", "bb", True, None)], expose=("field", "dm", "cf"))

    # Menu ------------------------------------------------------------------------------------------
    def menu_item(vp):
        danger, has_icon = vp["Danger"] == "True", vp["Leading"] == "Icon"
        col = "Color/error" if danger else "Color/on-surface"
        c = comp(d, "Menu/Item", layout="h", w=180, h=48, gap=12, pad=[0, 12, 0, 12], cross="center")
        lay = {}
        if has_icon:
            lay["i"] = d.icon(c, "delete" if danger else "edit", color="Color/error" if danger else "Color/on-surface-variant",
                              name="Icon")
        else:
            spacer(d, c, "Icon space", w=24, h=24)
        lay["l"] = txt(d, c, "Delete playlist" if danger else "Rename", "Label/Large", col, "Label", fill=True, lines=1)
        return c, lay

    S.make("Menu/Item", "Menu item",
           "A menu item: a 24dp glyph and the label in label large, 48dp tall, 12dp in. Danger turns both to the error "
           "colour. Leading Space keeps the glyph's space, so a menu of choices can tick only the current one.",
           SURF, "ExpressiveMenuItem",
           [dict(Danger="False", Leading="Icon"), dict(Danger="True", Leading="Icon"), dict(Danger="False", Leading="Space")],
           menu_item, props=[("Label", "text", "l", None, None), ("Icon", "instanceSwap", "i", None, None)])
    mi_props = C["Menu/Item"]["props"]

    def menu(vp):
        c = comp(d, "Menu", layout="v", pad=8, fill="Color/surface-container", sizing=("hug", "hug"))
        bind_dim(d, c, "cornerRadius", "radius/large")
        d.set(c, minWidth=196)
        d.cmd("style.apply", ids=[c], style=d.effect_styles["Elevation/Level 2"])
        items = frame(d, c, "Items", layout="v", sizing=("hug", "hug"))
        a = inst(d, "Menu/Item", items, name="Rename", Danger="False", Leading="Icon")
        b = inst(d, "Menu/Item", items, name="Delete playlist", Danger="True", Leading="Icon")
        for i, (lab, lig, col) in ((a, ("Rename", "edit", "Color/on-surface-variant")),
                                   (b, ("Delete playlist", "delete", "Color/error"))):
            d.set(i, sizingH="fill")
            set_text(d, i, mi_props["Label"], lab)
            set_glyph(d, ctx, i, "Menu/Item", lig, color=col)
        d.set(items, minWidth=180)
        return c, {"items": items}

    S.make("Menu", "Menu",
           "Material's dropdown menu as the kit sets it: surface container, 16dp corners, the 3dp shadow (Elevation/Level 2), "
           "8dp round the items and at least 196dp wide. It opens from the button that shows it, its top-end corner "
           "under the button's (the playlist kebab's Rename and Delete playlist; Library's sort menu). Put Menu/Item "
           "instances in the Items slot.", SURF, "ExpressiveMenu", [{}], menu, single=True)
    d.cmd("component.makeSlot", id=d.find(d.comp["Menu"], name="Items", limit=1)[0], name="Items")
    C["Menu"]["props"]["Items"] = "Items"

    # Snackbar ------------------------------------------------------------------------------------------
    def snackbar(vp):
        c = comp(d, "Snackbar", layout="h", w=355, h=48, pad=[0, 16, 0, 16], cross="center", fill="Color/inverse-surface")
        bind_dim(d, c, "cornerRadius", "radius/small")
        d.cmd("style.apply", ids=[c], style=d.effect_styles["Elevation/Level 3"])
        t = txt(d, c, "Added to queue", "Body/Medium", "Color/inverse-on-surface", "Message", fill=True, lines=1)
        return c, {"t": t}

    S.make("Snackbar", "Snackbar",
           "A message: inverse surface, 8dp corners, text only, 48dp for one line, with the 6dp shadow. It spans the screen "
           "less 28dp each side (the host's 16 and Material's 12) and sits 16dp above the mini player, or 24 above the "
           "gesture bar over the player, queue or lyrics, where it takes the cover scheme's inverse roles (Color mode "
           "Cover on the instance).", SURF, "ExpressiveSnackbar", [{}], snackbar, single=True,
           props=[("Message", "text", "t", None, None)])

    ctx["overlays"] = {k: C[k] for k in C if k in OVERLAY_SETS}


OVERLAY_SETS = ("Scrim", "Sheet/Drag handle", "Sheet", "Sheet/Header", "Sheet/Title", "Sheet/Quick action",
                "Sheet/Quick actions", "Sheet/Menu row", "Sheet/Picker row", "Sheet/Track menu", "Sheet/Sleep timer",
                "Sheet/Language picker", "Sheet/Playlist picker", "Sheet/Add songs", "Text field", "Dialog",
                "Menu/Item", "Menu", "Snackbar")
