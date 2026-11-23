"""Sheets, menus, pop-overs, dialogs, toasts. Page: Components / Overlays.

Owner: playback screens agent. Drawn from ui/comfort/Overlays.kt and ComfortSheets.kt; every set binds
colours to Color/*, text to text styles, icons as Icon/* instances, and is linked with codeConnect
(compose, app.attic.player.ui.comfort). Variant properties are Prop=Value|Value; component properties
after "props". Sheets over Now Playing, the queue or lyrics take the player's wash: set Color mode
`Tinted wash` on the screen frame, not on the component.

COMPONENTS / OVERLAYS
  Sheet            (single) raised, 30dp top corners, the 40 x 4 grab handle in fill-2, a slot "Content", and
                   30dp at the foot plus the navigation inset (set Foot to 54 on a screen), its bottom on the screen's bottom edge. Shadow/Floating, no edge.
                   Overlays.kt ComfortSheet / StaticSheet. Fill: instance.slotInsert(replace=True) the parts below.
  Sheet header     (single) props Title, Subtitle, Show subtitle; the nested "Cover" (Cover/Album, 48) is exposed.
                   Overlays.kt SheetHeader
  Sheet title      (single) props Title, Subtitle, Show subtitle. Overlays.kt SheetTitle
  Quick action     (single) props Label; the nested "Button" (Tonal icon button, Large) is exposed: set Icon,
                   Checked and Container on it (Container Fill in light, Fill 2 in dark). ComfortSheets.kt QuickAction
  Quick actions    (single) Start radio, Add to favourites, Play next, Add to queue as four exposed Quick action
                   instances, then the full-width rule. ComfortSheets.kt ComfortTrackMenu
  Sheet row        Kind=Default|Selected|Danger, End=None|Chevron|Pop-over (Selected and Danger: End=None).
                   props Label, Show icon, Icon (swap), Value, Show value, Show hairline. Overlays.kt SheetRow
  Menu             On sheet=False|True. Slot "Content". Raised (fill on a sheet), radius 22, Shadow/Floating and the
                   1dp edge. Overlays.kt ComfortMenu / StaticMenu
  Menu caption     (single) props Text. Overlays.kt MenuCaption
  Menu row         Kind=Default|Selected|Danger. props Label, Show icon, Icon (swap), Show check. Overlays.kt MenuRow
  Dialog field     State=Empty|Filled. props Label, Value, Show caret. Overlays.kt DialogField
  Dialog           Content=Body|Field, Buttons=Two|One, Confirm=Default|Disabled|Danger.
                   props Title, Body, Dismiss label, Confirm label; the nested "Field" (Dialog field) is exposed.
                   Overlays.kt ComfortDialog / DialogCard. Place it 12dp in from the sides and 22 above the navigation inset (46 above the edge).
  Toast            (single) props Text. Overlays.kt Toast (ToastHost shows snackbars as this)
  Scrim            (single) a 411 x 914 rectangle in Color/scrim, under sheets and dialogs. Overlays.kt

Text styles: Component/Sheet header title, Overlay row, Overlay row selected, Sheet value, Menu caption, Field label,
Field and Toast, made in foundations.py (OVERLAY_TYPE).
"""
from __future__ import annotations

from figbuild.kit import Doc, V

from .components_core import K, SetBuilder, Components, _swap_icon, _set_text_prop, _set_text_prop_bool, COVER, cover_image

REFS: dict[str, str] = {}

PKG = "app.attic.player.ui.comfort"

def build(d: Doc, ctx: dict):
    k = K(d, ctx["tokens"])
    cc = Components(d, "Components / Overlays",
                    "Things that need a decision sit at the bottom: the track menu, the playlist picker and Add songs are "
                    "bottom sheets; naming and deleting a playlist are bottom dialogs. A short choice (sleep timer, song "
                    "language) opens as a pop-over beside the row that asked. Sheets, dialogs and menus are raised; the "
                    "scrim dims what is under them. Over Now Playing, the queue or lyrics they take the player's wash: set "
                    "Color mode Tinted wash on the screen. Source: ui/comfort/Overlays.kt, ComfortSheets.kt.")
    _sheet_parts(d, k, cc)
    _sheet(d, k, cc)
    _menus(d, k, cc)
    _dialogs(d, k, cc)
    _toast_scrim(d, k, cc)
    cc.dark_preview()


def _hairline(d, k, parent, x, w, name="Hairline"):
    hl = d.rect(parent, name=name, w=w, h=1, fill="Color/line")
    k.absolute(hl, x, 0, w, 1, {"horizontal": "leftRight", "vertical": "top"})
    d.set(hl, visible=False)
    return hl


# ------------------------------------------------------------------------ sheet parts
def _sheet_parts(d: Doc, k: K, cc: Components):
    # -- Sheet header
    c = cc.single("Sheet header", "Overlays.kt · SheetHeader", "SheetHeader", layout="h", w=411, pad=[4, 24, 14, 24], gap=14,
                  cross="center", sizing=("fixed", "hug"))
    cv = d.instance("Cover/Album", c, name="Cover")
    d.cmd("instance.setProperties", ids=[cv], properties={"Shape": "Squircle", "Art": "Image", "Dimmed": "False"})
    d.set(cv, width=48, height=48)
    cover_image(d, cv, COVER[0])
    d.cmd("component.exposeInstance", id=cv)
    lab = d.frame(c, name="Labels", layout="v", sizing=("fill", "hug"))
    t1 = k.text(lab, "Lights Over the Water", "Component/Sheet header title", "Color/on", "Title", fill=True, lines=1)
    t2 = k.text(lab, "Mira Okafor", "Scale/Sub", "Color/on-2", "Subtitle", fill=True, lines=1)
    cc.finish_single("Sheet header", c, "Overlays.kt · SheetHeader", "SheetHeader",
                     "The song at the top of the track menu: its 48dp cover (exposed: Art=None for a file opened from another "
                     "app), the title in 17/22 semibold and the artist in 13.5/18 on-2, 14dp apart. 24dp sides, 4 above, 14 below.",
                     binds=[("Title", "text", t1, "Lights Over the Water"), ("Subtitle", "text", t2, "Mira Okafor"),
                            ("Show subtitle", "boolean", t2, True)])

    # -- Sheet title
    c = cc.single("Sheet title", "Overlays.kt · SheetTitle", "SheetTitle", layout="v", w=411, pad=[6, 24, 12, 24], gap=2,
                  sizing=("fixed", "hug"))
    t1 = k.text(c, "Add songs", "Scale/Bar title", "Color/on", "Title", fill=True)
    t2 = k.text(c, "Long drive, 12 songs", "Scale/Sub", "Color/on-2", "Subtitle", fill=True, lines=1)
    cc.finish_single("Sheet title", c, "Overlays.kt · SheetTitle", "SheetTitle",
                     "A sheet's title in 20/26 bold (\"Add to playlist\"), and optionally what it acts on in 13.5/18 on-2, 2dp "
                     "under it. 24dp sides, 6 above, 12 below.",
                     binds=[("Title", "text", t1, "Add songs"), ("Subtitle", "text", t2, "Long drive, 12 songs"),
                            ("Show subtitle", "boolean", t2, True)])

    # -- Quick action
    c = cc.single("Quick action", "ComfortSheets.kt · QuickAction", "QuickAction", layout="v", w=96.75, pad=[6, 0, 0, 0], gap=8,
                  cross="center", sizing=("fixed", "hug"))
    d.set(c, minHeight=104)
    b = d.instance("Tonal icon button", c, name="Button")
    d.cmd("instance.setProperties", ids=[b], properties={"Size": "Large", "Checked": "False", "State": "Default", "Container": "Fill"})
    _swap_icon(d, b, "Icon/sensors")
    d.cmd("component.exposeInstance", id=b)
    lb = k.text(c, "Start radio", "Component/Quick action", "Color/on", "Label", align="center", w=86)
    d.set(lb, maxLines=2)
    cc.finish_single("Quick action", c, "ComfortSheets.kt · QuickAction", "QuickAction",
                     "A quarter of the sheet's width: the 54dp tonal superellipse and its label under it (12.5/16 medium, two "
                     "lines at most, 86dp wide), 8 apart, 6 from the top, at least 104 tall. On a sheet the resting fill is "
                     "one step up: set the exposed Button to Container=Fill in light and Fill 2 in dark. Checked fills it with "
                     "the accent (the heart while the song is a favourite).",
                     binds=[("Label", "text", lb, "Start radio")])

    # -- Quick actions
    c = cc.single("Quick actions", "ComfortSheets.kt · ComfortTrackMenu", "ComfortTrackMenu", layout="v", w=411,
                  sizing=("fixed", "hug"))
    row = d.frame(c, name="Actions", layout="h", pad=[2, 12, 8, 12], sizing=("fill", "hug"), cross="min")
    for nm, lig, label in (("Radio", "sensors", "Start radio"), ("Favourite", "favorite", "Add to favourites"),
                           ("Play next", "playlist_play", "Play next"), ("Add to queue", "queue_music", "Add to queue")):
        q = d.instance("Quick action", row, name=nm)
        d.set(q, sizingH="fill")
        _set_text_prop(d, q, "Label", label)
        _swap_icon(d, d.child(q, "Button"), f"Icon/{lig}")
        d.cmd("component.exposeInstance", id=q)
    rule = d.frame(c, name="Rule", layout="h", pad=[0, 24, 0, 24], sizing=("fill", "hug"))
    d.rect(rule, name="Rule line", w=363, h=1, fill="Color/line", sizingH="fill")
    cc.finish_single("Quick actions", c, "ComfortSheets.kt · ComfortTrackMenu", "ComfortTrackMenu",
                     "The track menu's row of four: Start radio, Add to favourites (\"Remove from favourites\", checked, while "
                     "it is one), Play next, Add to queue. 12dp sides, 2 above, 8 below, then a rule in line across the "
                     "sheet's 24dp margins (`.quick + .sheet-row::before`). The four Quick action instances are exposed.")

    # -- Sheet row
    sb = SetBuilder(cc, "Sheet row", "Overlays.kt · SheetRow", "SheetRow",
                    "At least 52dp, 24dp sides, 18 between parts: an icon in on-2 (or its 24dp space), the label (16/22), a "
                    "value in on-2 (\"23 min left\"), and at the end chevron_right (opens a picker) or unfold_more (opens a "
                    "pop-over) in on-3. Selected: a semibold label and the tick in accent-ink. Danger: icon and label in "
                    "danger. Show hairline on rows after the first: from 66 to 24 from the end.")
    combos = [("Default", "None"), ("Default", "Chevron"), ("Default", "Pop-over"), ("Selected", "None"), ("Danger", "None")]
    defaults = {("Default", "None"): ("Go to album", "album"), ("Default", "Chevron"): ("Add to playlist", "playlist_add"),
                ("Default", "Pop-over"): ("Language: English", "language"), ("Selected", "None"): ("English", "language"),
                ("Danger", "None"): ("Remove from this playlist", "do_not_disturb_on")}
    for kind, end in combos:
        c = sb.variant({"Kind": kind, "End": end}, layout="h", w=411, pad=[0, 24, 0, 24], gap=18, cross="center",
                       sizing=("fixed", "hug"))
        d.set(c, minHeight=52)
        hl = _hairline(d, k, c, 66, 321)
        sb.bool("Show hairline", hl, False)
        label, lig = defaults[(kind, end)]
        box = d.frame(c, name="Icon space", w=24, h=24)
        ic = k.icon(box, lig, color="Color/danger" if kind == "Danger" else "Color/on-2", name="Icon")
        sb.bool("Show icon", ic, kind != "Selected")
        if kind == "Selected":
            d.set(ic, visible=False)
        sb.swap("Icon", ic, f"Icon/{lig}")
        t = k.text(c, label, "Component/Overlay row selected" if kind == "Selected" else "Component/Overlay row",
                   "Color/danger" if kind == "Danger" else "Color/on", "Label", fill=True, lines=1)
        sb.text("Label", t, label)
        v = k.text(c, "23 min left", "Component/Sheet value", "Color/on-2", "Value", lines=1)
        d.set(v, visible=False)
        sb.text("Value", v, "23 min left")
        sb.bool("Show value", v, False)
        if kind == "Selected":
            k.icon(c, "check", color="Color/accent-ink", name="Check")
        if end != "None":
            k.icon(c, "chevron_right" if end == "Chevron" else "unfold_more", color="Color/on-3", name="End")
    sb.finish([{"Kind": "Default", "End": "Chevron"}, {"Kind": "Default", "End": "Pop-over"}, {"Kind": "Selected", "End": "None"},
               {"Kind": "Danger", "End": "None"}])


def _sheet(d: Doc, k: K, cc: Components):
    c = cc.single("Sheet", "Overlays.kt · ComfortSheet", "ComfortSheet", layout="v", w=411, fill="Color/raised",
                  sizing=("fixed", "hug"), cross="center")
    d.set(c, cornerRadius=[30, 30, 0, 0], clip=True)
    d.cmd("style.apply", ids=[c], style=d.effect_styles["Shadow/Floating"])
    grab = d.frame(c, name="Grab", layout="h", pad=[12, 0, 12, 0], main="center", sizing=("fill", "hug"))
    h = d.rect(grab, name="Handle", w=40, h=4, fill="Color/fill-2")
    k.bind_radius(h, "pill")
    slot = d.frame(c, name="Content", layout="v", sizing=("fill", "hug"))
    hd = d.instance("Sheet header", slot, name="Sheet header")
    d.set(hd, sizingH="fill")
    for i, (label, lig, end) in enumerate((("Add to playlist", "playlist_add", "Chevron"), ("Language: English", "language", "Pop-over"),
                                           ("Go to album", "album", "None"))):
        r = d.instance("Sheet row", slot, name=f"Row: {label}")
        d.cmd("instance.setProperties", ids=[r], properties={"Kind": "Default", "End": end})
        d.set(r, sizingH="fill")
        _set_text_prop(d, r, "Label", label)
        _swap_icon(d, r, f"Icon/{lig}")
        if i:
            _set_text_prop_bool(d, r, "Show hairline", True)
    d.cmd("component.makeSlot", id=slot, name="Content",
          preferred=[d.comp[n] for n in ("Sheet header", "Sheet title", "Quick actions", "Sheet row", "Collection row", "Song row",
                                         "Search field")])
    d.frame(c, name="Foot", w=411, h=30, sizing=("fill", "fixed"))
    cc.finish_single("Sheet", c, "Overlays.kt · ComfortSheet", "ComfortSheet",
                     "Material's ModalBottomSheet in the kit's dress: raised, 30dp top corners, the 40 x 4 grab handle in fill-2 "
                     "(12 above and below), the scrim behind. It opens fully expanded and hugs its content; 30dp under the last "
                     "row, then navigationBarsPadding(): on a screen set the Foot layer to 30 + the navigation inset (54 over the "
                     "24dp gesture inset), the sheet's bottom on the screen's bottom edge. Put the parts in the Content slot (instance.slotInsert with "
                     "replace): Sheet header or Sheet title first, then Quick actions, Sheet rows, Collection rows or Song rows "
                     "(set their start padding to 24 in a sheet). Shadow/Floating without the edge (StaticSheet: edge = false).")


# ------------------------------------------------------------------------ menus
def _menus(d: Doc, k: K, cc: Components):
    c = cc.single("Menu caption", "Overlays.kt · MenuCaption", "MenuCaption", layout="h", w=200, pad=[10, 22, 6, 22],
                  sizing=("fixed", "hug"))
    t = k.text(c, "Sleep timer", "Component/Menu caption", "Color/on-2", "Caption text", fill=True, lines=1)
    cc.finish_single("Menu caption", c, "Overlays.kt · MenuCaption", "MenuCaption",
                     "A small caption at the top of a pop-over (\"Song language\", \"Sleep timer\"): 13/18 semibold on-2, 22dp "
                     "sides, 10 above, 6 below.", binds=[("Text", "text", t, "Sleep timer")])

    sb = SetBuilder(cc, "Menu row", "Overlays.kt · MenuRow", "MenuRow",
                    "50dp, 22dp sides, 14 between the icon (on-2) and the label (16/22). Selected (`.on`): accent-ink and "
                    "semibold, ending in the tick in a single-choice pop-over (Show check), which takes half the room the label leaves. Danger: icon and label in danger. "
                    "A row without an icon keeps no space for it.")
    for kind, label, lig in (("Default", "Rename", "edit"), ("Selected", "Off", "check"), ("Danger", "Delete playlist", "delete")):
        c = sb.variant({"Kind": kind}, layout="h", w=200, h=50, pad=[0, 22, 0, 22], gap=14, cross="center",
                       sizing=("fixed", "fixed"))
        ink = {"Default": "Color/on", "Selected": "Color/accent-ink", "Danger": "Color/danger"}[kind]
        ic = k.icon(c, lig, color="Color/on-2" if kind == "Default" else ink, name="Icon")
        sb.swap("Icon", ic, f"Icon/{lig}")
        sb.bool("Show icon", ic, kind != "Selected")
        if kind == "Selected":
            d.set(ic, visible=False)
        t = k.text(c, label, "Component/Overlay row selected" if kind == "Selected" else "Component/Overlay row", ink, "Label",
                   lines=1)
        sb.text("Label", t, label)
        # MenuRow: the label takes weight(1, fill = false) and the tick's Box weight(1): half of what the
        # row has left ((200 - 44) / 2 = 78 in a 200dp menu), right after the label, the tick at its end.
        ck = d.frame(c, name="Check", w=78, layout="h", pad=[0, 0, 0, 18], main="max", cross="center", sizing=("fixed", "hug"))
        k.icon(ck, "check", color=ink, name="Tick")
        sb.bool("Show check", ck, kind == "Selected")
        if kind != "Selected":
            d.set(ck, visible=False)
    sb.finish()

    sb = SetBuilder(cc, "Menu", "Overlays.kt · ComfortMenu", "ComfortMenu",
                    "Material's DropdownMenu: raised, radius 22, at least 200dp wide, 6dp above and below the rows, with "
                    "Shadow/Floating and the 1dp edge. It opens beside the control that asked. On sheet (`.menu.on-sheet`): "
                    "opened from a sheet's row it sits one step up, on fill, over the row, toward the row's end (x = sheet "
                    "width - 214). Fill the Content slot with a Menu caption and Menu rows.")
    for on_sheet in (False, True):
        c = sb.variant({"On sheet": str(on_sheet)}, layout="v", w=200, pad=[6, 0, 6, 0],
                       fill="Color/fill" if on_sheet else "Color/raised", stroke="Color/edge", stroke_w=1,
                       sizing=("fixed", "hug"))
        d.set(c, strokeAlign="inside", minWidth=200, clip=True)
        k.bind_radius(c, "menu")
        d.cmd("style.apply", ids=[c], style=d.effect_styles["Shadow/Floating"])
        slot = d.frame(c, name="Content", layout="v", sizing=("fill", "hug"))
        if on_sheet:
            cap = d.instance("Menu caption", slot, name="Caption")
            d.set(cap, sizingH="fill")
            _set_text_prop(d, cap, "Text", "Song language")
            for lang in ("Tamil", "Hindi", "English"):
                r = d.instance("Menu row", slot, name=f"Row: {lang}")
                d.cmd("instance.setProperties", ids=[r], properties={"Kind": "Selected" if lang == "English" else "Default"})
                d.set(r, sizingH="fill")
                _set_text_prop(d, r, "Label", lang)
                _set_text_prop_bool(d, r, "Show icon", False)
                _set_text_prop_bool(d, r, "Show check", lang == "English")
        else:
            for kind, label, lig in (("Default", "Rename", "edit"), ("Danger", "Delete playlist", "delete")):
                r = d.instance("Menu row", slot, name=f"Row: {label}")
                d.cmd("instance.setProperties", ids=[r], properties={"Kind": kind})
                d.set(r, sizingH="fill")
                _set_text_prop(d, r, "Label", label)
                _swap_icon(d, r, f"Icon/{lig}")
        d.cmd("component.makeSlot", id=slot, name="Content", preferred=[d.comp["Menu caption"], d.comp["Menu row"]])
    sb.finish()


# ------------------------------------------------------------------------ dialogs
def _dialogs(d: Doc, k: K, cc: Components):
    sb = SetBuilder(cc, "Dialog field", "Overlays.kt · DialogField", "DialogField",
                    "A label in accent-ink (12.5/16 medium) over the text (17/24), 4 apart, underlined 2dp in the accent; 18 "
                    "above, 9 between the text and the line. A prefilled name opens with the cursor (2dp, accent) after it.")
    for state in ("Empty", "Filled"):
        c = sb.variant({"State": state}, layout="v", w=339, pad=[18, 0, 9, 0], gap=4, sizing=("fixed", "hug"))
        ul = d.rect(c, name="Underline", w=339, h=2, fill="Color/accent")
        t = k.text(c, "Playlist name", "Component/Field label", "Color/accent-ink", "Label", fill=True)
        sb.text("Label", t, "Playlist name")
        line = d.frame(c, name="Text line", layout="h", h=24, cross="center", sizing=("fill", "fixed"))
        v = k.text(line, "Road trip" if state == "Filled" else "", "Component/Field", "Color/on", "Value", lines=1)
        if state == "Empty":
            d.set(v, visible=False)
        sb.text("Value", v, "Road trip" if state == "Filled" else "")
        caret = d.rect(line, name="Caret", w=2, h=22, fill="Color/accent")
        d.set(caret, visible=False)
        sb.bool("Show caret", caret, False)
        d.set(ul, absolute=True)  # out of the flow first, so the field's height is its own
        g = d.get(c)
        k.absolute(ul, 0, g["height"] - 2, 339, 2, {"horizontal": "leftRight", "vertical": "bottom"})
    sb.finish()

    sb = SetBuilder(cc, "Dialog", "Overlays.kt · ComfortDialog", "ComfortDialog",
                    "A bottom dialog (`.dialog`): raised, radius 30, Shadow/Floating without the edge, 24dp in at the sides and "
                    "top, 6 at the foot. The title (20/26 bold); a body (15/21 on-2, 8 below it) or the name field; then the "
                    "buttons 14 below: text buttons 52dp tall in accent-ink, split by a 1 x 18 rule in fill-2. One button "
                    "(Buttons=One) for a result to acknowledge. Disabled confirm is accent-ink at 38 % (waiting for a name); "
                    "Danger is the confirm in danger (\"Delete playlist\"). On a screen it floats 12dp in from the sides and "
                    "22dp above the navigation inset (46dp above the bottom edge over the 24dp gesture inset: "
                    "navigationBarsPadding()), over the scrim, and rides above the keyboard.")
    for content in ("Body", "Field"):
        for buttons in ("Two", "One"):
            for confirm in ("Default", "Disabled", "Danger"):
                c = sb.variant({"Content": content, "Buttons": buttons, "Confirm": confirm}, layout="v", w=387,
                               pad=[24, 24, 6, 24], fill="Color/raised", sizing=("fixed", "hug"))
                k.bind_radius(c, "float")
                d.cmd("style.apply", ids=[c], style=d.effect_styles["Shadow/Floating"])
                title = "Delete Long drive?" if confirm == "Danger" else ("New playlist" if content == "Field" else "Restore this backup?")
                t = k.text(c, title, "Scale/Bar title", "Color/on", "Title", fill=True)
                sb.text("Title", t, title)
                if content == "Body":
                    bf = d.frame(c, name="Body", layout="v", pad=[8, 0, 0, 0], sizing=("fill", "hug"))
                    body = ("The playlist goes away. The songs stay in your library." if confirm == "Danger"
                            else "Favourites, playlists, play history and lyrics from this backup are added to what is here.")
                    bt = k.text(bf, body, "Scale/Body", "Color/on-2", "Body text", fill=True)
                    sb.text("Body", bt, body)
                else:
                    f = d.instance("Dialog field", c, name="Field")
                    d.set(f, sizingH="fill")
                    d.cmd("component.exposeInstance", id=f)
                row = d.frame(c, name="Buttons", layout="h", pad=[14, 0, 0, 0], cross="center", sizing=("fill", "hug"))
                ink = "Color/danger" if confirm == "Danger" else "Color/accent-ink"
                if buttons == "Two":
                    db = d.frame(row, name="Dismiss", layout="h", h=52, main="center", cross="center", sizing=("fill", "fixed"))
                    k.bind_radius(db, "pill")
                    dt = k.text(db, "Cancel", "Scale/Button", "Color/accent-ink", "Dismiss label", lines=1, align="center")
                    sb.text("Dismiss label", dt, "Cancel")
                    d.rect(row, name="Divider", w=1, h=18, fill="Color/fill-2")
                cb = d.frame(row, name="Confirm", layout="h", h=52, main="center", cross="center", sizing=("fill", "fixed"))
                k.bind_radius(cb, "pill")
                clabel = {"Danger": "Delete playlist", "Disabled": "Create"}.get(confirm, "Restore" if content == "Body" else "Create")
                if buttons == "One":
                    clabel = "Close"
                ct = k.text(cb, clabel, "Scale/Button", ink, "Confirm label", lines=1, align="center")
                if confirm == "Disabled":
                    d.set(ct, opacity=0.38)
                sb.text("Confirm label", ct, clabel)
    sb.finish([{"Content": "Body", "Buttons": "Two", "Confirm": "Danger"}, {"Content": "Field", "Buttons": "Two", "Confirm": "Disabled"},
               {"Content": "Body", "Buttons": "One", "Confirm": "Default"}])


# ------------------------------------------------------------------------ toast and scrim
def _toast_scrim(d: Doc, k: K, cc: Components):
    c = cc.single("Toast", "Overlays.kt · Toast", "Toast", layout="h", pad=[12, 22, 12, 22], fill="Color/toast",
                  sizing=("hug", "hug"))
    d.set(c, maxWidth=330)
    k.bind_radius(c, "pill")
    d.cmd("style.apply", ids=[c], style=d.effect_styles["Shadow/Small"])
    t = k.text(c, "Added to queue", "Component/Toast", "Color/on-toast", "Message", align="center")
    cc.finish_single("Toast", c, "Overlays.kt · Toast", "Toast",
                     "Snackbars, shown as one line in a capsule with no action: 14.5/20 on-toast on toast, 22 x 12 padding, at "
                     "most 330dp wide, Shadow/Small. Centred above the dock (6dp over it); over Now Playing, the queue or "
                     "lyrics, 32dp above the gesture inset.",
                     binds=[("Text", "text", t, "Added to queue")])

    c = cc.single("Scrim", "Overlays.kt · ComfortSheet, ComfortDialog", "ComfortSheet", w=411, h=914, fill="Color/scrim")
    cc.finish_single("Scrim", c, "Overlays.kt · ComfortSheet, ComfortDialog", "ComfortSheet",
                     "The whole window dimmed in Color/scrim behind a sheet (ModalBottomSheet scrimColor) or a dialog (its own "
                     "background, the window's dim off). It covers the status bar too. Resize it with the screen. Not in the "
                     "Dark preview: the dark scrim (black at 62 %) on the dark ground shows nothing.",
                     preview=False)
