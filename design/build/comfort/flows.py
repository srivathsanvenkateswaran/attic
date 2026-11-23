"""Prototype links, flow starting points, and the Flow overview page.

Owner: finishing agent. Runs after every screen module, before handoff.

PROTOTYPE (page Screens)
  Every trigger the screen modules recorded in ctx["triggers"][(src, dst)] = Light layer id is wired, on the Light
  screen and on its Dark copy (the layer found through the screen's id map). dst "back" is a Back action. Pseudo
  targets go to a `Prototype note` frame: settings_style_switch and onboarding_style_switch to "Style switch"
  (Comfort and Expressive are separate files). A trigger whose source or target screen is missing is skipped and
  listed in ctx["flows_report"].
  EXTRA adds what the screens didn't record: the mini player on every screen with the dock, settings from each tab
  root, Recap from Home (through Recap / Loading, which moves on after a second), songs and Play on the album, a
  song's track menu on Home, Share (to the "Share" note: the system share sheet), back up to a file (to the "File
  picker" note), the restore dialogs' buttons, every Back button not yet wired, and the theme radios.
  Theme radios: `setVariableMode` on every Light/Dark collection (d.mode_collections) and a move to the copy that
  shows the picked radio. For the switch to reach the other screens, the Light frames inherit the default mode
  (Light) instead of setting it, which draws the same; Dark copies keep their explicit Dark.

  Transitions follow ComfortShell.kt: a tab root dissolves (220 ms, fast-out slow-in); a detail screen slides in
  from the right (300 ms); Now Playing, the queue and lyrics move in from the bottom on the overlay spring
  (stiffness 380, damping ratio 1); sheets, dialogs, menus and messages dissolve (200 ms), since each frame repeats
  the screen beneath and a slide would move it too; Recap story cards crossfade (220 ms); filters (sub-tabs, chips,
  the view toggle) and typing a query are instant, as the code redraws in place; scrolling to Backup and the
  keyboard rising are Smart animate. Back reverses whatever brought the screen in.

  Flow starting points (FLOWS) and prototype settings: custom size 411 x 914 (Android Compact is 412 x 917).

FLOW OVERVIEW (page Flow overview)
  Swimlanes, one per flow (LANES): each screen a card (area colour strip, name, key), arrows labelled with the
  action. Solid arrows go forward, dashed ones go back. A dashed card is a screen drawn in another lane; a grey one
  is outside Attic. Colours are Canvas/flow/* variables added here; text is Doc/*, plus Doc/Flow label (added here).
"""
from __future__ import annotations

import math

from figbuild.kit import Doc, V, Area, SCREEN_W, SCREEN_H

REFS: dict[str, str] = {}

FAST_OUT_SLOW_IN = {"customBezier": {"x1": 0.4, "y1": 0.0, "x2": 0.2, "y2": 1.0}}
OVERLAY_SPRING = {"customSpring": {"mass": 1, "stiffness": 380, "damping": round(2 * math.sqrt(380), 2)}}

# kind -> (transition, duration ms, easing)
MOTION = {
    "tab": ({"type": "dissolve"}, 220, FAST_OUT_SLOW_IN),
    "detail": ({"type": "slideIn", "direction": "left"}, 300, FAST_OUT_SLOW_IN),
    "overlay": ({"type": "moveIn", "direction": "top"}, 300, OVERLAY_SPRING),
    "sheet": ({"type": "dissolve"}, 200, "easeOut"),
    "story": ({"type": "dissolve"}, 220, FAST_OUT_SLOW_IN),
    "smart": ({"type": "smartAnimate"}, 300, FAST_OUT_SLOW_IN),
    "instant": ({"type": "instant"}, 0, "linear"),
    "close": ({"type": "moveOut", "direction": "bottom"}, 300, OVERLAY_SPRING),
}

OVERLAYS = ("now_playing", "queue", "lyrics_", "open_with")
SHEETS = ("sheet_", "dialog_", "menu_", "snackbar", "sleep_timer_active")

# key -> (frame name, heading, paragraphs, buttons). A note with buttons (label, screen key) goes on in the flow, so it is
# drawn twice, "· Light" and "· Dark", and each copy leads to the screen in its own theme; one without buttons is a single
# frame whose Back returns to wherever it was opened from.
NOTES = {
    "style": ("Prototype note / Style switch",
              "Comfort and Expressive are separate files",
              ["Picking Expressive, in Settings or on the first run's Look step, redraws every screen of the app in "
               "the Expressive style at once. Nothing else changes: the library, the queue and the settings stay.",
               "This file draws Comfort only. To follow the same flow in the other style, open Attic Expressive.",
               "Code: settings.setUiStyle; MainActivity provides LocalUiStyle, and each screen draws its Comfort "
               "or Expressive composable from it."], []),
    "share": ("Prototype note / Share",
              "Share hands the file to Android",
              ["Share in a track menu sends the song's audio file to Android's share sheet, titled with the song's "
               "name. Share on a Recap story card sends the card as a 1080 x 1920 image (Recap share images).",
               "The system draws the sheet and lists the apps that can take the file; Attic draws nothing after the tap.",
               "If no app can take a song, or it isn't in the library, Attic says \"Couldn't share this song\".",
               "Code: ui/SongShare.kt and ui/ShareImage.kt, Intent.ACTION_SEND through Intent.createChooser."],
              [("No app can take it", "snackbar_share_failed")]),
    "files": ("Prototype note / File picker",
              "Backup goes through Android's file picker",
              ["Back up to a file opens Android's file picker to choose where attic-backup-<date>.json goes.",
               "Restore from a file opens it to choose the backup. A backup leads to the question; any other file "
               "to \"Couldn't restore\".",
               "The picker writes and reads through the Storage Access Framework, so Attic asks for no storage "
               "permission.",
               "Code: ui/model/SettingsModel.kt, ActivityResultContracts.CreateDocument and OpenDocument."],
              [("Save the file", "snackbar_backup_saved"), ("Pick a backup", "dialog_restore_confirm"),
               ("Pick another file", "dialog_restore_problem")]),
    "permission": ("Prototype note / Permission dialog",
                   "Android asks for access to music",
                   ["Allow access on the first run's access step raises Android's own permission dialog for music and "
                    "audio, then the one for notifications. Attic draws neither.",
                    "Allowed, the step shows both rows as allowed and Continue. Refused, it says audio access is off and "
                    "offers Open app permissions.",
                    "Code: ui/comfort/ComfortOnboarding.kt AccessStep, rememberLauncherForActivityResult with "
                    "RequestMultiplePermissions."],
                   [("Allow", "onboarding_access_granted"), ("Don't allow", "onboarding_access_refused")]),
    "openwith": ("Prototype note / Open with",
                 "Another app hands Attic an audio file",
                 ["In a file manager or a chat, Open with Attic on an audio file starts it playing and opens Now Playing "
                  "over whatever Attic last showed. A file that isn't in the library plays without a cover, artist or "
                  "lyrics.",
                  "If Attic can't read the file, it stays where it was and says \"Couldn't open this file\".",
                  "Code: playback/OpenWith.kt, the VIEW intent in MainActivity; ShellModel shows Now Playing."],
                 [("Open with Attic", "open_with"), ("A file Attic can't read", "snackbar_open_failed")]),
}
PSEUDO = {"settings_style_switch": "style", "onboarding_style_switch": "style"}

FLOWS = [
    ("onboarding_welcome", "First run",
     "Welcome, the look, access, the library scan, then Home."),
    ("home", "Home, album and Now Playing",
     "Home to an album; the mini player opens Now Playing, then the queue and lyrics and back. Playing a song only "
     "starts it: Now Playing opens from the mini player."),
    ("search_browse", "Search",
     "Tap the field, type, open a result."),
    ("library_list", "Library and filters",
     "The chips (Playlists, Albums, Artists, Songs), the grid view, and the collections they open."),
    ("settings", "Settings: theme and style",
     "The theme radios switch the prototype between Light and Dark; the style radios explain the Expressive file."),
    ("settings_backup", "Backup and restore",
     "Back up to a file or restore from one through Android's file picker, the question and the result."),
    ("recap", "Recap stories",
     "Play recap, tap through the ten cards, close back to Recap."),
    ("now_playing", "Sleep timer",
     "More on Now Playing, Sleep timer, pick a time; the message, then the running timer in the menu, and Off."),
    ("sheet_track_menu", "Share a song",
     "The track menu of a song on Home: Share hands the file over to Android's share sheet."),
    ("note:openwith", "Open with",
     "Another app opens an audio file in Attic: Now Playing without a cover, then the mini player when it closes."),
    ("playlist", "Playlists",
     "A playlist: add songs, reorder, a song's menu, rename and delete; Library's + names a new one."),
]


# ============================================================================== helpers
def _named(d: Doc, root: int, name: str, limit=50) -> list[int]:
    return [i for i in d.find(root, name=name, limit=limit) if d.get(i).get("name") == name]


def _first(d: Doc, root: int, *path: str) -> int | None:
    """The layer at `path` (names, each searched under the previous) under root, or None."""
    cur = root
    for name in path:
        hits = _named(d, cur, name)
        if not hits:
            return None
        cur = hits[0]
    return cur


def _kind(src: str, dst: str, lname: str) -> str:
    if lname.startswith(("Sub-tab:", "Chip:")) or lname == "View toggle":
        return "instant"
    if lname in ("Play button", "Play or pause"):
        return "smart"        # play and pause redraw in place
    if src.startswith(OVERLAYS) and lname == "Close" and not dst.startswith(OVERLAYS + SHEETS):
        return "close"        # an overlay closed onto the screen beneath it
    if lname.startswith("Tab:"):
        return "tab"
    if dst in PSEUDO or dst.startswith("note:"):
        return "sheet"
    if dst.startswith(OVERLAYS):
        return "overlay"
    if dst.startswith(SHEETS):
        return "sheet"
    if src.startswith("recap_story") or dst.startswith("recap_story"):
        return "story"
    if src.startswith("onboarding") and dst == "home":
        return "tab"
    if (src, dst) in (("settings", "settings_backup"), ("search_browse", "search_typing"), ("playlist", "playlist_reorder"),
                      ("playlist_reorder", "playlist")):
        return "smart"
    if (src, dst) == ("search_typing", "search_results"):
        return "instant"
    return "detail"


class Wiring:
    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.added: list[dict] = []          # every interaction, for the read-back
        self.skipped: list[str] = []
        self.wired_layers: set[int] = set()
        self.notes: dict[str, int] = {}

    # ---------------------------------------------------------------- one link, Light and Dark
    def dark_of(self, src: str, layer: int) -> int | None:
        s = self.d.screens[src]
        if layer == s["light"]:
            return s["dark"]
        return s["map"].get(layer)

    def note_frame(self, key: str, theme: str) -> int:
        n = self.notes[key]
        return n[theme] if isinstance(n, dict) else n

    def dest_of(self, dst: str, theme: str) -> int | None:
        if dst == "back":
            return None
        if dst in PSEUDO:
            return self.note_frame(PSEUDO[dst], theme)
        if dst.startswith("note:"):
            return self.note_frame(dst[5:], theme)
        return self.d.screens[dst][theme]

    def link(self, src: str, layer: int, dst: str, *, kind: str | None = None, label: str = "", trigger=None) -> bool:
        d = self.d
        if src not in d.screens:
            self.skipped.append(f"{src} -> {dst}: no screen {src!r}")
            return False
        if dst != "back" and dst not in PSEUDO and not dst.startswith("note:") and dst not in d.screens:
            self.skipped.append(f"{src} -> {dst}: no screen {dst!r}")
            return False
        lname = d.get(layer).get("name", "")
        kind = kind or ("back" if dst == "back" else _kind(src, dst, lname))
        for theme in ("light", "dark"):
            lay = layer if theme == "light" else self.dark_of(src, layer)
            if lay is None:
                if d.screens[src]["dark"]:
                    self.skipped.append(f"{src} -> {dst}: layer {lname!r} has no Dark twin (Light only)")
                continue
            dest = self.dest_of(dst, theme)
            self._add(lay, kind, dest, src=src, dst=dst, theme=theme, label=label or lname, trigger=trigger)
        self.wired_layers.add(layer)
        return True

    def _add(self, layer: int, kind: str, dest: int | None, *, src, dst, theme, label, actions=None, trigger=None):
        params = {"id": layer, "trigger": trigger or {"type": "onClick"}}
        if kind == "back":
            params["action"] = {"type": "back"}
        else:
            acts = list(actions or [])
            if dest is not None:
                acts.append({"type": "navigate", "dest": dest})
            params["actions"] = acts
            tr, ms, ease = MOTION[kind]
            params["transition"] = dict(tr)
            if tr["type"] != "instant":
                params["durationMs"] = ms
                params["easing"] = ease
        self.d.cmd("proto.interaction.add", **params)
        self.added.append({"layer": layer, "src": src, "dst": dst, "theme": theme, "dest": dest, "kind": kind,
                           "label": label, "modes": [a for a in (actions or [])]})

    def theme(self, src: str, pick: str):
        """A theme radio: set every Light/Dark collection's mode, and show the copy with that radio on."""
        d = self.d
        s = d.screens[src]
        row = _first(d, s["light"], f"Theme: {pick}")
        if row is None:
            self.skipped.append(f"{src}: no Theme: {pick} row")
            return
        mode = "Dark" if pick == "Dark" else "Light"      # Match phone: a phone in Light
        acts = [{"type": "setVariableMode", "collection": c, "mode": mode} for c in d.mode_collections]
        for theme, lay in (("light", row), ("dark", s["map"].get(row))):
            if lay is None:
                continue
            # From the Light copy, Dark moves to the Dark copy (its radio shows Dark); from the Dark copy, Light
            # and Match phone move to the Light copy. Picking the theme already shown only sets the mode.
            go = None
            if theme == "light" and mode == "Dark":
                go = s["dark"]
            elif theme == "dark" and mode == "Light":
                go = s["light"]
            self._add(lay, "sheet" if go else "instant", go, src=src, dst=f"theme {pick}", theme=theme,
                      label=f"Theme: {pick}", actions=acts)
        self.wired_layers.add(row)


# ============================================================================== notes on the Screens page
def _note_frame(d: Doc, sec, name, head, paras, x, y):
    f = d.frame(sec, name=name, x=x, y=y, w=SCREEN_W, h=SCREEN_H, layout="v", gap=20, pad=[96, 32, 48, 32],
                fill="Canvas/card", clip=True)
    d.text(f, "Prototype note", style="Doc/Label", color="Canvas/accent", name="Overline")
    hd = d.text(f, head, style="Doc/Heading", color="Canvas/label", name="Heading", width=SCREEN_W - 64, textAutoResize="height")
    d.set(hd, sizingH="fill", textAutoResize="height")
    for j, p in enumerate(paras):
        tp = d.text(f, p, style="Doc/Code" if p.startswith("Code:") else "Doc/Body",
                    color="Canvas/label-muted" if p.startswith("Code:") else "Canvas/label",
                    name=f"Paragraph {j + 1}", width=SCREEN_W - 64, textAutoResize="height")
        # The style is applied after the text is made; size it again so the column doesn't overlap a long paragraph.
        d.set(tp, sizingH="fill", textAutoResize="height")
    sp = d.frame(f, name="Space", w=SCREEN_W - 64, h=8)
    d.set(sp, sizingV="fill")
    return f


def _note_button(d: Doc, parent, label, *, primary=True):
    b = d.frame(parent, name=f"Button: {label}", layout="h", gap=8, pad=[14, 24, 14, 24], radius=999, cross="center",
                fill="Canvas/accent" if primary else "Canvas/section", sizing=("hug", "hug"))
    d.text(b, label, style="Doc/Label", color="Canvas/card" if primary else "Canvas/label", name="Label")
    return b


def _notes(d: Doc, w: Wiring):
    d.page("Screens")
    y0 = Area._next_y.get((id(d), "Screens"), 0)
    pad, title_h, gap = Area.PAD, Area.TITLE, Area.GAP_X
    frames = sum(2 if go else 1 for _, (_, _, _, go) in NOTES.items())
    sec_w = pad * 2 + frames * SCREEN_W + (frames - 1) * gap
    sec = d.section("Prototype notes", x=0, y=y0, w=max(sec_w, 1500), h=pad * 2 + title_h + SCREEN_H)
    d.text(sec, "Prototype notes", style="Doc/Section title", color="Canvas/label", name="Title: Prototype notes",
           x=pad, y=pad - 24)
    intro = d.text(sec, "Where a flow leaves this file or the app, the prototype stops at one of these frames. They are "
                "notes, not screens: Back returns to where you were. A note that the flow goes on from is drawn twice, "
                "Light and Dark, so its buttons lead to the screen in the theme you came from.", style="Doc/Body",
           color="Canvas/label", name="Note", x=pad, y=pad + 32, width=min(1200, sec_w - 2 * pad), textAutoResize="height")
    d.set(intro, textAutoResize="height")
    ib = d.get(intro)
    top = max(pad + title_h, ib["y"] + ib["height"] + 40)      # frames start under the note, however long it runs
    sec_h = top + SCREEN_H + pad
    d.set(sec, height=sec_h)
    col = 0
    for key, (name, head, paras, go) in NOTES.items():
        themes = ("light", "dark") if go else (None,)
        made = {}
        for theme in themes:
            nm = f"{name} · {theme.capitalize()}" if theme else name
            f = _note_frame(d, sec, nm, head, paras, pad + col * (SCREEN_W + gap), top)
            col += 1
            row = d.frame(f, name="Buttons", layout="h", wrap=True, gap=10, crossGap=10, sizing=("fill", "hug"))
            for label, dst in go:
                b = _note_button(d, row, label)
                if dst in d.screens:
                    w._add(b, MOTION_OF.get(dst, "sheet"), d.screens[dst][theme], src=f"note:{key}", dst=dst, theme=theme,
                           label=label)
                else:
                    w.skipped.append(f"note:{key} -> {dst}: no screen {dst!r}")
            back = _note_button(d, row, "Back", primary=not go)
            d.cmd("proto.interaction.add", id=back, trigger={"type": "onClick"}, action={"type": "back"})
            w.added.append({"layer": back, "src": f"note:{key}", "dst": "back", "theme": theme or "-", "dest": None,
                            "kind": "back", "label": "Back", "modes": []})
            made[theme] = f
        w.notes[key] = made[None] if None in made else made
    Area._next_y[(id(d), "Screens")] = y0 + sec_h + 200


# How a note's button moves on: the destination decides, as in _kind.
MOTION_OF = {"open_with": "overlay", "onboarding_access_granted": "detail", "onboarding_access_refused": "detail"}


# ============================================================================== wiring
def _inherit_light(d: Doc):
    """Light frames take the default mode (Light) instead of setting it, so a setVariableMode action reaches them."""
    for s in d.screens.values():
        lid = s["light"]
        for c in d.mode_collections:
            try:
                now = d.cmd("proto.evaluate", expr={"mode": {"collection": c}}, id=lid).get("value")
            except Exception:
                continue
            if now == "Light":
                d.set_mode(lid, c, None)


def _extra(d: Doc, w: Wiring, trig: dict):
    S = d.screens
    have = set(trig)

    def add(src, layer, dst, kind=None):
        if src not in S or layer is None:
            if src in S:
                w.skipped.append(f"{src} -> {dst}: trigger layer not found")
            return
        if layer in w.wired_layers:
            return
        if (src, dst) in have and trig[(src, dst)] == layer:
            return
        w.link(src, layer, dst, kind=kind)

    # The mini player opens Now Playing from every screen that has the dock with a song (AppRoot.kt MiniPlayer onOpen);
    # nothing else in the app does, apart from Open with. A mini player wired already (the opened file's) is skipped.
    for key, s in S.items():
        if key.startswith(("sheet_", "dialog_", "menu_", "sleep_timer")) or (key, "now_playing") in have:
            continue
        mini = _first(d, s["light"], "Dock", "Mini player")
        if mini is not None:
            add(key, mini, "now_playing", "overlay")
    # Settings from each root's gear.
    for key in ("home", "home_full", "home_recap_year", "home_albums", "home_artists", "home_folders", "search_browse", "search_results",
                "search_no_results", "library_list", "library_grid", "library_playlists", "library_albums",
                "library_artists", "library_songs"):
        if key in S and _first(d, S[key]["light"], "Settings") is not None:
            add(key, _first(d, S[key]["light"], "Settings"), "settings", "detail")
    # Recap from Home. The page opens on its loading state (ComfortRecap: data == null) and shows the period once
    # its numbers are in.
    recap_in = "recap_loading" if "recap_loading" in S else "recap"
    for key in ("home", "home_full", "home_recap_year"):
        if key in S:
            add(key, _first(d, S[key]["light"], "Recap entry"), recap_in, "detail")
    if "recap_loading" in S and "recap" in S:
        w.link("recap_loading", S["recap_loading"]["light"], "recap", kind="smart", label="After 1 s",
               trigger={"type": "afterDelay", "ms": 1000})
    # Playing a song or an album only starts playback (AppRoot.kt: the mini player alone opens Now Playing), so Play
    # and the songs on the album link nowhere.
    # A song's track menu on Home ("Track menu / From a song" is Porch Light's, over Home). Porch Light is in Instant
    # mixes, whose rows end in the radio button, and the code opens a row's menu on a long press. The prototype has no
    # long press, so a tap on the row stands in for it; Home's annotation says so.
    if "home" in S and "sheet_track_menu" in S:
        title = None
        menu = _first(d, S["sheet_track_menu"]["light"], "Track menu")
        if menu is not None:
            t = _first(d, menu, "Title")
            title = d.get(t).get("characters") if t else None
        row = _first(d, S["home"]["light"], f"Row: {title}") if title else None
        if row is not None:
            add("home", row, "sheet_track_menu", "sheet")
            for fid in (S["home"]["light"], S["home"]["dark"]):
                d.annotate(fid, f"In the prototype a tap on {title} in Instant mixes opens its track menu, standing in for "
                                "the long press the code uses (SongRow onLongClick). In the app a tap plays the song, and "
                                "the row's end button starts radio from it.", "Note")
    # Share, from every track menu that has it.
    for key in ("sheet_track_menu", "sheet_track_menu_playlist", "sheet_track_menu_now_playing"):
        if key in S:
            add(key, _first(d, S[key]["light"], "Row: Share"), "note:share", "sheet")
    # States one step on from another: Clear empties Up next; Download lyrics downloads, then shows them; the
    # Settings button downloads lyrics for every song; typing on the keyboard a word nothing matches.
    def more(src, path, dst, kind=None, trigger=None):
        if src in S and dst in S:
            lay = _first(d, S[src]["light"], *path)
            if lay is None:
                w.skipped.append(f"{src} -> {dst}: no layer {path}")
            elif lay not in w.wired_layers:
                w.link(src, lay, dst, kind=kind, trigger=trigger)
    more("queue", ("Clear",), "queue_empty", "instant")
    for key in ("lyrics_not_downloaded", "lyrics_none", "lyrics_instrumental"):
        more(key, ("Download lyrics",), "lyrics_downloading", "instant")
    if "lyrics_downloading" in S and "lyrics_synced" in S:
        w.link("lyrics_downloading", S["lyrics_downloading"]["light"], "lyrics_synced", kind="smart", label="After 2 s",
               trigger={"type": "afterDelay", "ms": 2000})
    more("settings", ("Lyrics: saved", "Text button"), "settings_lyrics_download", "instant")
    more("search_typing", ("Keyboard (system)",), "search_no_results", "instant")
    # A message goes after about four seconds (ToastHost), leaving the screen it was shown over.
    for key, dst in (("snackbar", "home"), ("snackbar_over_player", "now_playing"), ("snackbar_share_failed", "home"),
                     ("snackbar_open_failed", "home"), ("snackbar_backup_saved", "settings_backup"),
                     ("snackbar_sleep_timer_set", "now_playing"), ("snackbar_sleep_timer_off", "now_playing")):
        if key in S and dst in S:
            w.link(key, S[key]["light"], dst, kind="sheet", label="After 4 s", trigger={"type": "afterDelay", "ms": 4000})
    # Backup: the file picker, and the dialogs' buttons.
    if "settings_backup" in S:
        add("settings_backup", _first(d, S["settings_backup"]["light"], "Back up to a file"), "note:files", "sheet")
    if "dialog_restore_confirm" in S:
        add("dialog_restore_confirm", _first(d, S["dialog_restore_confirm"]["light"], "Dismiss"), "back")
    for key in ("dialog_restore_result", "dialog_restore_problem"):
        if key in S and "settings_backup" in S:
            add(key, _first(d, S[key]["light"], "Confirm"), "settings_backup", "sheet")
    # Theme radios.
    for key in ("settings", "settings_full", "onboarding_look"):
        if key in S:
            for pick in ("Match phone", "Dark", "Light"):
                w.theme(key, pick)
    # Every Back button nobody wired yet goes back (not under a sheet or dialog: the scrim covers it there).
    for key, s in S.items():
        if key.startswith(("sheet_", "dialog_", "menu_", "sleep_timer", "snackbar", "recap_share", "widget_", "system_")):
            continue
        for b in _named(d, s["light"], "Back"):
            if b not in w.wired_layers:
                w.link(key, b, "back")


def _verify(d: Doc, w: Wiring) -> list[str]:
    """Read every interaction back; check each navigate goes to the intended frame, by name."""
    problems = []
    by_layer: dict[int, list] = {}
    for a in w.added:
        by_layer.setdefault(a["layer"], []).append(a)
    for layer, wants in by_layer.items():
        got = d.tool("get_node", id=layer, fields=["interactions"]).get("interactions") or []
        dests = []
        backs = 0
        modes = 0
        for it in got:
            for act in it.get("actions", []):
                if act.get("type") == "navigate":
                    dests.append(act.get("dest"))
                elif act.get("type") == "back":
                    backs += 1
                elif act.get("type") == "setVariableMode":
                    modes += 1
        for a in wants:
            if a["kind"] == "back":
                if not backs:
                    problems.append(f"{a['src']} {a['theme']} {a['label']}: no back action")
                continue
            if a["dest"] is not None:
                if a["dest"] not in dests:
                    problems.append(f"{a['src']} {a['theme']} {a['label']}: no navigate to {a['dest']}")
                    continue
                name = d.get(a["dest"]).get("name", "")
                nk = PSEUDO.get(a["dst"]) or (a["dst"][5:] if a["dst"].startswith("note:") else None)
                if nk:
                    want = NOTES[nk][0] + (f" · {a['theme'].capitalize()}" if NOTES[nk][3] else "")
                elif a["dst"].startswith("theme"):
                    # Light copy, Dark picked: the Dark copy; Dark copy, Light picked: the Light copy.
                    want = f"{d.screens[a['src']]['name']} · {'Dark' if a['theme'] == 'light' else 'Light'}"
                else:
                    want = f"{d.screens[a['dst']]['name']} · {a['theme'].capitalize()}"
                if name != want:
                    problems.append(f"{a['src']} {a['label']}: goes to {name!r}, wanted {want!r}")
            if a["modes"] and modes < len(a["modes"]):
                problems.append(f"{a['src']} {a['label']}: {modes} setVariableMode actions, wanted {len(a['modes'])}")
    return problems


def _flows(d: Doc, notes: dict):
    d.page("Screens")
    made = []
    for key, name, desc in FLOWS:
        if key.startswith("note:"):
            n = notes.get(key[5:])
            if n is None:
                continue
            start = n["light"] if isinstance(n, dict) else n
        elif key not in d.screens:
            continue
        else:
            start = d.screens[key]["light"]
        if any(m[0] == start for m in made):
            # One starting point per frame: a second flow from the same screen names itself after both.
            prev = next(m for m in made if m[0] == start)
            name = f"{prev[1]}; {name}"
            desc = f"{prev[2]} Or: {desc[0].lower()}{desc[1:]}"
            d.cmd("proto.flow.rename", start=start, name=name, description=desc)
            made = [m for m in made if m[0] != start] + [(start, name, desc)]
            continue
        d.cmd("proto.flow.add", start=start, name=name)
        d.cmd("proto.flow.rename", start=start, name=name, description=desc)
        made.append((start, name, desc))
    d.cmd("proto.settings", device="Custom size", width=SCREEN_W, height=SCREEN_H)
    return made


# ============================================================================== Flow overview
AREA_COLOURS = [
    ("flow/launch", "#2563EB", "First run and launch"),
    ("flow/tabs", "#16A34A", "Tab roots: Home, Search, Library"),
    ("flow/collections", "#0891B2", "Album, artist, playlist, folder, Favourites"),
    ("flow/player", "#DC2626", "Now Playing, queue, lyrics"),
    ("flow/sheets", "#EA580C", "Sheets, dialogs, menus"),
    ("flow/settings", "#4F46E5", "Settings, backup, licences"),
    ("flow/recap", "#CA8A04", "Recap and its story"),
    ("flow/outside", "#71717A", "Outside Attic: Android or the other file"),
]


def _area_of(key: str) -> str:
    if key.startswith(("onboarding", "system_splash", "permission", "loading")):
        return "launch"
    if key.startswith(("home", "search", "library", "mini_player", "empty_library")):
        return "tabs"
    if key.startswith(("album", "artist", "playlist", "folder", "favourites", "item_missing")):
        return "collections"
    if key.startswith(OVERLAYS) or key == "open_with":
        return "player"
    if key.startswith(SHEETS) or key.startswith("sleep_timer"):
        return "sheets"
    if key.startswith(("settings", "licence", "dialog_restore")):
        return "settings"
    if key.startswith("recap"):
        return "recap"
    return "outside"


# A lane: (title, description, nodes, edges). Node: (id, keys or None, label or None, col, row, kind)
# kind: "screen" | "ref" (drawn in another lane) | "outside" | "note". Edge: (from, to, label, style) where style is
# "go" (solid) or "back" (dashed, drawn under the cards).
LANES = [
    ("First run", "Once per install. Steps slide in from the right. Android asks for access in its own dialog.", [
        ("splash", ["system_splash"], None, 0, 0, "screen"),
        ("welcome", ["onboarding_welcome"], None, 1, 0, "screen"),
        ("look", ["onboarding_look"], None, 2, 0, "screen"),
        ("access", ["onboarding_access"], None, 3, 0, "screen"),
        ("ask", None, "Android permission dialog", 4, 0, "outside"),
        ("granted", ["onboarding_access_granted"], None, 5, 0, "screen"),
        ("refused", ["onboarding_access_refused"], None, 4, 1, "screen"),
        ("scan", ["onboarding_library_scanning"], None, 6, 0, "screen"),
        ("found", ["onboarding_library_found"], None, 7, 0, "screen"),
        ("none", ["onboarding_library_empty"], None, 7, 1, "screen"),
        ("home", ["home"], None, 8, 0, "ref"),
        ("nosongs", ["empty_library"], None, 8, 1, "screen"),
    ], [
        ("splash", "welcome", "First launch", "go"),
        ("welcome", "look", "Get started", "go"),
        ("look", "access", "Continue", "go"),
        ("access", "ask", "Allow access", "go"),
        ("ask", "granted", "Allow", "go"),
        ("ask", "refused", "Don't allow", "go"),
        ("refused", "granted", "Open app permissions", "go"),
        ("granted", "scan", "Continue", "go"),
        ("scan", "found", "Songs found", "go"),
        ("scan", "none", "Nothing found", "go"),
        ("found", "home", "Start listening", "go"),
        ("none", "nosongs", "Start listening", "go"),
    ]),
    ("Tabs", "Home, Search and Library are tab roots in the dock; switching tabs dissolves.", [
        ("home", ["home"], None, 0, 0, "screen"),
        ("search", ["search_browse"], None, 1, 0, "screen"),
        ("library", ["library_list"], None, 2, 0, "screen"),
        ("grid", ["library_grid"], None, 3, 0, "screen"),
        ("subtabs", ["home_albums", "home_artists", "home_folders"], "Home / Albums, Artists, Folders", 0, 1, "screen"),
        ("filters", ["library_playlists", "library_albums", "library_artists", "library_songs"],
         "Library / Playlists, Albums, Artists, Songs", 2, 1, "screen"),
        ("paused", ["mini_player_paused"], None, 1, 1, "screen"),
    ], [
        ("home", "search", "Tab: Search", "go"),
        ("search", "library", "Tab: Library", "go"),
        ("search", "home", "Tab: Home", "back"),
        ("library", "search", "Tab: Search", "back"),
        ("library", "grid", "View toggle", "go"),
        ("home", "subtabs", "Sub-tabs", "go"),
        ("library", "filters", "Chips", "go"),
        ("home", "paused", "Pause in the mini player", "go"),
    ]),
    ("Collections", "Detail screens slide in from the right; Back slides them out. Playing a song only starts it.", [
        ("home", ["home"], None, 0, 0, "ref"),
        ("album", ["album"], None, 1, 0, "screen"),
        ("np", ["now_playing"], None, 2, 0, "ref"),
        ("artist", ["artist"], None, 1, 1, "screen"),
        ("folder", ["folder"], None, 1, 2, "screen"),
        ("fav", ["favourites"], None, 1, 3, "screen"),
    ], [
        ("home", "album", "Tap an album", "go"),
        ("album", "home", "Back", "back"),
        ("album", "np", "Tap the mini player", "go"),
        ("home", "artist", "Tap an artist", "go"),
        ("home", "folder", "Tap a folder", "go"),
        ("home", "fav", "Tap Favourites", "go"),
    ]),
    ("Playlists", "A playlist's tools and menus. Sheets and dialogs dissolve in over the playlist.", [
        ("library", ["library_list"], None, 0, 0, "ref"),
        ("playlist", ["playlist"], None, 1, 0, "screen"),
        ("add", ["sheet_add_songs"], None, 2, 0, "screen"),
        ("reorder", ["playlist_reorder"], None, 2, 1, "screen"),
        ("song", ["sheet_track_menu_playlist"], None, 2, 2, "screen"),
        ("artist", ["artist"], None, 3, 2, "ref"),
        ("menu", ["menu_playlist_overflow"], None, 2, 3, "screen"),
        ("rename", ["dialog_rename_playlist"], None, 3, 3, "screen"),
        ("delete", ["dialog_delete_playlist"], None, 3, 4, "screen"),
        ("name", ["dialog_name_playlist"], None, 1, 5, "screen"),
        ("empty", ["playlist_empty"], None, 2, 5, "screen"),
    ], [
        ("library", "playlist", "Tap a playlist", "go"),
        ("playlist", "add", "Add songs", "go"),
        ("playlist", "reorder", "Reorder songs", "go"),
        ("reorder", "playlist", "Done reordering", "back"),
        ("playlist", "song", "More on a song", "go"),
        ("song", "artist", "Go to artist", "go"),
        ("playlist", "menu", "More", "go"),
        ("menu", "rename", "Rename", "go"),
        ("menu", "delete", "Delete playlist", "go"),
        ("delete", "library", "Delete playlist", "back"),
        ("library", "name", "+ in the tools row", "go"),
        ("name", "empty", "Create", "go"),
    ]),
    ("Search", "The field opens the keyboard; results update as you type.", [
        ("search", ["search_browse"], None, 0, 0, "ref"),
        ("typing", ["search_typing"], None, 1, 0, "screen"),
        ("results", ["search_results"], None, 2, 0, "screen"),
        ("artist", ["artist"], None, 3, 0, "ref"),
        ("none", ["search_no_results"], None, 2, 1, "screen"),
    ], [
        ("search", "typing", "Tap the field", "go"),
        ("typing", "results", "Type", "go"),
        ("typing", "none", "Type a word nothing matches", "go"),
        ("results", "artist", "Tap an artist", "go"),
    ]),
    ("Now Playing", "Now Playing, the queue and lyrics rise from the bottom and close downwards. Sheets dissolve in.", [
        ("mini", None, "Any screen with the dock", 0, 0, "ref"),
        ("np", ["now_playing"], None, 1, 0, "screen"),
        ("queue", ["queue"], None, 2, 0, "screen"),
        ("lyrics", ["lyrics_synced"], None, 2, 1, "screen"),
        ("menu", ["sheet_track_menu_now_playing"], None, 2, 2, "screen"),
        ("sleep", ["sheet_sleep_timer"], None, 3, 2, "screen"),
        ("set", ["snackbar_sleep_timer_set"], None, 4, 2, "screen"),
        ("running", ["sleep_timer_active"], None, 5, 2, "screen"),
        ("picker", ["sheet_playlist_picker"], None, 3, 3, "screen"),
        ("new", ["dialog_new_playlist"], None, 4, 3, "screen"),
        ("paused", ["now_playing_paused_radio"], None, 1, 4, "screen"),
    ], [
        ("mini", "np", "Tap the mini player", "go"),
        ("np", "mini", "Swipe down or Close", "back"),
        ("np", "queue", "Tap Queue", "go"),
        ("queue", "np", "Back", "back"),
        ("np", "lyrics", "Tap the lyric", "go"),
        ("np", "menu", "Tap More", "go"),
        ("menu", "sleep", "Sleep timer", "go"),
        ("sleep", "set", "Pick 30 minutes", "go"),
        ("set", "running", "More", "go"),
        ("menu", "picker", "Add to playlist", "go"),
        ("picker", "new", "New playlist", "go"),
        ("np", "paused", "Pause", "go"),
    ]),
    ("Open with", "Another app hands Attic a file: it plays at once and Now Playing opens over whatever Attic showed.", [
        ("other", None, "A file manager or chat (Prototype note / Open with)", 0, 0, "outside"),
        ("np", ["open_with"], None, 1, 0, "screen"),
        ("menu", ["sheet_track_menu_open_with"], None, 2, 0, "screen"),
        ("mini", ["mini_player_open_with"], None, 2, 1, "screen"),
        ("failed", ["snackbar_open_failed"], None, 1, 2, "screen"),
    ], [
        ("other", "np", "Open with Attic", "go"),
        ("np", "menu", "More", "go"),
        ("np", "mini", "Close", "go"),
        ("mini", "np", "Tap the mini player", "back"),
        ("other", "failed", "A file Attic can't read", "go"),
    ]),
    ("Share", "Share hands the song's file to Android; Attic draws nothing after the tap.", [
        ("home", ["home"], None, 0, 0, "ref"),
        ("menu", ["sheet_track_menu"], None, 1, 0, "screen"),
        ("system", None, "Android share sheet", 2, 0, "outside"),
        ("failed", ["snackbar_share_failed"], None, 3, 0, "screen"),
        ("queued", ["snackbar"], None, 2, 1, "screen"),
    ], [
        ("home", "menu", "Tap Porch Light (long press)", "go"),
        ("menu", "home", "Tap the scrim", "back"),
        ("menu", "system", "Share", "go"),
        ("system", "failed", "No app can take it", "go"),
        ("menu", "queued", "Add to queue", "go"),
    ]),
    ("Settings", "Settings opens from the gear on Home, Search and Library. Theme switches in place.", [
        ("home", ["home"], "Home, Search or Library", 0, 0, "ref"),
        ("settings", ["settings"], None, 1, 0, "screen"),
        ("dark", ["settings"], "Settings / Top, Dark copy", 2, 0, "screen"),
        ("style", None, "Attic Expressive (the other file)", 2, 1, "outside"),
        ("licences", ["licences"], None, 2, 2, "screen"),
        ("text", ["licence_text"], None, 3, 2, "screen"),
    ], [
        ("home", "settings", "Tap the gear", "go"),
        ("settings", "dark", "Theme: Dark", "go"),
        ("dark", "settings", "Theme: Light", "back"),
        ("settings", "style", "Style: Expressive", "go"),
        ("settings", "licences", "Open-source licences", "go"),
        ("licences", "text", "Tap a notice", "go"),
    ]),
    ("Backup and restore", "Both go through Android's file picker; restoring asks first, then reports.", [
        ("settings", ["settings"], None, 0, 0, "ref"),
        ("backup", ["settings_backup"], None, 1, 0, "screen"),
        ("picker", None, "Android file picker", 2, 0, "outside"),
        ("saved", ["snackbar_backup_saved"], None, 3, 0, "screen"),
        ("confirm", ["dialog_restore_confirm"], None, 3, 1, "screen"),
        ("done", ["dialog_restore_result"], None, 4, 1, "screen"),
        ("problem", ["dialog_restore_problem"], None, 3, 2, "screen"),
    ], [
        ("settings", "backup", "Scroll to Backup", "go"),
        ("backup", "picker", "Back up or restore", "go"),
        ("picker", "saved", "Save the file", "go"),
        ("picker", "confirm", "Pick a backup", "go"),
        ("confirm", "done", "Restore", "go"),
        ("picker", "problem", "Pick another file", "go"),
    ]),
    ("Recap", "The story covers everything and crossfades from card to card.", [
        ("home", ["home"], None, 0, 0, "ref"),
        ("year", ["home_recap_year"], None, 0, 1, "screen"),
        ("loading", ["recap_loading"], None, 1, 0, "screen"),
        ("recap", ["recap"], None, 2, 0, "screen"),
        ("intro", ["recap_story_intro"], None, 3, 0, "screen"),
        ("minutes", ["recap_story_minutes"], None, 4, 0, "screen"),
        ("middle", ["recap_story_top_song", "recap_story_top_songs", "recap_story_top_artist",
                    "recap_story_top_artists", "recap_story_languages", "recap_story_listening_clock",
                    "recap_story_highlights"], "Recap story / 3 to 9", 5, 0, "screen"),
        ("summary", ["recap_story_summary"], None, 6, 0, "screen"),
        ("share", None, "Android share sheet", 7, 0, "outside"),
        ("empty", ["recap_empty"], None, 2, 1, "screen"),
    ], [
        ("home", "loading", "Tap Recap", "go"),
        ("year", "loading", "Tap Recap (from 24 December)", "go"),
        ("loading", "recap", "Numbers load", "go"),
        ("recap", "intro", "Play recap or a card", "go"),
        ("intro", "recap", "Close (any card)", "back"),
        ("intro", "minutes", "Tap the card", "go"),
        ("minutes", "middle", "Tap the card", "go"),
        ("middle", "summary", "Tap the card", "go"),
        ("summary", "share", "Share (any card)", "go"),
        ("recap", "empty", "This week", "go"),
    ]),
]

CARD_W, COL_GAP, ROW_PITCH = 340, 340, 230
LANE_HEAD = 560          # the lane's title column
LANE_PAD = 64


def _canvas_vars(d: Doc):
    have = {v["name"] for v in d.cmd("variable.list")["variables"]}
    for name, hexv, desc in AREA_COLOURS:
        if name not in have:
            d.variable("Canvas", name, "color", value=hexv, description=f"Flow overview: {desc}")
    if "Doc/Flow label" not in d.text_styles:
        d.text_style("Doc/Flow label", family="Google Sans Flex", size=22, line=28, weight=600,
                     description="Action labels on the Flow overview's arrows")


def _card(d: Doc, parent, nid, keys, label, kind, x, y) -> int:
    area = "outside" if kind == "outside" else _area_of(keys[0]) if keys else "tabs"
    if kind == "note":
        area = "outside"
    name = label or (d.screens[keys[0]]["name"] if keys and keys[0] in d.screens else (keys or ["?"])[0])
    card = d.frame(parent, name=f"Card: {name}", x=x, y=y, w=CARD_W, layout="v", gap=0, radius=20,
                   fill="Canvas/card" if kind != "ref" else None, clip=True, sizing=("fixed", "hug"),
                   stroke="Canvas/divider" if kind == "screen" else "Canvas/label-muted",
                   stroke_w=2 if kind != "screen" else 1)
    if kind in ("ref", "outside"):
        d.set(card, dashes=[8, 6])
    strip = d.rect(card, name="Area colour", w=CARD_W, h=10, fill=f"Canvas/flow/{area}")
    d.set(strip, sizingH="fill")
    body = d.frame(card, name="Card text", layout="v", gap=6, pad=[18, 20, 18, 20], sizing=("fill", "hug"))
    t = d.text(body, name, style="Doc/Heading", color="Canvas/label", name="Name")
    d.set(t, sizingH="fill", textAutoResize="height")
    if kind == "outside":
        sub = "Outside Attic"
    elif kind == "ref":
        sub = "Drawn in another lane" if keys else "Mini player in the dock"
    else:
        sub = "Key: " + (", ".join(keys) if len(keys) <= 2 else f"{keys[0]} … {keys[-1]} ({len(keys)} screens)")
    s = d.text(body, sub, style="Doc/Body", color="Canvas/label-muted", name="Key")
    d.set(s, sizingH="fill", textAutoResize="height")
    return card


def _pill(d: Doc, parent, text, cx, cy, *, bg="Canvas/section", muted=False) -> int:
    p = d.frame(parent, name=f"Action: {text}", layout="h", pad=[2, 10, 2, 10], fill=bg, radius=8,
                sizing=("hug", "hug"))
    d.text(p, text, style="Doc/Flow label", color="Canvas/label-muted" if muted else "Canvas/label", name="Label")
    g = d.get(p)
    d.set(p, x=round(cx - g["width"] / 2), y=round(cy - g["height"] / 2))
    return p


def _arrow(d: Doc, parent, pts, *, back=False, name="Arrow") -> int:
    r = 14
    path = [f"M {pts[0][0]:.1f} {pts[0][1]:.1f}"]
    for i in range(1, len(pts) - 1):
        (x0, y0), (x1, y1), (x2, y2) = pts[i - 1], pts[i], pts[i + 1]
        def toward(ax, ay, bx, by, dist):
            L = math.hypot(bx - ax, by - ay) or 1
            k = min(dist, L / 2) / L
            return ax + (bx - ax) * k, ay + (by - ay) * k
        a = toward(x1, y1, x0, y0, r)
        b = toward(x1, y1, x2, y2, r)
        path.append(f"L {a[0]:.1f} {a[1]:.1f} Q {x1:.1f} {y1:.1f} {b[0]:.1f} {b[1]:.1f}")
    path.append(f"L {pts[-1][0]:.1f} {pts[-1][1]:.1f}")
    # A vector's x/y move the path's own origin, so absolute path coordinates go in at 0, 0.
    v = d.vector(parent, name=name, path=" ".join(path), x=0, y=0,
                 stroke="Canvas/label-muted" if back else "Canvas/accent", stroke_w=2 if back else 3,
                 strokeJoin="round", endCap="triangleArrow")
    if back:
        d.set(v, dashes=[10, 8])
    return v


def _lane(d: Doc, title, desc, nodes, edges, y) -> int:
    cols = max(n[3] for n in nodes) + 1
    rows = max(n[4] for n in nodes) + 1
    w = LANE_HEAD + cols * CARD_W + (cols - 1) * COL_GAP + LANE_PAD
    lane = d.frame(None, name=f"Lane: {title}", x=0, y=y, w=w, h=200, fill="Canvas/section", radius=40)
    d.text(lane, title, style="Doc/Section title", color="Canvas/label", name="Lane title", x=LANE_PAD, y=LANE_PAD)
    d.text(lane, desc, style="Doc/Body", color="Canvas/label", name="Lane note", x=LANE_PAD, y=LANE_PAD + 72,
           width=LANE_HEAD - LANE_PAD - 60, textAutoResize="height")
    box = {}
    top = LANE_PAD
    for nid, keys, label, c, r, kind in nodes:
        x = LANE_HEAD + c * (CARD_W + COL_GAP)
        cid = _card(d, lane, nid, keys, label, kind, x, top + r * ROW_PITCH)
        g = d.get(cid)
        box[nid] = (x, top + r * ROW_PITCH, g["width"], g["height"], c, r)
    # Arrows behind cards: create, then send to the back.
    made = []
    lowest = max(b[1] + b[3] for b in box.values())
    for a, b, label, style in edges:
        ax, ay, aw, ah, ac, ar = box[a]
        bx, by, bw, bh, bc, br = box[b]
        if style == "back":
            # Under both cards: down from the source, across, up into the target.
            base = max(ay + ah, by + bh) + 46
            sx, tx = ax + aw / 2 - 40, bx + bw / 2 + 40
            pts = [(sx, ay + ah), (sx, base), (tx, base), (tx, by + bh + 2)]
            made.append(_arrow(d, lane, pts, back=True, name=f"Back: {a} to {b}"))
            made.append(_pill(d, lane, label, (sx + tx) / 2, base, muted=True))
            lowest = max(lowest, base + 24)
            continue
        if bc == ac and br > ar:
            # Down the card's left side, clear of the dashed back arrows that leave from the middle.
            sx = ax + 64
            pts = [(sx, ay + ah), (sx, by - 2)]
            made.append(_arrow(d, lane, pts, name=f"Go: {a} to {b}"))
            made.append(_pill(d, lane, label, sx, by - 34))
            continue
        sy = ay + min(ah / 2, 40) if br != ar else ay + min(ah, bh) / 2
        ty = by + min(bh / 2, 40) if br != ar else sy
        sx, tx = ax + aw, bx - 2
        if abs(sy - ty) < 1:
            pts = [(sx, sy), (tx, ty)]
            cx = (sx + tx) / 2
        else:
            x1 = sx + 44
            pts = [(sx, sy), (x1, sy), (x1, ty), (tx, ty)]
            cx = (x1 + tx) / 2
        made.append(_arrow(d, lane, pts, name=f"Go: {a} to {b}"))
        made.append(_pill(d, lane, label, cx, ty))
    note_h = d.get(d.child(lane, "Lane note"))["height"]
    d.set(lane, height=max(lowest + LANE_PAD, LANE_PAD + 72 + note_h + LANE_PAD))
    return lane


def _legend(d: Doc, x, y):
    lg = d.frame(None, name="Legend", x=x, y=y, layout="h", gap=48, pad=[28, 40, 28, 40], fill="Canvas/card",
                 radius=24, cross="center", sizing=("hug", "hug"), stroke="Canvas/divider", stroke_w=1)
    for name, hexv, desc in AREA_COLOURS:
        it = d.frame(lg, name=f"Key: {desc}", layout="h", gap=12, cross="center", sizing=("hug", "hug"))
        d.rect(it, name="Swatch", w=20, h=20, radius=6, fill=f"Canvas/{name}")
        d.text(it, desc, style="Doc/Label", color="Canvas/label", name="Label")
    for back, desc in ((False, "Forward: navigate"), (True, "Back or close")):
        it = d.frame(lg, name=f"Key: {desc}", layout="h", gap=12, cross="center", sizing=("hug", "hug"))
        holder = d.frame(it, name="Arrow sample", w=64, h=12)
        _arrow(d, holder, [(0, 6), (62, 6)], back=back, name="Back arrow" if back else "Forward arrow")
        d.text(it, desc, style="Doc/Label", color="Canvas/label", name="Label")
    return lg


def _overview(d: Doc):
    d.page("Flow overview")
    d.text(None, "Flow overview", style="Doc/Page title", color="Canvas/label", name="Page title", x=0, y=0)
    d.text(None, "How the screens connect. One lane per flow; each card is a screen on the Screens page (its name, "
                 "and its key: the snake-case id the code's reference renders and this file's build use), coloured by area, and each arrow names what the person does. The prototype on the "
                 "Screens page follows the same links, with a starting point per flow. A dashed card is drawn in "
                 "another lane; a grey one is outside Attic.", style="Doc/Body", color="Canvas/label", name="Intro",
           x=0, y=140, width=1600, textAutoResize="height")
    lg = _legend(d, 0, 260)
    y = 260 + d.get(lg)["height"] + 80
    for title, desc, nodes, edges in LANES:
        lane = _lane(d, title, desc, nodes, edges, y)
        y += d.get(lane)["height"] + 64


# ============================================================================== build
def build(d: Doc, ctx: dict):
    trig: dict = ctx.get("triggers", {})
    w = Wiring(d, ctx)
    _canvas_vars(d)
    _notes(d, w)
    _inherit_light(d)
    for (src, dst), layer in trig.items():
        w.link(src, layer, dst)
    for src, dst, layer in ctx.get("triggers_more", []):
        if layer not in w.wired_layers:
            w.link(src, layer, dst)
    _extra(d, w, trig)
    problems = _verify(d, w)
    flows = _flows(d, w.notes)
    _overview(d)
    ctx["flows_report"] = {
        "interactions": len(w.added),
        "links": sum(1 for a in w.added if a["theme"] in ("light", "-")),
        "dark_mirrors": sum(1 for a in w.added if a["theme"] == "dark"),
        "skipped": w.skipped,
        "problems": problems,
        "flows": [(n, desc) for _, n, desc in flows],
        "notes": {k: v for k, v in w.notes.items()},
        "added": w.added,
    }
