"""Screens: Settings (Appearance is its first group), licences, backup and restore, Recap and its story and share
images, the first run, permission, empty and loading states. Page: Screens (areas Settings, Recap, First run,
Permission, empty and loading).

Owner: settings screens agent. Every screen is built from instances (components_core, components_overlays,
components_states); screen-only frames are auto-layout columns and rows. Triggers for the finishing agent are
stored in ctx["triggers"][(src_key, dst_key)] = Light layer id.
"""
from __future__ import annotations

from pathlib import Path

from figbuild.kit import Doc, V

from .components_core import COVER, cover_image, _cover, K
from .components_states import (setp, inst, group, row, block, scaled, tint_hex, SONG_COVER, ARTIST_COVER, TOP_SONGS,
                                TOP_ARTISTS, LANGUAGES, CARD_NAMES, WASH_CARDS, artist_minutes)
from .foundations import _modes, HARNESS_NOTES, REF_DIFFERS

SRC = "ui/comfort/"
REPO = Path(__file__).resolve().parents[3]

STORY_KEYS = ["intro", "minutes", "top_song", "top_songs", "top_artist", "top_artists", "languages", "listening_clock",
              "highlights", "summary"]
NO_REF = {"recap_story_summary", "recap_share_summary"}   # Comfort's Summary card throws while measuring
KEYS = (["settings", "settings_full", "settings_analysis", "settings_lyrics_download", "settings_backup",
         "dialog_restore_confirm", "dialog_restore_result", "dialog_restore_problem", "licences", "licence_text",
         "recap", "recap_full"]
        + [f"recap_story_{k}" for k in STORY_KEYS] + [f"recap_share_{k}" for k in STORY_KEYS]
        + ["onboarding_welcome", "onboarding_look", "onboarding_access", "onboarding_access_granted",
           "onboarding_access_refused", "onboarding_library_scanning", "onboarding_library_found", "onboarding_library_empty",
           "permission", "permission_refused", "loading", "empty_library"])
REFS = {k: f"comfort-{{theme}}-{k}" for k in KEYS if k not in NO_REF}
# Where the drawing and the render part for the renderer's reasons, not the layout's.
WIDER = ("FigCraft sets Onest about 2 % wider than Android does, so a long paragraph here breaks a word or two earlier "
         "than in the render and can run a line longer. The text, style and width are the code's.")
HEADLINE = ("Recap's headlines (Onest 800, a line height under the font's own: 34 on 38, 40 on 44) sit about 4dp higher "
            "here and their lines about 2dp closer than in the render, where Compose centres the glyphs in each line "
            "(CssLineHeight); what follows moves up with them. Sizes and spacing are the code's.")

H_SETTINGS_FULL = 5345 / 2.625
H_RECAP_FULL = 7451 / 2.625


class S:
    """Screen building shared by the areas."""

    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.k = K(d, ctx["tokens"])
        self.trig = ctx.setdefault("triggers", {})
        self.notes: dict[str, list[tuple[str, str]]] = {}

    # -------------------------------------------------------------- frame and chrome
    def phone(self, a, key, name, source, h=914.0, top=40):
        d = self.d
        f = a.screen(key, name=name, source=source, h=h)
        col = d.frame(f, name="Content", layout="v", w=411, x=0, y=0, pad=[top, 0, 0, 0], cross="center",
                      sizing=("fixed", "hug"))
        return f, col

    def chrome(self, f, h=914.0, dock="Home", mini=True, status=True, gesture=True):
        d = self.d
        if dock:
            dk = d.instance("Dock", f, name="Dock")
            setp(d, dk, Tab=dock, **{"Mini player": str(mini)})
            d.set(dk, x=0, y=h - d.get(dk)["height"], constraints={"horizontal": "leftRight", "vertical": "bottom"})
        if status:
            sb = d.instance("Status bar", f, name="Status bar", x=0, y=0)
        if gesture:
            gb = d.instance("Gesture bar", f, name="Gesture bar", x=0, y=h - 24)
            d.set(gb, constraints={"horizontal": "leftRight", "vertical": "bottom"})

    def collapsed_bar(self, f, title, back=True):
        """RootPage's collapsed bar: the ground under the status bar and the lifted Top bar."""
        d = self.d
        bg = d.frame(f, name="Bar", layout="v", w=411, x=0, y=0, pad=[40, 0, 0, 0], fill="Color/ground", sizing=("fixed", "hug"))
        tb = d.instance("Top bar", bg, name="Top bar")
        setp(d, tb, Back="Arrow" if back else "None", Lift="True", Count="False", Title=title)
        return tb

    def hero(self, col, title, subtitle=None, short=False, mark=False):
        d = self.d
        h = d.instance("Hero", col, name="Hero")
        setp(d, h, Mark=str(mark), Short=str(short), Title=title, **{"Show subtitle": subtitle is not None})
        if subtitle:
            setp(d, h, Subtitle=subtitle)
        # Long titles wrap inside the 32dp sides, centred, as Text does.
        d.set(d.child(h, "Title"), width=347, textAutoResize="height")
        return h

    def lead_tools(self, col):
        d = self.d
        t = d.frame(col, name="Tools", layout="h", w=411, h=50, pad=[0, 0, 2, 8], cross="center", sizing=("fixed", "fixed"))
        b = d.instance("Icon button", t, name="Back")
        setp(d, b, Icon="Icon/arrow_back")
        return b

    def label(self, col, text, top=None):
        d = self.d
        l = d.instance("Section label", col, name=f"Label: {text}")
        setp(d, l, Label=text)
        if top is not None:
            d.set(l, paddingTop=top)
        return l

    def gap(self, col, h, name="Space"):
        return self.d.frame(col, name=name, w=411, h=h)

    def text(self, col, chars, style, ink, name, pad=(0, 28, 0, 28), align=None):
        d = self.d
        fr = d.frame(col, name=name, layout="v", w=411, pad=list(pad), sizing=("fixed", "hug"))
        return self.k.text(fr, chars, style, ink, f"{name} text", fill=True, align=align)

    def finish(self, key, extra=()):
        """Source annotation, notes and Ready for dev on both copies (run after Area.close())."""
        d = self.d
        s = d.screens[key]
        for fid in (s["light"], s["dark"]):
            if not fid:
                continue
            d.annotate(fid, f"Compose: {s['source'].replace('ui/comfort/', SRC)}", "Source")
            for cat, txt in list(extra) + self.notes.get(key, []):
                d.annotate(fid, txt, cat)
            d.ready_for_dev(fid)

    def dark_theme(self, keys):
        """The Dark renders are made with the theme set to Dark: select Dark, not Light, on the Dark copies."""
        d = self.d
        for k in keys:
            sc = d.screens[k]
            for nm, on in (("Theme: Dark", True), ("Theme: Light", False)):
                for lid in d.find(sc["light"], name=nm, limit=5):
                    if d.get(lid).get("name") == nm and lid in sc["map"]:
                        setp(d, sc["map"][lid], Control="Radio on" if on else "Radio off")

    def note(self, key, txt, cat="Code differs from mock"):
        self.notes.setdefault(key, []).append((cat, txt))


# ============================================================================== build
def build(d: Doc, ctx: dict):
    s = S(d, ctx)
    _settings_area(s)
    _recap_area(s)
    _first_run_area(s)
    _states_area(s)


# ------------------------------------------------------------------------------ Settings
SETTINGS_SRC = "ui/comfort/ComfortSettings.kt · ComfortSettings"


def _settings_content(s: S, col, *, lyrics_running=False, analysis_running=False, key=None):
    d = s.d
    out = {}
    s.hero(col, "Settings")
    out["back"] = s.lead_tools(col)
    # Appearance: the theme, then the style
    s.label(col, "Appearance")
    g, _ = group(d, col, "Appearance")
    for i, (t, on) in enumerate((("Match phone", False), ("Dark", False), ("Light", True))):
        row(d, g, "Settings row", f"Theme: {t}", Control="Radio on" if on else "Radio off", Title=t, **{"Show hairline": i > 0})
    s.label(col, "Style")
    g, _ = group(d, col, "Style switch")
    out["style"] = g
    for i, (t, on) in enumerate((("Expressive", False), ("Comfort", True))):
        r = row(d, g, "Settings row", f"Style: {t}", Control="Radio on" if on else "Radio off", Title=t, **{"Show hairline": i > 0})
        out[f"style_{t}"] = r
    # Lyrics
    s.label(col, "Lyrics")
    g, _ = group(d, col, "Lyrics")
    para = "51 of 69 songs have lyrics saved on this phone. Saved lyrics work offline."
    privacy = "Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are never sent."
    if lyrics_running:
        block(d, g, "Lyrics: downloading", text=para, bold="51 of 69 songs", small_above=privacy, progress=23 / 69,
              small="Checked 23 of 69, found 17")
    else:
        block(d, g, "Lyrics: saved", text=para, bold="51 of 69 songs", small_above=privacy, button="Download lyrics for all songs")
    # Song radio
    s.label(col, "Song radio")
    g, _ = group(d, col, "Song radio")
    row(d, g, "Settings row", "Autoplay", Control="Switch on", Title="Autoplay", Summary="When a queue reaches its last song, "
        "radio keeps going with similar songs.", **{"Show summary": True})
    block(d, g, "Song analysis", text="Radio finds similar songs by listening to your files. This happens on this phone, and "
          "nothing is uploaded.", progress=41 / 69 if analysis_running else 1.0,
          small="Analysing songs: 41 of 69" if analysis_running else "All 69 songs analysed.", after_row=True)
    # Backup
    out["backup_label"] = s.label(col, "Backup")
    g, _ = group(d, col, "Backup")
    out["backup"] = g
    out["save"] = row(d, g, "Settings row", "Back up to a file", Control="None", Title="Back up to a file",
                      Summary="Saves favourites, playlists, listening history, saved lyrics and settings in one file. Keep it "
                              "somewhere other than this phone.", **{"Show summary": True})
    out["restore"] = row(d, g, "Settings row", "Restore from a file", Control="None", Title="Restore from a file",
                         Summary="Adds a backup to what is on this phone. Nothing here is deleted.",
                         **{"Show summary": True, "Show hairline": True})
    block(d, g, "Backup note", small="Song analysis isn't in the file, so a new phone analyses the songs again while it "
          "charges. If backup is on in the phone's settings, Android also keeps a copy, encrypted with your screen lock.",
          after_row=True)
    # About
    s.label(col, "About")
    g, slot = group(d, col, "About")
    about = d.frame(None, name="About Attic", layout="h", w=379, pad=[16, 20, 16, 20], gap=16, cross="center",
                    sizing=("fixed", "hug"))
    mb = d.instance("Mark box", about, name="Mark box")
    d.set(mb, width=52, height=52)
    body = "Attic plays your own music files, from any folder. It has no account and no streaming. 69 songs on this phone."
    t = s.k.text(about, body, "Component/Settings block", "Color/on-2", "About text", fill=True)
    d.cmd("node.set", ids=[t], start=body.index("69 songs"), end=len(body), fontStyle="SemiBold")
    for c in d.get(slot, depth=1).get("children", []):
        d.cmd("edit.delete", ids=[c["id"]])
    d.__dict__.setdefault("_slot_used", {})[g] = True
    d.cmd("node.reparent", ids=[about], parent=slot)
    d.set(about, sizingH="fill")
    out["licences"] = row(d, g, "Settings row", "Open-source licences", Control="Chevron", Title="Open-source licences",
                          **{"Show hairline": True})
    s.gap(col, 8 + 182 - 30, "Space for the dock")
    return out


def _settings_scrolled(s: S, a, key, name, offset, **kw):
    d = s.d
    f, col = s.phone(a, key, name, SETTINGS_SRC)
    out = _settings_content(s, col, **kw)
    d.set(col, y=-offset)
    s.collapsed_bar(f, "Settings")
    s.chrome(f)
    return f, out


def _settings_area(s: S):
    d = s.d
    a = d.area("Settings", note="Settings is a root page: the large title in the empty upper third, the back arrow in the row that "
                                "closes it, then one labelled card per section. Appearance (theme) and Style are its first "
                                "two groups; there is no separate Appearance screen. Scrolled, the title joins the back arrow "
                                "in the bar. Restoring a backup asks first, then reports, as dialogs at the bottom edge.")
    f, col = s.phone(a, "settings", "Settings / Top", SETTINGS_SRC)
    o = _settings_content(s, col)
    s.chrome(f)
    d.set(o["style"], name="Style switch")
    s.trig[("settings", "settings_style_switch")] = o["style"]
    s.trig[("settings", "settings_backup")] = o["backup"]
    s.trig[("settings", "licences")] = o["licences"]
    s.note("settings", "Appearance is not its own screen: the theme radios (Match phone, Dark, Light) and the Style radios "
                       "(Expressive, Comfort) are the first two groups of Settings. Picking a style switches the whole app.")

    for k in ("settings", "settings_full"):
        s.note(k, "The code adds a Style group (Expressive, Comfort) under Appearance and a Backup section (Back up to a file, "
                  "Restore from a file, the note) that sheet 08 doesn't have, and About says \"from any folder\" where the mock "
                  "said Music/Offline. Analysis is done in the demo library, so its button is hidden.")
    for k in ("settings_backup", "dialog_restore_confirm", "dialog_restore_result", "dialog_restore_problem"):
        s.note(k, "Backup and restore aren't in sheet 08: drawn from ComfortSettings.kt (the Backup group, BackupDialogs) and "
                  "Overlays.kt ComfortDialog.")
    for k in ("dialog_restore_confirm", "dialog_restore_result", "dialog_restore_problem"):
        s.note(k, HARNESS_NOTES["dialog"], REF_DIFFERS)
    for k in ("settings", "settings_full"):
        s.note(k, "About: " + WIDER, REF_DIFFERS)

    f, col = s.phone(a, "settings_full", "Settings / Top (full length)", SETTINGS_SRC, h=H_SETTINGS_FULL)
    o = _settings_content(s, col)
    s.chrome(f, h=H_SETTINGS_FULL)
    d.set(o["style"], name="Style switch")

    _settings_scrolled(s, a, "settings_analysis", "Settings / Song analysis running", 1018, analysis_running=True)
    _settings_scrolled(s, a, "settings_lyrics_download", "Settings / Lyrics downloading", 342, lyrics_running=True)
    f, o = _settings_scrolled(s, a, "settings_backup", "Settings / Backup", 1018)
    # Restore goes through Android's file picker too (note:files), which leads to the question or to "not a backup".
    s.trig[("settings_backup", "note:files")] = o["restore"]

    dialogs = (
        ("dialog_restore_confirm", "Settings / Restore, confirm", "Restore this backup?",
         "Made November 2, 2026, with 42 favourites, 3 playlists, 4,870 plays and lyrics for 51 songs. They are added to what "
         "is on this phone, and nothing here is deleted. Theme, style and autoplay change to the backup's.", "Two", "Restore"),
        ("dialog_restore_result", "Settings / Restore, done", "Backup restored",
         "Added 38 favourites, 2 playlists, 4,312 plays and lyrics for 47 songs. Songs were also added to 1 playlist that "
         "already had the same name. 64 of 69 songs in the backup are on this phone. The rest keep their place in your "
         "favourites, playlists and history, and show up when their files are added to this phone.", "One", "Close"),
        ("dialog_restore_problem", "Settings / Restore, not a backup", "Couldn't restore", "This file isn't an Attic backup.",
         "One", "Close"),
    )
    for key, name, title, body, buttons, confirm in dialogs:
        if "Dialog" not in d.comp:
            continue
        f, o = _settings_scrolled(s, a, key, name, 1018)
        d.instance("Scrim", f, name="Scrim", x=0, y=0) if "Scrim" in d.comp else d.rect(f, name="Scrim", w=411, h=914, fill="Color/scrim")
        dl = d.instance("Dialog", f, name="Dialog")
        vals = {"Content": "Body", "Buttons": buttons, "Confirm": "Default", "Title": title, "Body": body, "Confirm label": confirm}
        if buttons == "Two":
            vals["Dismiss label"] = "Cancel"
        setp(d, dl, **vals)
        d.set(dl, width=387)
        g = d.get(dl)
        d.set(dl, x=12, y=914 - 24 - 22 - g["height"])
        if key == "dialog_restore_confirm":
            btn = _find_named(d, dl, ("Confirm", "Confirm button", "Restore"))
            s.trig[("dialog_restore_confirm", "dialog_restore_result")] = btn or dl
    # -- Backup saved: the message once the file is written (BackupController.kt), over Settings at Backup.
    f, o = _settings_scrolled(s, a, "snackbar_backup_saved", "Settings / Backup saved", 1018)
    ts = d.instance("Toast", f, name="Toast")
    setp(d, ts, Text="Backup saved")
    g, dk = d.get(ts), d.get(d.child(f, "Dock"))
    d.set(ts, x=(411 - g["width"]) / 2, y=914 - dk["height"] - 6 - g["height"])
    s.note("snackbar_backup_saved", "After Android's file picker returns, BackupController writes the file and says "
           "\"Backup saved\" (or \"Couldn't save the backup\"), a toast 6dp above the dock (ComfortShell.kt ToastHost) for "
           "about four seconds.", "Note")
    a.close()
    s.dark_theme(a.keys)
    for k in a.keys:
        s.finish(k)

    # Licences
    a = d.area("Licences", note="Open-source licences, from About in Settings: one card of notices, each opening its text.")
    f, col = s.phone(a, "licences", "Licences / List", "ui/comfort/ComfortLicences.kt · ComfortLicences")
    s.hero(col, "Open-source licences")
    s.lead_tools(col)
    g, _ = group(d, col, "Notices")
    first = None
    for i, (n, l) in enumerate((("Attic", "GNU General Public License v3.0"),
                                ("Discogs-EffNet model", "Creative Commons BY-NC-SA 4.0"),
                                ("ONNX Runtime", "MIT License"), ("Google Sans Flex", "SIL Open Font License 1.1"),
                                ("Onest", "SIL Open Font License 1.1"), ("Material Symbols", "Apache License 2.0"),
                                ("Jetpack, Kotlin and other libraries", "Apache License 2.0"))):
        r = row(d, g, "Settings row", f"Notice: {n}", Control="Chevron", Title=n, Summary=l,
                **{"Show summary": True, "Show hairline": i > 0})
        first = first or r
    s.chrome(f)
    s.trig[("licences", "licence_text")] = first

    f, col = s.phone(a, "licence_text", "Licences / Licence text", "ui/comfort/ComfortLicences.kt · ComfortLicence")
    s.hero(col, "Attic", short=True)
    s.lead_tools(col)
    for i, p in enumerate(_licence_paragraphs()[:9]):
        s.text(col, p, "Component/Settings block", "Color/on-2", f"Paragraph {i + 1}", pad=(6, 28, 6, 28))
    s.note("licence_text", WIDER, REF_DIFFERS)
    s.chrome(f)
    a.close()
    for k in a.keys:
        s.finish(k)


def _find_named(d, root, names):
    for n in names:
        try:
            return d.child(root, n)
        except Exception:
            continue
    return None


def _licence_paragraphs() -> list[str]:
    """LicencesModel.kt licenceParagraphs over the GPL text (the repo's LICENSE is LICENSE-attic.txt)."""
    import re
    text = (REPO / "LICENSE").read_text().replace("\r\n", "\n")
    out = []
    for block in re.split(r"\n\s*\n", text):
        lines = [l.rstrip() for l in block.strip("\n").split("\n")]
        if not any(l.strip() for l in lines):
            continue
        laid = any(l.startswith(" ") or l.startswith("\t") for l in lines[1:])
        out.append("\n".join(lines) if laid else " ".join(l.strip() for l in lines))
    return out


# ------------------------------------------------------------------------------ Recap
RECAP_SRC = "ui/comfort/ComfortRecap.kt · ComfortRecap"
STORY_SRC = "ui/comfort/ComfortRecapStory.kt · ComfortRecapStory"
CARD_SRC = "ui/comfort/ComfortRecapCards.kt · ComfortRecapCard"


def _recap_page(s: S, col, f):
    d = s.d
    s.hero(col, "Recap", subtitle="Your 2026 so far")
    bk = d.instance("Icon button", f, name="Back", x=8, y=40 + 228 - 50 + 4)
    setp(d, bk, Icon="Icon/arrow_back")
    tl = d.instance("Tools", col, name="Periods")
    setp(d, tl, Leading="Sub-tabs")
    d.set(d.child(tl, "Settings"), visible=False)
    st = d.child(tl, "Sub-tabs")
    for old, new in (("All", "This week"), ("Albums", "This month"), ("Artists", "This year"), ("Folders", "All time")):
        t = d.child(st, f"Sub-tab: {old}")
        setp(d, t, Selected=str(new == "This year"), Label=new)
        d.set(t, name=f"Sub-tab: {new}")
    play = d.instance("Play recap", col, name="Play recap")
    # Stats
    s.gap(col, 12, "Space")
    g, slot = group(d, col, "Stats")
    for c in d.get(slot, depth=1).get("children", []):
        d.cmd("edit.delete", ids=[c["id"]])
    stats = [("18,420", "Minutes"), ("4,870", "Plays"), ("69", "Songs"), ("9", "Artists"), ("301", "Days with music"),
             ("46 days", "Longest streak")]
    for r in range(3):
        rw = d.frame(None, name=f"Stats row {r + 1}", layout="h", w=379, sizing=("fixed", "hug"))
        if r:
            hl = d.rect(rw, name="Hairline", w=379, h=1, fill="Color/line")
            s.k.absolute(hl, 0, 0, 379, 1, {"horizontal": "leftRight", "vertical": "top"})
        for c in range(2):
            v, lab = stats[r * 2 + c]
            if c:
                d.rect(rw, name="Divider", w=1, h=10, fill="Color/line", sizingV="fill")
            sc = d.instance("Stat cell", rw, name=f"Stat: {lab}")
            setp(d, sc, Value=v, Label=lab)
            d.set(sc, sizingH="fill", paddingBottom=17)   # the renders' rows are 82dp: the label line draws 19
        d.cmd("node.reparent", ids=[rw], parent=slot)
        d.set(rw, sizingH="fill")
    # Share a card
    s.label(col, "Share a card")
    tray = d.frame(col, name="Cards", layout="h", gap=10, w=411, pad=[0, 24, 0, 24], sizing=("fixed", "hug"), clip=True)
    tray_cards = []
    for i, name in enumerate(CARD_NAMES):
        box = d.frame(tray, name=f"Card: {name}", w=118, h=round(118 * 16 / 9, 2), radius=14, clip=True)
        scaled(d, "Recap card", box, "Card", 118 / 360, Card=name)
        tray_cards.append(box)
    # Top songs
    lr = d.frame(col, name="Label: Top songs", layout="h", w=411, pad=[15, 16, 4, 28], cross="center", sizing=("fixed", "hug"))
    s.k.text(lr, "Top songs", "Scale/Label", "Color/on-2", "Label", fill=True)
    pb = d.instance("Button", lr, name="Play")
    setp(d, pb, Style="Tonal", Size="Small", State="Default", Tone="Default", Label="Play", **{"Show leading icon": True})
    setp(d, pb, **{"Leading icon": "Icon/play_arrow (filled)"})
    g, _ = group(d, col, "Top songs")
    for i, (t, a_, plays) in enumerate(TOP_SONGS):
        r = row(d, g, "Song row", f"Song: {t}", Lead="Rank", Current=str(i == 0), Title=t, Subtitle=a_, Number=str(i + 1),
                Meta=f"{plays} plays", **{"Show meta": True, "Show trailing": False, "Show hairline": i > 0})
        cover_image(d, d.child(r, "Cover"), COVER[SONG_COVER[t]])
    s.label(col, "Top artists")
    g, _ = group(d, col, "Top artists")
    for i, (n, plays) in enumerate(TOP_ARTISTS):
        r = row(d, g, "Song row", f"Artist: {n}", Lead="Rank artist", Current="False", Title=n, Number=str(i + 1),
                Meta=f"{artist_minutes(plays):,} min", **{"Show meta": True, "Show trailing": False, "Show hairline": i > 0,
                                                          "Show subtitle": False})
        cover_image(d, d.child(r, "Cover"), COVER[ARTIST_COVER[n]])
    # Languages
    s.label(col, "Languages")
    g, slot = group(d, col, "Languages")
    for c in d.get(slot, depth=1).get("children", []):
        d.cmd("edit.delete", ids=[c["id"]])
    lw = d.frame(None, name="Languages", layout="v", w=379, pad=[20, 20, 4, 20], sizing=("fixed", "hug"))
    split = d.frame(lw, name="Split", layout="h", gap=2, h=28, radius=14, clip=True, sizing=("fill", "fixed"))
    steps = ["Color/accent", "Color/derived/language-2", "Color/derived/language-3", "Color/on-3"]
    for i, (lang, share) in enumerate(LANGUAGES):
        d.rect(split, name=f"Share: {lang}", w=round((339 - 6) * share, 2), h=28, fill=steps[i])
    d.frame(lw, name="Space", w=10, h=6)
    for i, (lang, share) in enumerate(LANGUAGES):
        lr_ = d.instance("Language row", lw, name=f"Language: {lang}")
        setp(d, lr_, Language=lang, Share=f"{round(share * 100)}%", **{"Show hairline": i > 0})
        d.set(lr_, sizingH="fill")
        d.override(lr_, "Swatch", fill=steps[i])
    d.cmd("node.reparent", ids=[lw], parent=slot)
    d.set(lw, sizingH="fill")
    s.label(col, "When you listen")
    d.instance("Clock summary", col, name="When you listen")
    nt = d.instance("Note", col, name="Footnote")
    setp(d, nt, Text="Counted from Attic's listening log, which began on 1 January 2025. A song counts as a play after 30 seconds.")
    s.gap(col, 16 + 182 - 30, "Space for the dock")
    s.tray_cards = tray_cards
    return play


def _story(s: S, a, idx: int):
    d = s.d
    key = STORY_KEYS[idx]
    name = CARD_NAMES[idx]
    f = a.screen(f"recap_story_{key}", name=f"Recap story / {idx + 1} {name}", source=STORY_SRC)
    st = d.frame(f, name="Story", w=411, h=914, x=0, y=0, fill="Color/ground")
    _modes(d, st, "Dark")
    if name in WASH_CARDS:
        d.set_mode(st, "Color", "Tinted wash")
    d.instance("Status bar", st, name="Status bar", x=0, y=0)
    hd = d.instance("Story header", st, name="Header", x=0, y=40)
    cw = 353
    ch = cw * 16 / 9
    card = scaled(d, "Recap card", st, "Card", cw / 360, x=29, y=round(100 + (660 - ch) / 2, 2), Card=name)
    d.set(card, cornerRadius=round(26 * cw / 360, 2), clip=True)
    deck = d.instance("Story deck", st, name="Deck", x=0, y=914 - 24 - 130)
    setp(d, deck, Position=f"{idx + 1} of 10")
    bar = d.child(deck, "Progress")
    d.override(bar, "Fill", width=round(253 * (idx + 0.1) / 10 + 10, 2))
    d.instance("Gesture bar", st, name="Gesture bar", x=0, y=890)
    k = f"recap_story_{key}"
    if idx + 1 < len(STORY_KEYS):
        s.trig[(k, f"recap_story_{STORY_KEYS[idx + 1]}")] = card
    s.trig[(k, "recap")] = d.child(hd, "Close")
    s.trig[(k, "note:share")] = d.child(deck, "Share")
    s.note(k, HARNESS_NOTES["story"], REF_DIFFERS)
    s.note(k, HEADLINE, REF_DIFFERS)
    if name == "Summary":
        s.note(k, "Known issue: Comfort's Summary card throws on render, so the story crashes when it reaches this card. "
                  "SummaryCard puts SummaryStat (whose FitText is a BoxWithConstraints) in Row(Modifier.height(IntrinsicSize.Min)), "
                  "and a SubcomposeLayout can't answer intrinsic measurements (ui/comfort/ComfortRecapCards.kt around line 640). "
                  "Drawn from the code; there is no reference render.", "Known issue")
        s.note(k, "Code differs from mock: the code currently throws on this card (see Known issue); this frame shows what "
                  "SummaryCard would draw.")


def _share(s: S, a, idx: int):
    d = s.d
    key = STORY_KEYS[idx]
    name = CARD_NAMES[idx]
    f = a.screen(f"recap_share_{key}", name=f"Recap share image / {idx + 1} {name}", source=CARD_SRC, h=round(411 * 16 / 9, 2))
    d.set(f, fills=[])
    scaled(d, "Recap card", f, "Card", 411 / 360, Card=name)
    if name != "Summary":
        s.note(f"recap_share_{key}", HEADLINE, REF_DIFFERS)
    if name == "Summary":
        s.note(f"recap_share_{key}", "Known issue: Save and Share of the Summary card fail, because SummaryCard throws while "
               "measuring (SummaryStat's FitText in a Row with IntrinsicSize.Min, ui/comfort/ComfortRecapCards.kt around line "
               "640). Drawn from the code; there is no reference render.", "Known issue")
        s.note(f"recap_share_{key}", "Code differs from mock: the code currently throws on render for this image (see Known issue).")


def _recap_area(s: S):
    d = s.d
    a = d.area("Recap", note="Recap's page is an ordinary screen of the kit: the title and the period's headline, the periods as "
                             "sub-tabs, then cards. Home's Recap entry opens it; Play recap opens the story.")
    f, col = s.phone(a, "recap", "Recap / This year", RECAP_SRC)
    play = _recap_page(s, col, f)
    s.chrome(f)
    s.trig[("recap", "recap_story_intro")] = play
    # A card in "Share a card" opens the story at that card (ComfortRecap.kt: clickable { openStory(index) }).
    more = s.ctx.setdefault("triggers_more", [])
    for i, box in enumerate(s.tray_cards):
        more.append(("recap", f"recap_story_{STORY_KEYS[i]}", box))
    tw = d.child(d.child(f, "Periods"), "Sub-tab: This week")
    more.append(("recap", "recap_empty", tw))
    f, col = s.phone(a, "recap_full", "Recap / This year (full length)", RECAP_SRC, h=H_RECAP_FULL)
    _recap_page(s, col, f)
    s.chrome(f, h=H_RECAP_FULL)
    def page_top(key, name, subtitle, period):
        """The hero, its Back and the periods with `period` selected: what the page draws before its content."""
        f, col = s.phone(a, key, name, RECAP_SRC)
        s.hero(col, "Recap", subtitle=subtitle)
        bk = d.instance("Icon button", f, name="Back", x=8, y=40 + 228 - 50 + 4)
        setp(d, bk, Icon="Icon/arrow_back")
        tl = d.instance("Tools", col, name="Periods")
        setp(d, tl, Leading="Sub-tabs")
        d.set(d.child(tl, "Settings"), visible=False)
        st = d.child(tl, "Sub-tabs")
        for old, new in (("All", "This week"), ("Albums", "This month"), ("Artists", "This year"), ("Folders", "All time")):
            tb = d.child(st, f"Sub-tab: {old}")
            setp(d, tb, Selected=str(new == period), Label=new)
            d.set(tb, name=f"Sub-tab: {new}")
        return f, col, st

    # -- Loading: until a period's numbers arrive (data == null || state.loading) the page draws the spinner under the
    # periods, in a full-width box padded 48 all round.
    f, col, _ = page_top("recap_loading", "Recap / This year, loading", "Your 2026 so far", "This year")
    box = d.frame(col, name="Loading", layout="v", w=411, pad=48, cross="center", sizing=("fixed", "hug"))
    sp = d.instance("Spinner", box, name="Spinner")
    setp(d, sp, Size="Default")
    s.chrome(f)
    s.note("recap_loading", "Drawn from the code; there is no reference render. Opening Recap, or picking another period, "
           "shows the spinner (Spinner, 36dp) in place of Play recap and the cards until that period's numbers have "
           "loaded. The headline above is already the period's, as periodHeadline words it before anything loads. In "
           "the prototype Home's Recap entry opens this frame, which moves on to Recap / This year after a second.",
           "Note")
    # -- Nothing played: a period with no plays (Recap.isEmpty) draws the empty state under the periods.
    f, col, st = page_top("recap_empty", "Recap / This week, nothing played", "Your week in music", "This week")
    es = inst(d, "Empty state", col, "Empty", Title="Nothing played this week yet",
              Body="Any song you play for 30 seconds or more counts. Your top songs, artists and listening clock build up "
                   "from there.", **{"Show body": True})
    setp(d, d.child(es, "Icon box"), Icon="Icon/graphic_eq")
    s.chrome(f)
    more.append(("recap_empty", "recap", d.child(st, "Sub-tab: This year")))
    s.note("recap_empty", "A period with nothing logged (Recap.isEmpty) shows this instead of Play recap and the cards; the "
           "title follows the period (\"Nothing played this month yet\", and so on). The demo library's week has plays, so "
           "the prototype stands this frame in for an empty week.", "Note")
    a.close()
    for k in a.keys:
        s.finish(k)

    a = d.area("Recap story", note="The story covers everything, dock included, and is always dark, as the cards are. On a "
                                   "cover-washed card (top song, top artist, summary) the screen takes the wash too. Tap the "
                                   "left third of the card to go back, anywhere else to go on; hold to pause.")
    for i in range(len(STORY_KEYS)):
        _story(s, a, i)
    a.close()
    for k in a.keys:
        s.finish(k)

    a = d.area("Recap share images", note="What Save and Share write: the same card drawing at 1080 x 1920 px, here a 411 x 731 "
                                          "frame (the card scaled by 411/360). Always dark, whatever the theme; the Dark row "
                                          "matches the Light one.")
    for i in range(len(STORY_KEYS)):
        _share(s, a, i)
    a.close()
    for k in a.keys:
        s.finish(k)


# ------------------------------------------------------------------------------ First run
ONB_SRC = "ui/comfort/ComfortOnboarding.kt · ComfortOnboarding"


def _onboarding(s: S, a, key, name, step, button, *, centred=False):
    d = s.d
    f = a.screen(key, name=name, source=ONB_SRC)
    s.chrome(f, dock=None)
    tb = d.instance("Onboarding top bar", f, name="Top bar", x=0, y=40)
    setp(d, tb, Step=str(step))
    body = d.frame(f, name="Step", layout="v", w=411, h=690, x=0, y=104, pad=[0, 0, 24, 0], cross="center",
                   main="center" if centred else "min", sizing=("fixed", "fixed"), clip=True)
    bf = d.frame(f, name="Bottom", layout="v", w=411, x=0, y=914 - 24 - 96, pad=[8, 24, 32, 24], sizing=("fixed", "hug"))
    b = d.instance("Button", bf, name="Button")
    setp(d, b, Style="Filled", Size="Large", State="Default", Tone="Default", Label=button, **{"Show leading icon": False})
    d.set(b, sizingH="fill")
    return f, body, b


def _first_run_area(s: S):
    d = s.d
    a = d.area("First run", note="The first run: Welcome, the look (style and theme), access to music and notifications, then "
                                 "the library scan. Back and the step pips at the top, the one full-width button at the "
                                 "bottom edge. Steps slide in a little, as detail screens do.")
    flow = []
    # Welcome
    f, body, b = _onboarding(s, a, "onboarding_welcome", "First run / Welcome", 1, "Get started", centred=True)
    # A variant switch keeps the Permission copy in Title and Body, so both are set (strings.xml onboarding_welcome_*).
    inst(d, "Prompt", body, "Welcome", Kind="Welcome", Title="Welcome to Attic",
         Body="Attic plays the music files on this phone. There's no account, and everything works offline.")
    s.gap(body, 96, "Lift")
    flow.append(("onboarding_welcome", b))
    # Look
    f, body, b = _onboarding(s, a, "onboarding_look", "First run / Look", 2, "Continue")
    h = inst(d, "Onboarding heading", body, "Heading", Title="Pick your look", Body="You can change it any time in Settings.")
    cards = d.frame(body, name="Looks", layout="h", gap=12, w=411, pad=[28, 16, 0, 16], sizing=("fixed", "hug"))
    for style in ("Expressive", "Comfort"):
        c = inst(d, "Look card", cards, f"Look: {style}", Style=style, Selected=str(style == "Comfort"))
        d.set(c, sizingH="fill")
    s.trig[("onboarding_look", "onboarding_style_switch")] = cards
    s.label(body, "Theme")
    g, _ = group(d, body, "Theme")
    for i, (t, on) in enumerate((("Match phone", False), ("Dark", False), ("Light", True))):
        row(d, g, "Settings row", f"Theme: {t}", Control="Radio on" if on else "Radio off", Title=t, **{"Show hairline": i > 0})
    flow.append(("onboarding_look", b))

    # Access
    def access(key, name, audio, notif, button):
        f, body, b = _onboarding(s, a, key, name, 3, button)
        hd = inst(d, "Onboarding heading", body, "Heading", Title="Let Attic find your music", **{"Show body": False})
        # FigCraft sets this title 340dp wide, Android 336: give it the 1dp it needs to stay on one line, as on the phone.
        d.set(d.child(hd, "Title"), paddingLeft=35.5, paddingRight=35.5)
        s.gap(body, 28)
        g, _ = group(d, body, "Access")
        r = row(d, g, "Access row", "Music and audio", Status=audio, Title="Music and audio",
                Summary="Required. Attic needs it to find the songs on this phone.")
        r2 = row(d, g, "Access row", "Notifications", Status=notif, Title="Notifications",
                 Summary="Optional. Shows the playback controls in the notification shade and on the lock screen.",
                 **{"Show hairline": True})
        setp(d, d.child(r2, "Icon box"), Icon="Icon/notifications")
        if audio == "Off required":
            s.text(body, "Audio access is off, so Attic can't see your songs. Turn on Music and audio in the app's permissions.",
                   "Component/Onboarding body", "Color/on-2", "Refused", pad=(24, 36, 0, 36), align="center")
        return b
    ask = access("onboarding_access", "First run / Access, not asked", "Not asked", "Allow", "Allow access")
    # Allow access raises Android's permission dialog; the prototype note answers it either way.
    s.trig[("onboarding_access", "note:permission")] = ask
    refused = access("onboarding_access_refused", "First run / Access, music refused", "Off required", "Allow", "Open app permissions")
    # The button opens Attic's page in Android's settings; coming back with access on lands on the allowed step.
    s.trig[("onboarding_access_refused", "onboarding_access_granted")] = refused
    s.note("onboarding_access_refused", "Open app permissions leaves for Android's settings. In the prototype it comes back "
           "with Music and audio turned on, on the allowed step.", "Note")
    flow.append(("onboarding_access_granted", access("onboarding_access_granted", "First run / Access, allowed", "Allowed", "Allowed",
                                                     "Continue")))
    # Library
    f, body, b = _onboarding(s, a, "onboarding_library_scanning", "First run / Library, scanning", 4, "Start listening", centred=True)
    scan = d.instance("Library scan", body, name="Scanning")
    s.trig[("onboarding_library_scanning", "onboarding_library_empty")] = scan
    s.note("onboarding_library_scanning", "The scan moves on by itself: to the songs it found, or, with no music on the "
           "phone, to Nothing found. The prototype takes Start listening to the first and a tap on the spinner to the "
           "second.", "Note")
    s.gap(body, 96, "Lift")
    flow.append(("onboarding_library_scanning", b))
    f, body, b = _onboarding(s, a, "onboarding_library_found", "First run / Library, found", 4, "Start listening")
    inst(d, "Onboarding heading", body, "Heading", Title="Found 69 songs", **{"Show body": False})
    s.label(body, "Optional", top=36)
    g, _ = group(d, body, "Optional")
    row(d, g, "Settings row", "Download lyrics for all songs", Control="Switch off", Title="Download lyrics for all songs",
        Summary="Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are never sent.",
        **{"Show summary": True})
    block(d, g, "Song radio", text="Radio analyses your songs while the phone charges. It all happens on this phone.",
          button="Song radio settings")
    flow.append(("onboarding_library_found", b))
    f, body, b = _onboarding(s, a, "onboarding_library_empty", "First run / Library, nothing found", 4, "Start listening")
    inst(d, "Empty state", body, "Empty", Title="No songs on this phone",
         Body="Copy music files onto this phone, in Music or any other folder, and they will show up here.")
    setp(d, d.child(d.child(body, "Empty"), "Icon box"), Icon="Icon/music_note")
    inst(d, "Note", body, "Hint", Text="Connect the phone to a computer and copy music into the Music folder. Songs show up here "
                                       "as they arrive.")
    s.trig[("onboarding_library_empty", "empty_library")] = b
    for k in a.keys:
        s.note(k, "The first run isn't in the HTML mock (sheet 08 has only the permission screen); drawn from "
                  "ComfortOnboarding.kt.")
    a.close()
    s.dark_theme(["onboarding_look"])
    order = ["onboarding_welcome", "onboarding_look", "onboarding_access", "onboarding_access_granted",
             "onboarding_library_scanning", "onboarding_library_found", "home"]
    btn = dict(flow)
    for src, dst in zip(order, order[1:]):
        if src != "onboarding_access":      # Allow access goes through Android's dialog (note:permission)
            s.trig[(src, dst)] = btn[src]
    for k in a.keys:
        s.finish(k)


# ------------------------------------------------------------------------------ Permission, empty, loading
def _states_area(s: S):
    d = s.d
    a = d.area("Permission, empty and loading", note="What shows before there is music: the permission prompt (first ask, then "
                                                    "after it was declined), the first scan's spinner, and Home with no songs.")
    for key, name, title, body, button in (
            ("permission", "Permission / First ask", "Attic plays the music on this phone",
             "Allow audio access so it can find your songs in any folder. Notifications show the playback controls.", "Allow access"),
            ("permission_refused", "Permission / Refused", "Attic plays the music on this phone",
             "Audio access is off, so Attic can't see your songs. Turn on Music and audio in the app's permissions.",
             "Open app permissions")):
        f = a.screen(key, name=name, source="ui/comfort/ComfortStates.kt · ComfortPermission")
        s.chrome(f, dock=None)
        body_f = d.frame(f, name="Message", layout="v", w=411, h=754 - 96, x=0, y=40, main="center", cross="center",
                         sizing=("fixed", "fixed"))
        inst(d, "Prompt", body_f, "Prompt", Kind="Permission", Title=title, Body=body)
        bf = d.frame(f, name="Bottom", layout="v", w=411, x=0, y=914 - 24 - 40 - 56, pad=[0, 24, 0, 24], sizing=("fixed", "hug"))
        b = d.instance("Button", bf, name="Button")
        setp(d, b, Style="Filled", Size="Large", State="Default", Tone="Default", Label=button, **{"Show leading icon": False})
        d.set(b, sizingH="fill")
    for k in ("permission", "permission_refused"):
        s.note(k, "The sentence says \"find your songs in any folder\"; sheet 08 said \"read your songs in Music/Offline\".")
    f = a.screen("loading", name="Loading / First scan", source="ui/comfort/ComfortStates.kt · ComfortLoading")
    s.chrome(f, dock=None)
    sp = d.instance("Spinner", f, name="Spinner", x=0, y=0)
    setp(d, sp, Size="Large")
    d.set(sp, x=(411 - 44) / 2, y=(914 - 44) / 2, constraints={"horizontal": "center", "vertical": "center"})

    f, col = s.phone(a, "empty_library", "Home / No songs", "ui/comfort/ComfortHome.kt · ComfortHome")
    s.hero(col, "Home", mark=True)
    tl = d.instance("Tools", col, name="Tools")
    setp(d, tl, Leading="None")
    for n in ("Search", "Add"):
        d.set(d.child(tl, n), visible=False)
    es = inst(d, "Empty state", col, "Empty")
    s.chrome(f, dock="Home", mini=False)
    s.note("empty_library", "The code says \"No songs on this phone\" with folder_open, and keeps Settings in the tools row; "
                            "sheet 08 said \"No songs in Music/Offline\".")
    a.close()
    for k in a.keys:
        s.finish(k)
