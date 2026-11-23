"""Screens: Settings (with Appearance as its first group), licences, backup and restore, Recap and its story
and share images, the first run, the permission screen, the first scan and the empty library.

Areas on the Screens page, in order: Settings, Recap, First run, Permission, loading and empty.

Owner: settings screens agent. Components come from components_core (rows, cells, bars, buttons), from
components_states (states, first-run parts, Recap cards and chrome, settings rows) and, for the restore
dialogs, the Dialog component in components_overlays. Prototype triggers are recorded in
ctx["triggers"][(src, dst)] for the finishing agent; ("settings", "settings_style_switch") points at the
style control. Drags, delays and the like go to ctx["links"] through link(); wire_shell() records Back, the mini
player and the navigation bar on the shell's screens.
"""
from __future__ import annotations

import os
import re
from pathlib import Path

from figbuild.kit import Doc, V
from expressive.components_core import inst, set_text, set_cover
from expressive.foundations import cover_path, paragraph_line_heights, dialog_y, RENDER_DIFFERS, DIALOG_INSET_NOTE
from expressive.components_states import (prop, set_props, swap, swap_icon, mark, scaled_card, fit_group, CARDS,
                                          COLOR_MODE, TOP_SONGS, TOP_ARTISTS)

KEYS = (["settings", "settings_full", "settings_analysis", "settings_lyrics_download", "settings_backup",
         "dialog_restore_confirm", "dialog_restore_result", "dialog_restore_problem", "licences", "licence_text",
         "recap", "recap_full"]
        + [f"recap_story_{k}" for _, _, k in CARDS] + [f"recap_share_{k}" for _, _, k in CARDS]
        + ["onboarding_welcome", "onboarding_look", "onboarding_access", "onboarding_access_granted",
           "onboarding_access_refused", "onboarding_library_scanning", "onboarding_library_found",
           "onboarding_library_empty", "permission", "permission_refused", "loading", "empty_library"])
REFS: dict[str, str] = {k: f"expressive-{{theme}}-{k}" for k in KEYS}

REPO = Path(__file__).resolve().parents[3]
H = 914


# ───────────────────────────────────────────────────────────────── shared screen parts
def system_bars(d: Doc, fid: int, h: float = H):
    sb = inst(d, "Status bar", fid, name="Status bar", x=0, y=0)
    gb = inst(d, "Gesture bar", fid, name="Gesture bar", x=0, y=h - 24)
    return sb, gb


def link(ctx, src, layer, dst, *, trigger="onClick", delay=None, how=None, label=""):
    """A prototype link the plain triggers can't express (a drag, a long press, a delay): flows.py wires
    ctx["links"] after ctx["triggers"]."""
    ctx.setdefault("links", []).append(dict(src=src, layer=layer, dst=dst, trigger=trigger, delay=delay, how=how,
                                            label=label))


def wire_shell(d: Doc, T: dict, key: str, fid: int, *, back=True, mini=True, nav=True):
    """Back in the app bar, the mini player and the navigation bar's items, as the browse screens wire them."""
    def first(name):
        ids = [i for i in d.find(fid, name=name, limit=50) if d.get(i).get("name") == name]
        return ids[0] if ids else None
    if back:
        bar = first("App bar")
        b = first("Back") if bar is None else next((i for i in d.find(bar, name="Back", limit=5)
                                                    if d.get(i).get("name") == "Back"), None)
        if b:
            T[(key, "back")] = b
    if mini:
        m = first("Mini player")
        if m:
            T[(key, "now_playing")] = m
    if nav:
        n = first("Navigation bar")
        if n:
            for dest, dst in (("Home", "home"), ("Search", "search_browse"), ("Library", "library_list")):
                item = next((i for i in d.find(n, name=f"Item {dest}", limit=3) if d.get(i).get("name") == f"Item {dest}"), None)
                if item:
                    T[(key, dst)] = item


def shell(d: Doc, fid: int, h: float = H, mini=True):
    """The shell's mini player and navigation bar over a detail screen, then the system bars on top."""
    if mini:
        inst(d, "Mini player", fid, name="Mini player", State="Playing", Title_length="Default", x=12, y=h - 88 - 8 - 64)
    inst(d, "Navigation bar", fid, name="Navigation bar", Selected="Home", x=0, y=h - 88)
    system_bars(d, fid, h)


def column(d: Doc, parent, name="Content", y=0, w=411, gap=0) -> int:
    return d.frame(parent, name=name, layout="v", w=w, x=0, y=y, gap=gap, sizing=("fixed", "hug"))


def section(d: Doc, ctx, parent, label: str, top=None) -> int:
    s = inst(d, "Settings section", parent, name=f"Section {label}")
    set_text(d, s, prop(ctx, "Settings section", "Label"), label)
    if top is not None:
        d.set(s, paddingTop=top)
    return s


def group(d: Doc, parent, name: str) -> int:
    return d.frame(parent, name=name, layout="v", gap=2, pad=[0, 16, 0, 16], w=411, sizing=("fixed", "hug"))


def cell(d: Doc, parent, position: str, row_comp: str, name=None, surface="Default", **variant) -> tuple[int, int]:
    """A List group/Cell holding an instance of `row_comp` (variant given as keywords). Returns (cell, row)."""
    c = inst(d, "List group/Cell", parent, name=name or f"Cell {position}", Position=position, Surface=surface)
    d.set(c, sizingH="fill")
    row = d.child(c, "Row")
    swap(d, row, row_comp)
    if variant:
        set_props(d, row, **{k: str(v) for k, v in variant.items()})
    d.set(row, sizingH="fill")
    if row_comp == "Connected button group":
        d.set(row, minHeight=None)  # the swap keeps the song row's 64dp minimum
    return c, row


def setting_row(d: Doc, ctx, parent, position, title, summary=None, icon=None, trailing="None", lines=None, name=None):
    lines = lines or ("Two" if summary else "One")
    c, r = cell(d, parent, position, "List row/Setting", name=name, Leading="Icon", Lines=lines, Trailing=trailing,
                Status_line="False")
    set_text(d, r, prop(ctx, "List row/Setting", "Title"), title)
    if summary:
        set_text(d, r, prop(ctx, "List row/Setting", "Summary"), summary)
    if icon:
        set_props(d, r, Icon=d.comp[f"Icon/{icon}"])
    return c, r


def large_title(d: Doc, parent, text: str, bottom=0) -> int:
    box = d.frame(parent, name="Large title", layout="v", pad=[8, 16, bottom, 16], w=411, sizing=("fixed", "hug"))
    d.text(box, text, style="Kit/Root title", color="Color/on-surface", name="Title", width=379, textAutoResize="height")
    return box


def detail_bar(d: Doc, ctx, fid: int, title: str | None) -> int:
    """DetailAppBar: Plain with no title until the large title scrolls away, then Scrolled with it."""
    b = inst(d, "Top app bar/Detail", fid, name="App bar", Surface="Scrolled" if title else "Plain", x=0, y=0)
    d.cmd("instance.setProperties", ids=[b], properties={prop(ctx, "Top app bar/Detail", "Show action"): False})
    t = d.find(b, name="Title", limit=1)[0]
    if title:
        set_text(d, b, prop(ctx, "Top app bar/Detail", "Title"), title)
    else:
        d.set(t, visible=False)
    return b


def annotate(d: Doc, key: str, source: str, *notes: str, render: tuple = ()):
    """Source, notes (category Content) and the ways the phone differs from the reference render."""
    s = d.screens[key]
    for fid in (s["light"], s["dark"]):
        if fid:
            d.annotate(fid, f"Compose: {source}", "Source")
            for n in notes:
                d.annotate(fid, n, "Content")
            for n in render:
                d.annotate(fid, n, RENDER_DIFFERS)
            d.ready_for_dev(fid)


def dark_layer(d: Doc, key: str, light_layer: int) -> int | None:
    return d.screens[key]["map"].get(light_layer)


# ───────────────────────────────────────────────────────────────── Settings
def settings_content(d: Doc, ctx, parent, y: float, *, lyrics="idle", analysis="done") -> tuple[int, dict]:
    col = column(d, parent, "Settings list", y=y)
    out = {}
    large_title(d, col, "Settings")
    section(d, ctx, col, "Appearance")
    g = group(d, col, "Appearance group")
    c, theme = cell(d, g, "Top", "Connected button group", name="Theme cell", Options="3", Selected="3", On_cell="True")
    d.set(c, paddingTop=12, paddingBottom=12)
    fit_group(d, theme, 379)
    out["theme"] = theme
    c, style = cell(d, g, "Bottom", "List row/Style picker", name="Style cell", Selected="Expressive")
    out["style"] = d.child(style, "Style group")
    out["style_cell"] = style

    section(d, ctx, col, "Lyrics")
    g = group(d, col, "Lyrics group")
    cell(d, g, "Top", "List row/Setting text", name="Saved lyrics", Lead="Icon")
    st = prop(ctx, "List row/Setting progress", "Status")
    if lyrics == "running":
        _, r = cell(d, g, "Bottom", "List row/Setting progress", name="Download lyrics", Kind="Lyrics running")
        set_text(d, r, st, "Checked 23 of 69, found 17")
    else:
        setting_row(d, ctx, g, "Bottom", "Download lyrics for all songs",
                    "Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are "
                    "never sent.", "download", name="Download lyrics")

    section(d, ctx, col, "Song radio")
    g = group(d, col, "Song radio group")
    setting_row(d, ctx, g, "Top", "Autoplay", "When a queue reaches its last song, radio keeps going with similar songs.",
                "autoplay", trailing="Switch on", name="Autoplay")
    _, r = cell(d, g, "Bottom", "List row/Setting progress", name="Song analysis",
                Kind="Analysis running" if analysis == "running" else "Analysis done")
    set_text(d, r, st, "Analysing songs: 41 of 69" if analysis == "running" else "All 69 songs analysed.")

    out["backup_section"] = section(d, ctx, col, "Backup")
    g = group(d, col, "Backup group")
    out["backup_group"] = g
    out["backup"], _ = setting_row(d, ctx, g, "Top", "Back up to a file",
                                   "Saves favourites, playlists, listening history, saved lyrics and settings in one file. "
                                   "Keep it somewhere other than this phone.", "file_save", name="Back up")
    out["restore"], _ = setting_row(d, ctx, g, "Middle", "Restore from a file",
                                    "Adds a backup to what is on this phone. Nothing here is deleted.", "file_open",
                                    name="Restore")
    cell(d, g, "Bottom", "List row/Setting note", name="Backup note")

    section(d, ctx, col, "About")
    g = group(d, col, "About group")
    _, about = cell(d, g, "Top", "List row/Setting text", name="About Attic", Lead="Mark")
    set_text(d, about, prop(ctx, "List row/Setting text", "Text"),
             "Attic plays your own music files, from any folder. It has no account and no streaming. 69 songs on this phone.")
    out["licences"], _ = setting_row(d, ctx, g, "Bottom", "Open-source licences", icon="info", name="Licences")
    out["radio_section"] = d.find(col, name="Section Song radio", limit=1)[0]
    return col, out


def offset_to(d: Doc, col: int, layer: int, screen_y: float):
    """Scroll `col` so that `layer` (inside it) starts at `screen_y` on the screen."""
    paragraph_line_heights(d, roots=[col])
    cb, lb = d.get(col)["absoluteBounds"], d.get(layer)["absoluteBounds"]
    d.set(col, y=screen_y - (lb["y"] - cb["y"]))


def build_settings(d: Doc, ctx, T):
    a = d.area("Settings", note="Settings is one scrolling list: Appearance (theme and style) first, then Lyrics, Song radio, "
                                "Backup and About. The large title collapses into the bar once it scrolls away. The "
                                "restore dialogs open over the Backup section; licences open from About.")
    src = "ui/expressive/ExpressiveSettings.kt · ExpressiveSettings"
    made = {}

    def settings_screen(key, name, *, h=H, scroll_to=None, **content):
        fid = a.screen(key, name=name, source=src, h=h)
        col, out = settings_content(d, ctx, fid, 104, **content)
        if scroll_to:
            layer, at = scroll_to(out)
            offset_to(d, col, layer, at)
        detail_bar(d, ctx, fid, "Settings" if scroll_to else None)
        shell(d, fid, h)
        wire_shell(d, T, key, fid)
        made[key] = (fid, col, out)
        return fid, col, out

    fid, col, out = settings_screen("settings", "Settings / Top")
    T[("settings", "settings_style_switch")] = out["style"]
    d.set(out["style"], name="Style switch")
    # Backup is below the fold here: dragging the list (a scroll) goes to the list scrolled to Backup.
    link(ctx, "settings", col, "settings_backup", trigger="onDrag", how="fade", label="Scroll to Backup")
    fid, col, out = settings_screen("settings_full", "Settings / Whole list (full length)", h=1740)
    T[("settings_full", "licences")] = out["licences"]
    T[("settings_full", "settings_backup")] = out["backup_group"]
    T[("settings_full", "settings_style_switch")] = out["style"]
    settings_screen("settings_analysis", "Settings / Analysing songs", analysis="running",
                    scroll_to=lambda o: (o["backup_section"], 181))
    settings_screen("settings_lyrics_download", "Settings / Downloading lyrics", lyrics="running",
                    scroll_to=lambda o: (o["radio_section"], 322))
    fid, col, out = settings_screen("settings_backup", "Settings / Backup", scroll_to=lambda o: (o["backup_section"], 181))
    T[("settings_backup", "dialog_restore_confirm")] = out["restore"]
    T[("settings_backup", "licences")] = out["licences"]
    T[("settings_backup", "handoff_file_picker")] = out["backup"]
    T[("settings_full", "handoff_file_picker")] = made["settings_full"][2]["backup"]
    T[("settings_full", "dialog_restore_confirm")] = made["settings_full"][2]["restore"]

    # "Backup saved", after the system picker saves the file (BackupController.kt).
    fid = a.screen("snackbar_backup_saved", name="Settings / Backup saved", source="ui/expressive/Surfaces.kt · ExpressiveSnackbar")
    col, out = settings_content(d, ctx, fid, 104)
    offset_to(d, col, out["backup_section"], 181)
    detail_bar(d, ctx, fid, "Settings")
    shell(d, fid)
    wire_shell(d, T, "snackbar_backup_saved", fid)
    sn = inst(d, "Snackbar", fid, name="Snackbar", x=28, y=678)
    set_text(d, sn, prop(ctx, "Snackbar", "Message"), "Backup saved")
    T[("snackbar_backup_saved", "dialog_restore_confirm")] = out["restore"]

    # Restore dialogs over the Backup section (need the Dialog component from components_overlays).
    dialogs = "Dialog" in d.comp
    if dialogs:
        build_restore_dialogs(d, ctx, a, T, src)

    # Licences ------------------------------------------------------------------------------------
    lsrc = "ui/expressive/ExpressiveLicences.kt · ExpressiveLicences"
    fid = a.screen("licences", name="Licences / List", source=lsrc)
    col = column(d, fid, "Licences list", y=104)
    large_title(d, col, "Open-source licences", bottom=24)
    g = group(d, col, "Notices")
    notices = [("Attic", "GNU General Public License v3.0"), ("Discogs-EffNet model", "Creative Commons BY-NC-SA 4.0"),
               ("ONNX Runtime", "MIT License"), ("Google Sans Flex", "SIL Open Font License 1.1"),
               ("Onest", "SIL Open Font License 1.1"), ("Material Symbols", "Apache License 2.0"),
               ("Jetpack, Kotlin and other libraries", "Apache License 2.0")]
    for n, (title, lic) in enumerate(notices):
        pos = "Top" if n == 0 else ("Bottom" if n == len(notices) - 1 else "Middle")
        c, r = cell(d, g, pos, "List row/Setting", name=f"Notice {title}", Leading="None", Lines="Two", Trailing="None",
                    Status_line="False")
        set_text(d, r, prop(ctx, "List row/Setting", "Title"), title)
        set_text(d, r, prop(ctx, "List row/Setting", "Summary"), lic)
        if n == 0:
            T[("licences", "licence_text")] = c
    detail_bar(d, ctx, fid, None)
    shell(d, fid)
    wire_shell(d, T, "licences", fid)

    fid = a.screen("licence_text", name="Licences / Attic's licence", source="ui/expressive/ExpressiveLicences.kt · ExpressiveLicence")
    col = column(d, fid, "Licence text", y=104)
    large_title(d, col, "Attic", bottom=24)
    for n, p in enumerate(licence_paragraphs()[:12]):
        box = d.frame(col, name=f"Paragraph {n + 1}", layout="v", pad=[6, 16, 6, 16], w=411, sizing=("fixed", "hug"))
        d.text(box, p, style="Body/Medium", color="Color/on-surface-variant", name="Paragraph", width=379,
               textAutoResize="height")
    detail_bar(d, ctx, fid, None)
    shell(d, fid)
    wire_shell(d, T, "licence_text", fid)

    a.close()
    # The renders draw each theme with its own Theme choice selected: Dark copies select Dark.
    for key in ("settings", "settings_full"):
        dl = dark_layer(d, key, made[key][2]["theme"])
        if dl:
            set_props(d, dl, Selected="2")
    for key in ("settings", "settings_full", "settings_analysis", "settings_lyrics_download", "settings_backup"):
        annotate(d, key, src)
    annotate(d, "snackbar_backup_saved", "ui/expressive/Surfaces.kt · ExpressiveSnackbar",
             "After the system file picker saves the file, BackupController.kt shows backup_saved (or "
             "backup_save_failed, \"Couldn't save the backup\") above the mini player.")
    for key in ("settings", "settings_full"):
        for fid in (d.screens[key]["light"], d.screens[key]["dark"]):
            d.annotate(fid, "The mock heads the theme cell with a \"Theme\" row. That label would be new copy, so the code "
                            "leaves the three-way group alone in its cell (ThemeCell).", "Code differs from mock")
    annotate(d, "licences", lsrc)
    annotate(d, "licence_text", "ui/expressive/ExpressiveLicences.kt · ExpressiveLicence",
             "The renders show Attic's own licence (GPL v3); the other notices use the same screen.")
    if dialogs:
        for key in ("dialog_restore_confirm", "dialog_restore_result", "dialog_restore_problem"):
            annotate(d, key, "ui/expressive/ExpressiveSettings.kt · BackupDialogs", render=(DIALOG_INSET_NOTE,))


def licence_paragraphs() -> list[str]:
    """LicencesModel.licenceParagraphs over the repo's LICENSE (assets/licences/LICENSE-attic.txt)."""
    text = (REPO / "LICENSE").read_text().replace("\r\n", "\n")
    out = []
    for b in re.split(r"\n\s*\n", text):
        lines = [l.rstrip() for l in b.strip("\n").split("\n")]
        if not any(l.strip() for l in lines):
            continue
        laid = any(l.startswith((" ", "\t")) for l in lines[1:]) or any(l.lstrip().startswith(("- ", "* ")) for l in lines)
        out.append("\n".join(lines) if laid else " ".join(l.strip() for l in lines))
    return out


def build_restore_dialogs(d: Doc, ctx, a, T, src):
    """The restore question, its result and the problem dialog, each over Settings scrolled to Backup."""
    bp = ctx["components"]["Button"]["props"]["Label"]
    dp = ctx["components"]["Dialog"]["props"]
    specs = [
        ("dialog_restore_confirm", "Settings / Restore, confirm", "Two", 411, "Restore this backup?",
         "Made November 2, 2026, with 42 favourites, 3 playlists, 4,870 plays and lyrics for 51 songs. They are added to "
         "what is on this phone, and nothing here is deleted. Theme, style and autoplay change to the backup's.",
         ("Cancel", "Restore")),
        ("dialog_restore_result", "Settings / Restore, done", "One", 411, "Backup restored",
         "Added 38 favourites, 2 playlists, 4,312 plays and lyrics for 47 songs. Songs were also added to 1 playlist "
         "that already had the same name. 64 of 69 songs in the backup are on this phone. The rest keep their place in "
         "your favourites, playlists and history, and show up when their files are added to this phone.",
         (None, "Close")),
        ("dialog_restore_problem", "Settings / Restore, not a backup", "One", 280, "Couldn't restore",
         "This file isn't an Attic backup.", (None, "Close")),
    ]
    for key, name, buttons, width, title, text, (dismiss, confirm) in specs:
        fid = a.screen(key, name=name, source="ui/expressive/ExpressiveSettings.kt · BackupDialogs")
        col, out = settings_content(d, ctx, fid, 104)
        offset_to(d, col, out["backup_section"], 181)
        detail_bar(d, ctx, fid, "Settings")
        shell(d, fid)
        inst(d, "Scrim", fid, name="Scrim", Kind="Dialog", x=0, y=0)
        dg = inst(d, "Dialog", fid, name="Dialog", Content="Message", Buttons=buttons, Confirm="Default", x=0, y=0)
        d.set(dg, width=width)
        set_props(d, dg, **{dp["Title"]: title, dp["Body"]: text})
        cf = d.child(dg, "Confirm")
        set_text(d, cf, bp, confirm)
        if dismiss:
            set_text(d, d.child(dg, "Dismiss"), bp, dismiss)
        paragraph_line_heights(d, roots=[dg])
        g = d.get(dg)
        d.set(dg, x=round((411 - width) / 2, 2), y=dialog_y(g["height"]))
        if key == "dialog_restore_confirm":
            T[("dialog_restore_confirm", "dialog_restore_result")] = cf
            T[("dialog_restore_confirm", "settings_backup")] = d.child(dg, "Dismiss")
        else:
            T[(key, "settings_backup")] = cf


# ───────────────────────────────────────────────────────────────── Recap
def recap_content(d: Doc, ctx, parent, T, key) -> int:
    col = column(d, parent, "Recap page", y=0)
    d.frame(col, name="Bar space", w=411, h=104)
    large_title(d, col, "Recap", bottom=10)
    d.set(d.find(col, name="Large title", limit=1)[0], paddingTop=0)
    g = inst(d, "Connected button group", col, name="Period", Options="4", Selected="3", On_cell="False")
    seg_label = prop(ctx, "Connected button group/Segment", "Label")
    unit = (379 - 6) / (3 + 1.3)
    for n, lab in enumerate(("This week", "This month", "This year", "All time")):
        s = d.child(g, f"Segment {n + 1}")
        set_text(d, s, seg_label, lab)
        if n == 2:
            # selectedWeight 1.3: the selected segment at a fixed 1.3 units, the other fr columns share the rest.
            d.set(s, sizingH="fixed", width=round(unit * 1.3, 2))
    hb = d.frame(col, name="Headline box", layout="v", pad=[12, 16, 0, 16], w=411, sizing=("fixed", "hug"))
    hero = inst(d, "Recap/Hero", hb, name="Headline")
    T[("recap", "recap_story_intro")] = d.child(hero, "Play recap") if key == "recap" else T.get(("recap", "recap_story_intro"))
    if key == "recap_full":
        T[("recap_full", "recap_story_intro")] = d.child(hero, "Play recap")
    sb = d.frame(col, name="Stats box", layout="v", pad=[8, 16, 0, 16], w=411, sizing=("fixed", "hug"))
    inst(d, "Recap/Stats", sb, name="Stats")

    def header(title, trailing=False):
        hdr = inst(d, "Section header", col, name=f"Header {title}", Overline="False", Trailing=str(trailing))
        set_text(d, hdr, prop(ctx, "Section header", "Title"), title)
        return hdr

    header("Share a card")
    strip = d.frame(col, name="Cards strip", w=411, h=round(118 * 16 / 9, 2), clip=True)
    row = d.frame(strip, name="Cards", layout="h", gap=8, pad=[0, 16, 0, 16], x=0, y=0, sizing=("hug", "hug"))
    for n, (card, _, k) in enumerate(CARDS):
        th = scaled_card(d, row, card, 118, radius=round(28 * 118 / 360, 2), name=f"Card {card}")
        if key == "recap_full":
            T[("recap_full", f"recap_story_{k}")] = th
    hdr = header("Top songs", trailing=True)
    gs = group(d, col, "Top songs")
    for n, (title, artist, ci, plays) in enumerate(TOP_SONGS):
        pos = "Top" if n == 0 else ("Bottom" if n == len(TOP_SONGS) - 1 else "Middle")
        c, r = cell(d, gs, pos, "List row/Song", name=f"Song {n + 1}", Lead="Ranked cover",
                    State="Current playing" if n == 0 else "Default")
        set_text(d, r, prop(ctx, "List row/Song", "Title"), title)
        set_text(d, r, prop(ctx, "List row/Song", "Subtitle"), artist)
        rk = d.child(r, "Rank")
        d.set(rk, characters=str(n + 1))
        if n + 1 >= 10:
            d.set(rk, width=22)
        set_cover(d, r, ci)
        tr = d.child(r, "Trailing")
        swap(d, tr, "List row/Count")
        set_text(d, tr, prop(ctx, "List row/Count", "Count"), f"{plays} plays")
    header("Top artists")
    ga = group(d, col, "Top artists")
    for n, (name, ci, plays) in enumerate(TOP_ARTISTS):
        pos = "Top" if n == 0 else ("Bottom" if n == len(TOP_ARTISTS) - 1 else "Middle")
        c, r = cell(d, ga, pos, "List row/Song", name=f"Artist {n + 1}", Lead="Ranked artist", State="Default")
        set_text(d, r, prop(ctx, "List row/Song", "Title"), name)
        set_props(d, r, **{prop(ctx, "List row/Song", "Show subtitle"): False})
        d.set(d.child(r, "Rank"), characters=str(n + 1))
        lead = d.child(r, "Lead")
        for pic in d.find(lead, name="Picture", limit=2):
            d.cmd("image.replace", ids=[pic], path=cover_path(ci))
        tr = d.child(r, "Trailing")
        swap(d, tr, "List row/Count")
        set_text(d, tr, prop(ctx, "List row/Count", "Count"), f"{plays * 225 // 60:,} min")
    header("Languages")
    lb = d.frame(col, name="Languages box", layout="v", pad=[0, 16, 0, 16], w=411, sizing=("fixed", "hug"))
    inst(d, "Recap/Languages", lb, name="Languages")
    header("When you listen")
    wb = d.frame(col, name="Clock box", layout="v", pad=[0, 16, 0, 16], w=411, sizing=("fixed", "hug"))
    inst(d, "Recap/When you listen", wb, name="When you listen")
    fb = d.frame(col, name="Footnote", layout="v", pad=[20, 32, 0, 32], w=411, sizing=("fixed", "hug"))
    d.text(fb, "Counted from Attic's listening log, which began on 1 January 2025. A song counts as a play after 30 seconds.",
           style="Body/Small", color="Color/on-surface-variant", name="Footnote text", width=347, textAutoResize="height")
    return col


def build_recap(d: Doc, ctx, T):
    a = d.area("Recap", note="Recap's page follows the wallpaper scheme; its cards keep their cover's dark scheme in both "
                             "themes. The story takes the scheme of the card on show (light or dark), with the next card "
                             "as a sliver beside it. Share images are the card itself at 1080 x 1920, drawn here at "
                             "411 x 731.")
    src = "ui/expressive/ExpressiveRecap.kt · ExpressiveRecap"
    for key, name, h in (("recap", "Recap / Top", H), ("recap_full", "Recap / Whole page (full length)", 3083)):
        fid = a.screen(key, name=name, source=src, h=h)
        recap_content(d, ctx, fid, T, key)
        detail_bar(d, ctx, fid, None)
        shell(d, fid, h)
        wire_shell(d, T, key, fid)

    # The period while its numbers load, and a period with no plays yet (ExpressiveRecap: loading, isEmpty).
    for key, name in (("recap_loading", "Recap / Loading"), ("recap_empty", "Recap / Nothing played this year")):
        fid = a.screen(key, name=name, source=src)
        col = column(d, fid, "Recap page", y=0)
        d.frame(col, name="Bar space", w=411, h=104)
        large_title(d, col, "Recap", bottom=10)
        d.set(d.find(col, name="Large title", limit=1)[0], paddingTop=0)
        g = inst(d, "Connected button group", col, name="Period", Options="4", Selected="3", On_cell="False")
        seg_label = prop(ctx, "Connected button group/Segment", "Label")
        unit = (379 - 6) / (3 + 1.3)
        for n, lab in enumerate(("This week", "This month", "This year", "All time")):
            sg = d.child(g, f"Segment {n + 1}")
            set_text(d, sg, seg_label, lab)
            if n == 2:
                d.set(sg, sizingH="fixed", width=round(unit * 1.3, 2))
        if key == "recap_loading":
            box = d.frame(col, name="Loading box", layout="v", w=411, h=300, main="center", cross="center")
            inst(d, "Loading indicator", box, name="Loading indicator", Contained="False")
        else:
            e = inst(d, "Empty state", col, name="Empty state", Art="Rounded")
            set_text(d, e, prop(ctx, "Empty state", "Title"), "Nothing played this year yet")
            set_text(d, e, prop(ctx, "Empty state", "Body"),
                     "Any song you play for 30 seconds or more counts. Your top songs, artists and listening clock "
                     "build up from there.")
            set_props(d, e, **{prop(ctx, "Empty state", "Icon"): d.comp["Icon/calendar_month"]})
        detail_bar(d, ctx, fid, None)
        shell(d, fid)
        wire_shell(d, T, key, fid)

    story_src = "ui/expressive/ExpressiveRecapStory.kt · ExpressiveRecapStory"
    for n, (card, ci, k) in enumerate(CARDS):
        key = f"recap_story_{k}"
        fid = a.screen(key, name=f"Recap / Story, {card.lower()}", source=story_src, fill="Color/recap/story-ground")
        d.set_mode(fid, "Color", COLOR_MODE[ci])
        bar = inst(d, "Recap/Story bar", fid, name="Story bar", x=0, y=40)
        set_text(d, bar, prop(ctx, "Recap/Story bar", "Position"), f"{n + 1} of 10")
        cw = 323
        box = scaled_card(d, fid, card, cw, x=20, y=118.9, radius=round(28 * cw / 360, 2), name="Card")
        if n + 1 < len(CARDS):
            nxt = CARDS[n + 1]
            sl = d.frame(fid, name="Next card", w=40, h=round(cw * 16 / 9, 2), x=20 + cw + 8, y=118.9, clip=True, radius=20)
            inner = scaled_card(d, sl, nxt[0], cw, x=0, y=0, name="Next card picture")
            # The sliver shows the next card in its own colours.
            T[(key, f"recap_story_{nxt[2]}")] = box
        if n > 0:
            # A tap on the card's left third goes back a card (ExpressiveRecapStory: offset.x < size.width / 3).
            prev = d.frame(fid, name="Tap area · previous card", w=round(cw / 3, 2), h=round(cw * 16 / 9, 2), x=20, y=118.9)
            T[(key, f"recap_story_{CARDS[n - 1][2]}")] = prev
        inst(d, "Recap/Story progress", fid, name="Story progress", Card=str(n + 1), x=24, y=710)
        acts = inst(d, "Recap/Story actions", fid, name="Story actions", x=24, y=762)
        system_bars(d, fid)
        T[(key, "recap")] = d.child(bar, "Close")
        T[(key, f"recap_share_{k}")] = [d.child(acts, "Save"), d.child(acts, "Share")]
    for n, (card, ci, k) in enumerate(CARDS):
        key = f"recap_share_{k}"
        fid = a.screen(key, name=f"Recap / Share image, {card.lower()}", source="ui/expressive/ExpressiveRecapCards.kt · ExpressiveRecapCard",
                       h=731)
        img = scaled_card(d, fid, card, 411, x=0, y=0, name="Share image")
        T[(key, "back")] = img
    a.close()
    annotate(d, "recap", src)
    annotate(d, "recap_full", src)
    annotate(d, "recap_loading", src, "While a period's numbers load the page shows the plain loading indicator in a "
                                      "300dp box under the period buttons.")
    annotate(d, "recap_empty", src, "A period with no plays yet: recap_empty_week, _month, _year or _all_time over "
                                    "recap_empty_body, with the calendar glyph (EmptyState).")
    for card, ci, k in CARDS:
        annotate(d, f"recap_story_{k}", story_src,
                 "The story's line is caught two thirds of the way through each card, as the renders show it.",
                 render=("The reference render gives the story's window no insets, so there it has no status bar and "
                         "starts at the screen's top. On a phone the window draws behind the system bars and the column "
                         "pads under the status bar and above the navigation bar (statusBarsPadding, "
                         "navigationBarsPadding), as drawn here: the bar sits 40dp lower and the card about 8dp lower "
                         "than in the render.",))
        annotate(d, f"recap_share_{k}", "ui/expressive/ExpressiveRecapCards.kt · ExpressiveRecapCard",
                 "Save and Share write this image at 1080 x 1920 (ExpressiveRecapStory capture); 411 x 731 here. Save "
                 "puts it in Pictures/Attic and shows \"Saved to Pictures/Attic\"; Share hands it to Android's share "
                 "sheet. In the prototype both buttons open this frame, and a tap on it goes back to the story.")


# ───────────────────────────────────────────────────────────────── First run
def first_run_screen(d: Doc, ctx, a, key, name, step, button, *, trailing=None):
    fid = a.screen(key, name=name, source="ui/expressive/ExpressiveOnboarding.kt · ExpressiveOnboarding")
    fb = inst(d, "Top app bar/First run", fid, name="First run bar", Step=str(step), x=0, y=40)
    if step > 1:
        ctx.setdefault("triggers", {})[(key, "back")] = d.child(fb, "Back")
    col = column(d, fid, "Step", y=104)
    bb = d.frame(fid, name="Bottom button", layout="h", pad=[8, 16, 24, 16], w=411, x=0, y=914 - 24 - 24 - 56 - 8,
                 sizing=("fixed", "hug"))
    b = inst(d, "Button", bb, name=button, Style="Filled", Size="Medium", Icon="Trailing" if trailing else "None",
             State="Enabled", Squared="False")
    set_text(d, b, prop(ctx, "Button", "Label"), button)
    if trailing:
        set_props(d, b, **{"Trailing icon": d.comp[f"Icon/{trailing}"]})
    d.set(b, sizingH="fill")
    system_bars(d, fid)
    return fid, col, b


def off_cell(d: Doc, g: int):
    """A group off a cell: unselected segments on surface container high."""
    set_props(d, g, On_cell="False")


def headline(d: Doc, parent, text, top=8):
    box = d.frame(parent, name="Headline", layout="v", pad=[top, 24, 0, 24], w=411, sizing=("fixed", "hug"))
    d.text(box, text, style="Kit/Player title", color="Color/on-surface", name="Headline", width=363, textAutoResize="height")
    return box


def body(d: Doc, parent, text, top=14):
    box = d.frame(parent, name="Body", layout="v", pad=[top, 24, 0, 24], w=411, sizing=("fixed", "hug"))
    d.text(box, text, style="Body/Large", color="Color/on-surface-variant", name="Body", width=363, textAutoResize="height")
    return box


def access_group(d: Doc, ctx, col, audio, notes):
    g = group(d, col, "Access")
    p = lambda k: prop(ctx, "First run/Access row", k)
    for pos, title, bodytext, lig, status in (
            ("Top", "Music and audio", "Required. Attic needs it to find the songs on this phone.", "music_note", audio),
            ("Bottom", "Notifications", "Optional. Shows the playback controls in the notification shade and on the lock screen.",
             "notifications", notes)):
        c, r = cell(d, g, pos, "First run/Access row", name=title, State=status)
        set_text(d, r, p("Title"), title)
        set_text(d, r, p("Body"), bodytext)
        set_props(d, r, **{p("Icon"): d.comp[f"Icon/{lig}"]})
    return g


def build_first_run(d: Doc, ctx, T):
    a = d.area("First run", note="Four steps under a bar with back and the step pips: welcome, the look (style and theme), "
                                 "access (music and notifications) and the library. The one filled button sits at the "
                                 "bottom edge. Steps slide along the X axis.")
    fid, col, b = first_run_screen(d, ctx, a, "onboarding_welcome", "First run / Welcome", 1, "Get started")
    pic = d.frame(col, name="Picture box", layout="v", pad=[0, 16, 0, 16], w=411, sizing=("fixed", "hug"))
    inst(d, "Shapes card", pic, name="Welcome picture", Kind="Welcome")
    headline(d, col, "Welcome to Attic", top=32)
    body(d, col, "Attic plays the music files on this phone. There's no account, and everything works offline.")
    T[("onboarding_welcome", "onboarding_look")] = b

    fid, col, b = first_run_screen(d, ctx, a, "onboarding_look", "First run / Look", 2, "Continue")
    headline(d, col, "Pick your look")
    body(d, col, "You can change it any time in Settings.")
    row = d.frame(col, name="Styles", layout="h", gap=12, pad=[24, 16, 0, 16], w=411, sizing=("fixed", "hug"))
    inst(d, "First run/Look card", row, name="Expressive", Style="Expressive", Selected="True")
    inst(d, "First run/Look card", row, name="Comfort", Style="Comfort", Selected="False")
    section(d, ctx, col, "Theme")
    look_theme = inst(d, "Connected button group", col, name="Theme", Options="3", Selected="3", On_cell="True")
    off_cell(d, look_theme)
    T[("onboarding_look", "onboarding_access")] = b

    for key, name, audio, notes, btn, trailing in (
            ("onboarding_access", "First run / Access", "None", "Allow", "Allow access", None),
            ("onboarding_access_granted", "First run / Access, allowed", "Allowed", "Allowed", "Continue", None),
            ("onboarding_access_refused", "First run / Access, refused", "Off required", "Allow", "Open app permissions",
             "open_in_new")):
        fid, col, b = first_run_screen(d, ctx, a, key, name, 3, btn, trailing=trailing)
        headline(d, col, "Let Attic find your music")
        d.frame(col, name="Gap", w=411, h=24)
        access_group(d, ctx, col, audio, notes)
        if key == "onboarding_access_refused":
            body(d, col, "Audio access is off, so Attic can't see your songs. Turn on Music and audio in the app's "
                         "permissions.", top=24)
        if key == "onboarding_access":
            T[("onboarding_access", "onboarding_access_granted")] = b
        if key == "onboarding_access_granted":
            T[("onboarding_access_granted", "onboarding_library_scanning")] = b
        if key == "onboarding_access_refused":
            T[("onboarding_access_refused", "handoff_app_permissions")] = b

    fid, col, b = first_run_screen(d, ctx, a, "onboarding_library_scanning", "First run / Library, scanning", 4, "Start listening")
    inst(d, "First run/Scanning", col, name="Scanning")
    T[("onboarding_library_scanning", "onboarding_library_found")] = b

    fid, col, b = first_run_screen(d, ctx, a, "onboarding_library_found", "First run / Library, found", 4, "Start listening")
    headline(d, col, "Found 69 songs")
    section(d, ctx, col, "Optional", top=32)
    g = group(d, col, "Optional")
    cell(d, g, "Top", "First run/Lyrics row", name="Lyrics", Checked="False")
    cell(d, g, "Bottom", "First run/Radio note", name="Radio note")
    T[("onboarding_library_found", "home")] = b

    fid, col, b = first_run_screen(d, ctx, a, "onboarding_library_empty", "First run / Library, empty", 4, "Start listening")
    e = inst(d, "Empty state", col, name="Empty state", Art="None")
    set_text(d, e, prop(ctx, "Empty state", "Title"), "No songs on this phone")
    hint = d.frame(col, name="Hint", layout="v", pad=[0, 32, 0, 32], w=411, sizing=("fixed", "hug"))
    d.text(hint, "Connect the phone to a computer and copy music into the Music folder. Songs show up here as they arrive.",
           style="Body/Medium", color="Color/on-surface-variant", name="Hint", width=347, textAutoResize="height")
    a.close()
    dl = dark_layer(d, "onboarding_look", look_theme)
    if dl:
        set_props(d, dl, Selected="2", On_cell="False")
    for key in ("onboarding_welcome", "onboarding_look", "onboarding_access", "onboarding_access_granted",
                "onboarding_access_refused", "onboarding_library_scanning", "onboarding_library_found",
                "onboarding_library_empty"):
        annotate(d, key, "ui/expressive/ExpressiveOnboarding.kt · ExpressiveOnboarding")


# ───────────────────────────────────────────────────────────────── Permission, loading, empty
def build_states(d: Doc, ctx, T):
    a = d.area("Permission, loading and empty", note="Before the library exists: the audio permission (first ask and "
                                                     "after it was refused), the first scan, and a phone with no music.")
    psrc = "ui/expressive/ExpressiveStates.kt · ExpressivePermission"
    for key, name, refused in (("permission", "Permission / Ask", False), ("permission_refused", "Permission / Refused", True)):
        fid = a.screen(key, name=name, source=psrc)
        col = d.frame(fid, name="Permission", layout="v", pad=[4, 16, 0, 16], w=411, x=0, y=40, sizing=("fixed", "hug"))
        inst(d, "Shapes card", col, name="Picture", Kind="Refused" if refused else "Permission")
        t = d.frame(col, name="Title box", layout="v", pad=[32, 8, 0, 8], sizing=("fill", "hug"))
        d.text(t, "Attic plays the music on this phone", style="Kit/Player title", color="Color/on-surface", name="Title",
               width=363, textAutoResize="height")
        bx = d.frame(col, name="Body box", layout="v", pad=[14, 8, 0, 8], sizing=("fill", "hug"))
        d.text(bx, "Audio access is off, so Attic can't see your songs. Turn on Music and audio in the app's permissions."
               if refused else "Allow audio access so it can find your songs in any folder. Notifications show the "
                               "playback controls.", style="Body/Large", color="Color/on-surface-variant", name="Body",
               width=363, textAutoResize="height")
        bb = d.frame(fid, name="Bottom button", layout="h", pad=[0, 16, 0, 16], w=411, x=0, y=914 - 24 - 24 - 56,
                     sizing=("fixed", "hug"))
        label = "Open app permissions" if refused else "Allow access"
        b = inst(d, "Button", bb, name=label, Style="Filled", Size="Medium", Icon="Trailing" if refused else "None",
                 State="Enabled", Squared="False")
        set_text(d, b, prop(ctx, "Button", "Label"), label)
        if refused:
            set_props(d, b, **{"Trailing icon": d.comp["Icon/open_in_new"]})
        d.set(b, sizingH="fill")
        system_bars(d, fid)
        if not refused:
            T[("permission", "loading")] = b
        else:
            T[("permission_refused", "handoff_app_permissions")] = b

    fid = a.screen("loading", name="Loading / First scan", source="ui/expressive/ExpressiveStates.kt · ExpressiveLoading")
    inst(d, "Loading indicator", fid, name="Loading indicator", Contained="False", x=181.5, y=433)
    system_bars(d, fid)
    # The first scan ends on its own: the prototype moves on to Home after a moment.
    link(ctx, "loading", None, "home", trigger="afterDelay", delay=2500, how="fade", label="Scan finishes")

    fid = a.screen("empty_library", name="Home / No songs", source="ui/expressive/ExpressiveHome.kt · ExpressiveHome")
    bar = inst(d, "Top app bar/Root", fid, name="App bar", Kind="Home", Scrolled="False", x=0, y=0)
    set_text(d, bar, prop(ctx, "Top app bar/Root", "Title"), "Attic")
    col = d.frame(fid, name="Empty", layout="v", pad=[64, 0, 0, 0], w=411, x=0, y=112, sizing=("fixed", "hug"))
    inst(d, "Empty state", col, name="Empty state", Art="Clover")
    inst(d, "Navigation bar", fid, name="Navigation bar", Selected="Home", x=0, y=H - 88)
    system_bars(d, fid)
    wire_shell(d, T, "empty_library", fid, back=False, mini=False)
    T.pop(("empty_library", "home"), None)      # Home is this screen
    T[("empty_library", "settings")] = d.child(bar, "Settings")
    a.close()
    annotate(d, "permission", psrc)
    annotate(d, "permission_refused", psrc)
    annotate(d, "loading", "ui/expressive/ExpressiveStates.kt · ExpressiveLoading",
             "The indicator morphs through seven shapes; this frame shows the one the render caught.")
    annotate(d, "empty_library", "ui/expressive/ExpressiveHome.kt · ExpressiveHome")


def build(d: Doc, ctx: dict):
    T = ctx.setdefault("triggers", {})
    build_settings(d, ctx, T)
    build_recap(d, ctx, T)
    build_first_run(d, ctx, T)
    build_states(d, ctx, T)
