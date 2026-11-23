"""Prototype links, flow starting points, and the Flow overview page.

Owner: finishing agent. Runs after every screen module and before handoff.

Prototype
---------
- Every trigger the screen modules recorded in ctx["triggers"][(src, dst)] = Light layer id (or a list of them) is
  wired with Wiring.link, which mirrors it onto the Dark copy through the screen's id map. dst "back" is the back
  action. ctx["links"] holds what a plain click can't say: a drag (scroll), a long press (mouse down held 500ms) and
  a delay (the first scan finishing).
  Pseudo targets ("settings_style_switch") go to the frames in the "Outside Attic" area below.
- Transitions follow ExpressiveShell.kt and the Elevation and motion page: a tab root or a filter dissolves on
  defaultEffects; a detail screen, a first-run step and a Recap story card slide in on defaultSpatial (the
  shared X axis, drawn as a slide from the right); Now Playing, the queue, lyrics and every sheet rise from the
  bottom on overlaySpring (damping ratio 1, stiffness 380); dialogs, menus and in-place changes dissolve.
- Links the triggers don't cover are added here: the theme buttons (set variable mode on every Light/Dark
  collection), the style switch, Share, the stand-ins' buttons, Close on the other Now Playing states, swipe down
  on Now Playing, every list's song More button and the long press on an instant mix (to the track menu over
  Home), the track menu's Play next and Add to queue (to a message), messages timing out after 4 seconds, the mini
  player's play and pause, and a drag on the album and Recap pages (their scrolled states).
- The Light screens on the Screens page don't pin a Light mode (it is every collection's default), so a set
  variable mode action really turns them Dark. The Dark copies keep their pinned Dark mode.
- ctx["flow_report"] holds what was wired, verified and skipped, for the lead.

Area "Outside Attic" (Screens page)
-----------------------------------
Frames for the places a flow leaves the app: the Comfort style (its own file), Android's share sheet, the
system file picker (with Save, which goes on to Backup saved) and Attic's page in Android's app settings. Keys
handoff_style_switch, handoff_share, handoff_file_picker, handoff_app_permissions, kind "prototype note" in d.screens
(not app screens: no ready-for-dev status).

Flow overview page
------------------
Cards (screen name, key, area colour stripe) on a grid grouped by area, joined by orthogonal arrows
(vectors with a triangle end cap) labelled with the user's action. Solid arrows go forward, dashed ones go
back. Colours are Canvas/flow/* variables added to the Canvas collection here.
"""
from __future__ import annotations

import math

from figbuild.kit import Doc, V

REFS: dict[str, str] = {}

# Springs from ExpressiveTheme.kt · ExpressiveMotion and ExpressiveShell.kt · overlaySpring, as Figma springs
# (mass 1, damping = 2·ζ·√stiffness).
SPATIAL = {"customSpring": {"mass": 1, "stiffness": 380, "damping": round(2 * 0.8 * math.sqrt(380), 1)}}
OVERLAY = {"customSpring": {"mass": 1, "stiffness": 380, "damping": round(2 * 1.0 * math.sqrt(380), 1)}}
EFFECTS = {"customSpring": {"mass": 1, "stiffness": 1600, "damping": 80.0}}

TRANSITIONS = {
    "detail": dict(transition="slideIn", direction="left", easing=SPATIAL, duration=400),
    "overlay": dict(transition="moveIn", direction="top", easing=OVERLAY, duration=400),
    "fade": dict(transition="dissolve", easing=EFFECTS, duration=200),
}

TABS = {"home", "search_browse", "library_list"}
MODE_TARGETS = {"Light": "Light", "Dark": "Dark", "Match phone": "Light"}


def transition_for(src: str, dst: str) -> str:
    """Which of TRANSITIONS a link from screen `src` to screen `dst` takes, after the code."""
    if dst in TABS or dst.startswith(("home_", "library_", "search_")) or src.startswith(("home_", "library_")):
        return "fade"                                   # tab roots, filters, search states
    if src.startswith("snackbar"):
        return "fade"                                   # the message goes away, or More opens over the same screen
    if dst.startswith(("now_playing", "queue", "lyrics_", "sheet_", "sleep_timer", "open_with")):
        return "overlay"
    if dst.startswith(("dialog_", "menu_", "snackbar", "handoff_")) or src.startswith(("dialog_", "menu_")):
        return "fade"
    if "reorder" in src or "reorder" in dst:
        return "fade"
    if src.startswith("recap_story") and dst == "recap":
        return "fade"                                   # Close: the story dialog goes away
    if src.startswith("recap_story") and dst.startswith("recap_share"):
        return "fade"                                   # Save or Share: the image that is written
    return "detail"                                     # detail screens, Recap story cards, first-run steps


# ───────────────────────────────────────────────────────────────── wiring
class Wiring:
    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self.added: list[dict] = []          # every interaction added: layer, expected dest id or "back"
        self.skipped: list[str] = []
        self.links = 0

    def dark_layer(self, key: str, layer: int | None):
        s = self.d.screens[key]
        if not s.get("dark"):
            return None
        if layer is None or layer == s["light"]:
            return s["dark"]
        return s["map"].get(layer)

    def _has(self, layer: int, trigger: str) -> bool:
        got = self.d.get(layer, fields=["interactions"]).get("interactions") or []
        return any((i.get("trigger") or {}).get("type") == trigger for i in got)

    def link(self, src: str, layer: int | None, dst: str | None, *, how: str | None = None, label: str = "",
             trigger: str = "onClick", delay: int | None = None):
        """Light and Dark: navigate `src` -> `dst` (None: back) from `layer` (None: the whole frame)."""
        d = self.d
        if src not in d.screens:
            self.skipped.append(f"{src} -> {dst or 'back'}: no screen {src!r}")
            return False
        if dst is not None and dst not in d.screens:
            self.skipped.append(f"{src} -> {dst}: no screen {dst!r}")
            return False
        s = d.screens[src]
        pairs = [("light", layer or s["light"], d.screens[dst]["light"] if dst else None)]
        if s.get("dark"):
            dl = self.dark_layer(src, layer)
            if dl is None:
                self.skipped.append(f"{src} -> {dst or 'back'} (Dark): layer {layer} has no Dark counterpart")
            else:
                dd = d.screens[dst]["dark"] or d.screens[dst]["light"] if dst else None
                pairs.append(("dark", dl, dd))
        how = how or (transition_for(src, dst) if dst else None)
        made = False
        for theme, lay, dest in pairs:
            if self._has(lay, trigger):
                self.skipped.append(f"{src} -> {dst or 'back'} ({theme}): layer {lay} already has a {trigger} interaction")
                continue
            params = {"id": lay, "trigger": {"type": trigger, **({"ms": delay} if trigger == "afterDelay" else {})}}
            if delay is not None and trigger in ("mouseDown", "mouseUp", "mouseEnter", "mouseLeave"):
                params["delayMs"] = delay
            if dst is None:
                params["action"] = {"type": "back"}
            else:
                t = TRANSITIONS[how]
                params["action"] = {"type": "navigate", "dest": dest}
                params["transition"] = {"type": t["transition"], **({"direction": t["direction"]} if "direction" in t else {})}
                params["durationMs"] = t["duration"]
                params["easing"] = t["easing"]
            d.cmd("proto.interaction.add", **params)
            self.added.append({"src": src, "dst": dst or "back", "theme": theme, "layer": lay, "dest": dest,
                               "trigger": trigger, "label": label})
            made = True
        if made:
            self.links += 1
        return made

    def actions(self, src: str, layer: int, theme: str, actions: list, *, label: str, dest_key: str | None = None):
        """A click with a list of actions (set variable mode, then maybe navigate) on one layer."""
        d = self.d
        if self._has(layer, "onClick"):
            self.skipped.append(f"{src} {label} ({theme}): layer {layer} already has an onClick interaction")
            return
        params = {"id": layer, "trigger": {"type": "onClick"}, "action": actions[0]}
        if len(actions) > 1:
            params["actions"] = actions
            params.pop("action")
        if dest_key:
            params["transition"] = {"type": "dissolve"}
            params["durationMs"] = 200
            params["easing"] = EFFECTS
        d.cmd("proto.interaction.add", **params)
        dest = next((a.get("dest") for a in actions if a.get("type") == "navigate"), None)
        self.added.append({"src": src, "dst": dest_key or "(mode)", "theme": theme, "layer": layer, "dest": dest,
                           "trigger": "onClick", "label": label, "modes": [a for a in actions if a["type"] == "setVariableMode"]})


def find_named(d: Doc, root: int, name: str) -> int | None:
    for i in d.find(root, name=name, limit=200):
        if d.get(i).get("name") == name:
            return i
    return None


# ───────────────────────────────────────────────────────────────── "Outside Attic" frames
OUTSIDE = [
    ("handoff_style_switch", "Style / Comfort", "palette",
     "Comfort is another file",
     "Choosing Comfort redraws every screen in the Comfort style at once; the library, queue and settings stay as "
     "they are. Comfort's screens are in the Attic Comfort design file.",
     "MainActivity.kt · LocalUiStyle; ui/expressive/ExpressiveSettings.kt · StyleCell"),
    ("handoff_share", "Share / System share sheet", "share",
     "Android's share sheet opens",
     "Share hands the song's file to Android's share sheet, titled with the song's name. Attic draws nothing "
     "after that; the share sheet and the app the user picks take over.",
     "ui/SongShare.kt · SongShare.share"),
    ("handoff_file_picker", "Backup / System file picker", "file_save",
     "Android's file picker opens",
     "Back up to a file opens the system picker to save attic-backup-<date>.json; Restore from a file opens it "
     "to choose one. Attic needs no storage permission for either.",
     "ui/model/SettingsModel.kt · createFile, openFile"),
    ("handoff_app_permissions", "Permissions / System app settings", "settings",
     "Android's app settings open",
     "Open app permissions opens Attic's page in Android's settings, where the person turns on Music and audio. "
     "Attic draws nothing there.",
     "ui/model/RootModel.kt · openAppPermissions (ACTION_APPLICATION_DETAILS_SETTINGS); ui/model/OnboardingModel.kt"),
]
# A second button on a stand-in, for the outcome the prototype follows: (key, label, destination).
OUTSIDE_NEXT = {"handoff_file_picker": ("Save", "snackbar_backup_saved")}


def build_outside(d: Doc, ctx: dict) -> list[str]:
    a = d.area("Outside Attic", note="Where a flow leaves the app or this file. These frames stand in for Android's "
                                    "own sheets and for the Comfort style, so the prototype can show the hand-off "
                                    "and come back.")
    keys = []
    for key, name, icon, title, body, source in OUTSIDE:
        f = a.screen(key, name=name, source=source)
        d.screens[key]["kind"] = "prototype note"
        d.instance("Status bar", f, name="Status bar", x=0, y=0)
        col = d.frame(f, name="Message", x=40, y=270, w=331, layout="v", gap=20, cross="center",
                      sizing=("fixed", "hug"))
        badge = d.frame(col, name="Badge", w=112, h=112, layout="v", main="center", cross="center",
                        fill="Color/secondary-container", radius=56)
        d.icon(badge, icon, size=48, color="Color/on-secondary-container", name="Icon")
        t = d.text(col, title, style="Headline/Small", color="Color/on-surface", name="Title", textAlign="center")
        d.set(t, width=331, textAutoResize="height")
        b = d.text(col, body, style="Body/Large", color="Color/on-surface-variant", name="Body", textAlign="center")
        d.set(b, width=331, textAutoResize="height")
        lab = next((p for p in d.props.get("Button", []) if p.split("#")[0] == "Label"), "Label")
        row = d.frame(col, name="Buttons", layout="h", gap=8, sizing=("hug", "hug"))
        btn = d.instance("Button", row, name="Back",
                         props={"Style": "Outlined" if key in OUTSIDE_NEXT else "Filled", "Size": "Small",
                                "Icon": "None", "State": "Enabled", "Squared": "False"})
        d.cmd("instance.setProperties", ids=[btn], properties={lab: "Back"})
        if key in OUTSIDE_NEXT:
            nb = d.instance("Button", row, name=OUTSIDE_NEXT[key][0],
                            props={"Style": "Filled", "Size": "Small", "Icon": "None", "State": "Enabled",
                                   "Squared": "False"})
            d.cmd("instance.setProperties", ids=[nb], properties={lab: OUTSIDE_NEXT[key][0]})
        d.instance("Gesture bar", f, name="Gesture bar", x=0, y=914 - 24)
        keys.append(key)
    a.close()
    for key in keys:
        s = d.screens[key]
        for fid in (s["light"], s["dark"]):
            d.annotate(fid, f"Compose: {s['source']}", "Source")
            d.annotate(fid, "Prototype stand-in, not an Attic screen: it shows where the flow leaves the app or this "
                            "file, and its Back button returns." + (
                                f" {OUTSIDE_NEXT[key][0]} follows the outcome the flow shows next." if key in OUTSIDE_NEXT
                                else ""), "Note")
    return keys


# ───────────────────────────────────────────────────────────────── theme
def unpin_light(d: Doc):
    """Let the Light screens follow each collection's default mode (Light) so set variable mode can turn them
    Dark in the prototype. The Dark copies keep their pinned mode."""
    for s in d.screens.values():
        if s["page"] != "Screens" or not s.get("light"):
            continue
        for c in d.mode_collections:
            d.set_mode(s["light"], c, None)


def wire_theme(w: Wiring):
    d = w.d
    for key in ("settings", "settings_full"):
        if key not in d.screens:
            w.skipped.append(f"{key}: theme buttons (no screen)")
            continue
        s = d.screens[key]
        for theme in ("light", "dark"):
            fid = s.get(theme)
            if not fid:
                continue
            cell = find_named(d, fid, "Theme cell")
            if cell is None:
                w.skipped.append(f"{key} ({theme}): no Theme cell")
                continue
            for n in (1, 2, 3):
                seg = find_named(d, cell, f"Segment {n}")
                lab = find_named(d, seg, "Label") if seg else None
                text = d.get(lab).get("characters") if lab else None
                if text not in MODE_TARGETS:
                    w.skipped.append(f"{key} ({theme}): segment {n} label {text!r}")
                    continue
                mode = MODE_TARGETS[text]
                acts = [{"type": "setVariableMode", "collection": c, "mode": mode} for c in d.mode_collections]
                dest_key = None
                if theme == "dark" and mode == "Light":
                    # The Dark copy pins Dark, so it hands over to the Light frame, now drawn in Light.
                    acts.append({"type": "navigate", "dest": s["light"]})
                    dest_key = key
                w.actions(key, seg, theme, acts, label=f"Theme {text}", dest_key=dest_key)
        w.links += 1
        for fid in (s["light"], s["dark"]):
            if fid:
                d.annotate(fid, "Prototype: Light and Dark run set variable mode on every Light/Dark collection, so the "
                                "screens you visit next are drawn in that theme. Match phone picks Light, the example "
                                "phone's setting.", "Note")


# ───────────────────────────────────────────────────────────────── build
def wire(d: Doc, ctx: dict) -> Wiring:
    w = Wiring(d, ctx)
    triggers = dict(ctx.get("triggers", {}))
    pseudo = {"settings_style_switch": "handoff_style_switch"}
    for (src, dst), layers in triggers.items():
        for layer in (layers if isinstance(layers, list) else [layers]):
            if dst == "back":
                w.link(src, layer, None, label="Back")
                continue
            if dst in pseudo:
                # The control is the whole group; Comfort is its second segment.
                seg = find_named(d, layer, "Segment 2") if src in d.screens else None
                w.link(src, seg or layer, pseudo[dst], how="fade", label="Tap Comfort")
                continue
            w.link(src, layer, dst)
    # Drags, long presses and delays the screen modules asked for.
    for l in ctx.get("links", []):
        w.link(l["src"], l["layer"], l["dst"], how=l.get("how"), label=l.get("label", ""), trigger=l["trigger"],
               delay=l.get("delay"))

    # What the triggers leave out.
    S = d.screens
    for key in ("sheet_track_menu", "sheet_track_menu_playlist", "sheet_track_menu_now_playing"):
        if key in S:
            cell = find_named(d, S[key]["light"], "Share cell")
            if cell:
                w.link(key, cell, "handoff_share", how="fade", label="Tap Share")
            else:
                w.skipped.append(f"{key} -> handoff_share: no Share cell")
    for key in OUTSIDE:
        k = key[0]
        if k in S:
            btn = find_named(d, S[k]["light"], "Back")
            w.link(k, btn, None, label="Back")
            if k in OUTSIDE_NEXT:
                nb = find_named(d, S[k]["light"], OUTSIDE_NEXT[k][0])
                w.link(k, nb, OUTSIDE_NEXT[k][1], how="fade", label=OUTSIDE_NEXT[k][0])
    for key in ("now_playing_full", "now_playing_paused_radio", "open_with"):
        if key in S and (key, "back") not in triggers:
            hd = find_named(d, S[key]["light"], "Header")
            close = find_named(d, hd, "Close") if hd else None
            if close:
                if key == "open_with":
                    # Closing shows the app under it, with this file in the mini player.
                    w.link(key, close, "mini_player_open_with", how="overlay", label="Close")
                else:
                    w.link(key, close, None, label="Close")
    # Any song's More opens the track menu (ExpressiveTrackMenu via actions.showMenu). The prototype has one menu
    # frame for lists, drawn over Home.
    for key in ("album", "artist", "folder", "favourites", "library_songs", "search_results", "home_full"):
        if key in S and "sheet_track_menu" in S:
            tr = find_named(d, S[key]["light"], "Trailing")
            if tr:
                w.link(key, tr, "sheet_track_menu", how="overlay", label="Tap a song's More")
            else:
                w.skipped.append(f"{key} -> sheet_track_menu: no song row's More")
    # A long press on an instant mix opens the same menu (ExpressiveHome InstantMixPill onLongClick).
    for key in ("home", "home_full", "home_recap_year"):
        if key in S and "sheet_track_menu" in S:
            pill = find_named(d, S[key]["light"], "Instant mix · Porch Light")
            if pill:
                w.link(key, pill, "sheet_track_menu", how="overlay", label="Long-press an instant mix",
                       trigger="mouseDown", delay=500)
    # The track menu's quick actions raise a message (Play next, Add to queue).
    for src, action, dst in (("sheet_track_menu", "Add to queue", "snackbar"),
                             ("sheet_track_menu_now_playing", "Play next", "snackbar_over_player")):
        if src in S and dst in S:
            qa = find_named(d, S[src]["light"], action)
            if qa:
                w.link(src, qa, dst, how="fade", label=f"Tap {action}")
    # Messages leave after a few seconds (SnackbarDuration.Short), showing the screen under them again.
    for key, under in (("snackbar", "home"), ("snackbar_share_failed", "home"), ("snackbar_open_failed", "home"),
                       ("snackbar_over_player", "now_playing"), ("snackbar_sleep_timer", "now_playing"),
                       ("snackbar_sleep_timer_off", "now_playing"), ("snackbar_backup_saved", "settings_backup")):
        if key in S:
            w.link(key, None, under, how="fade", label="Message times out", trigger="afterDelay", delay=4000)
    if "now_playing" in S:
        w.link("now_playing", None, None, trigger="onDrag", label="Swipe down")
    if "playlist" in S and "sheet_track_menu_playlist" in S:
        g = find_named(d, S["playlist"]["light"], "Songs group")
        trailing = find_named(d, g, "Trailing") if g else None
        if trailing:
            w.link("playlist", trailing, "sheet_track_menu_playlist", label="Tap a song's More")
    # The mini player's own control pauses and plays in place (ExpressiveMiniPlayer); the rest of it opens Now Playing.
    for src, dst in (("home", "mini_player_paused"), ("mini_player_paused", "home")):
        if src in S and dst in S:
            m = find_named(d, S[src]["light"], "Mini player")
            play = find_named(d, m, "Play") if m else None
            if play:
                w.link(src, play, dst, how="fade", label="Tap pause" if dst == "mini_player_paused" else "Tap play")
    # Scrolling a page shows its scrolled state.
    for src, dst in (("home", "home_full"), ("album", "album_collapsed"), ("recap", "recap_full")):
        if src in S and dst in S:
            w.link(src, None, dst, how="fade", label="Scroll", trigger="onDrag")
    wire_theme(w)
    return w


FLOWS = [
    ("onboarding_welcome", "1 First run",
     "Welcome, Pick your look, Access, Your library, then Home."),
    ("home", "2 Play an album",
     "Home, an album, Now Playing from the mini player, then the queue and lyrics and back."),
    ("search_browse", "3 Search",
     "Browse, type a query, open a result."),
    ("library_list", "4 Library filters",
     "The filter chips (Playlists, Albums, Artists, Songs) and the grid toggle."),
    ("settings", "5 Settings, theme and style",
     "Light and Dark switch the prototype's theme. Match phone switches to Light, the example phone's setting. "
     "Comfort explains the style switch. Drag the list to scroll to Backup."),
    ("recap", "6 Recap story",
     "Play recap, tap through the ten cards (the left third goes back one), Save or Share a card's image, close "
     "back to Recap."),
    ("now_playing", "7 Sleep timer",
     "Now Playing, More, Sleep timer, 30 minutes: the menu closes and a message says when music will pause. More "
     "again shows the timer running under Sleep timer."),
    ("sheet_track_menu", "8 Share a song",
     "The track menu's Share row hands the file to Android's share sheet."),
    ("settings_backup", "9 Backup and restore",
     "Back up to a file (system picker, then Backup saved), Restore from a file, confirm, done."),
    ("open_with", "10 Open a file from another app",
     "A file opened from a file manager plays in Now Playing with no cover or lyrics. More opens its shorter menu; "
     "Close shows the app with the file in the mini player."),
    ("playlist", "11 Edit a playlist",
     "Add songs, reorder them, or rename and delete the playlist from its menu. A song's More opens its menu."),
]


def add_flows(d: Doc, ctx: dict, w: Wiring) -> list[str]:
    made = []
    for key, name, desc in FLOWS:
        if key not in d.screens:
            w.skipped.append(f"flow {name!r}: no screen {key!r}")
            continue
        start = d.screens[key]["light"]
        d.cmd("proto.flow.add", start=start, name=name)
        try:
            d.cmd("proto.flow.rename", start=start, name=name, description=desc)
        except Exception:
            pass
        made.append(name)
    settings = d.cmd("proto.settings", device="Android Compact")
    scr = settings.get("screen") or {}
    if (scr.get("width"), scr.get("height")) != (411, 914):
        settings = d.cmd("proto.settings", device="Custom size", width=411, height=914)
    ctx.setdefault("flow_report", {})["settings"] = settings
    return made


def verify(d: Doc, w: Wiring) -> dict:
    """Read every added interaction back and check where it goes."""
    names = {}
    for s in d.screens.values():
        for t in ("light", "dark"):
            if s.get(t):
                names[s[t]] = s["name"] + (" · Light" if t == "light" else " · Dark")
    ok, bad = 0, []
    cache = {}
    for rec in w.added:
        lay = rec["layer"]
        if lay not in cache:
            cache[lay] = d.get(lay, fields=["interactions"]).get("interactions") or []
        found = False
        for it in cache[lay]:
            if (it.get("trigger") or {}).get("type") != rec["trigger"]:
                continue
            acts = it.get("actions") or ([it["action"]] if it.get("action") else [])
            types = [a.get("type") for a in acts]
            if rec["dst"] == "back":
                found = "back" in types
            elif rec["dst"] == "(mode)":
                found = "setVariableMode" in types
            else:
                dests = [a.get("dest") or a.get("destinationId") for a in acts]
                dest = next((x for x in dests if x), None)
                want = names.get(rec["dest"], "")
                got = names.get(dest) or (d.get(dest).get("name") if dest else None)
                found = dest == rec["dest"] and got == want
                if not found:
                    bad.append(f"{rec['src']} -> {rec['dst']} ({rec['theme']}): goes to {got!r}, wanted {want!r}")
                    found = None
            break
        if found:
            ok += 1
        elif found is False:
            bad.append(f"{rec['src']} -> {rec['dst']} ({rec['theme']}): interaction not found on layer {lay}")
    return {"interactions": len(w.added), "verified": ok, "problems": bad}


def build(d: Doc, ctx: dict):
    build_outside(d, ctx)
    unpin_light(d)
    w = wire(d, ctx)
    flows = add_flows(d, ctx, w)
    rep = verify(d, w)
    report = ctx.setdefault("flow_report", {})
    report.update(links=w.links, flows=flows, skipped=w.skipped, **rep)
    print(f"    flows: {w.links} links, {rep['interactions']} interactions, {rep['verified']} verified, "
          f"{len(rep['problems'])} problems, {len(w.skipped)} skipped; {len(flows)} flow starts")
    for line in rep["problems"][:20] + w.skipped[:40]:
        print("     ", line)
    build_overview(d, ctx)


# ───────────────────────────────────────────────────────────────── Flow overview
AREAS = {   # area -> (Canvas variable, hex, description)
    "first-run": ("flow/first-run", "#7C3AED", "First run and launch"),
    "tabs": ("flow/tabs", "#0284C7", "Tabs"),
    "settings": ("flow/settings", "#475569", "Settings"),
    "recap": ("flow/recap", "#DB2777", "Recap"),
    "collections": ("flow/collections", "#059669", "Collections"),
    "player": ("flow/player", "#EA580C", "Now Playing"),
    "sheets": ("flow/sheets", "#CA8A04", "Sheets, menus and dialogs"),
    "outside": ("flow/outside", "#A1A1AA", "Outside Attic"),
}

# key, column, row, area (card colour), caption
NODES = [
    ("onboarding_welcome", 0, 0, "first-run", "Once per install"),
    ("onboarding_look", 0, 1, "first-run", None),
    ("onboarding_access", 0, 2, "first-run", None),
    ("onboarding_access_granted", 0, 3, "first-run", None),
    ("onboarding_library_scanning", 0, 4, "first-run", None),
    ("onboarding_library_found", 0, 5, "first-run", None),
    ("permission", 0, 7, "first-run", "Later launches, while audio access is off"),
    ("loading", 0, 8, "first-run", None),
    ("home", 1, 0, "tabs", "Tab · start destination"),
    ("home_albums", 1, 1, "tabs", "Also Artists and Folders"),
    ("search_browse", 1, 3, "tabs", "Tab"),
    ("search_typing", 1, 4, "tabs", None),
    ("search_results", 1, 5, "tabs", None),
    ("library_list", 1, 7, "tabs", "Tab"),
    ("library_playlists", 1, 8, "tabs", "Also Albums, Artists, Songs, Grid"),
    ("settings", 2, 0, "settings", "Light and Dark switch the theme"),
    ("settings_backup", 3, 0, "settings", None),
    ("dialog_restore_confirm", 4, 0, "settings", "After the system file picker"),
    ("dialog_restore_result", 5, 0, "settings", None),
    ("handoff_style_switch", 3, 1, "outside", "Attic Comfort file"),
    ("handoff_file_picker", 4, 1, "outside", "Android"),
    ("licences", 3, 2, "settings", None),
    ("licence_text", 4, 2, "settings", None),
    ("recap", 2, 3, "recap", None),
    ("recap_story_intro", 3, 3, "recap", "Full-screen story"),
    ("recap_story_minutes", 4, 3, "recap", None),
    ("recap_story_top_song", 5, 3, "recap", None),
    ("recap_story_top_songs", 6, 3, "recap", None),
    ("recap_story_top_artist", 7, 3, "recap", None),
    ("recap_story_top_artists", 7, 4, "recap", None),
    ("recap_story_languages", 6, 4, "recap", None),
    ("recap_story_listening_clock", 5, 4, "recap", None),
    ("recap_story_highlights", 4, 4, "recap", None),
    ("recap_story_summary", 3, 4, "recap", "Close works on every card"),
    ("favourites", 2, 5, "collections", None),
    ("album", 2, 6, "collections", None),
    ("artist", 2, 7, "collections", None),
    ("folder", 2, 8, "collections", None),
    ("playlist", 2, 9, "collections", None),
    ("now_playing", 3, 6, "player", "From the mini player on any screen"),
    ("queue", 4, 6, "player", None),
    ("lyrics_synced", 4, 7, "player", None),
    ("sheet_track_menu_now_playing", 4, 8, "sheets", None),
    ("sheet_sleep_timer", 5, 8, "sheets", None),
    ("snackbar_sleep_timer", 6, 8, "sheets", "The menu closes; a message shows"),
    ("sleep_timer_active", 7, 8, "sheets", "More again"),
    ("handoff_share", 5, 9, "outside", "Android"),
    ("sheet_add_songs", 3, 10, "sheets", None),
    ("menu_playlist_overflow", 3, 11, "sheets", None),
    ("dialog_rename_playlist", 4, 11, "sheets", None),
    ("dialog_delete_playlist", 4, 12, "sheets", None),
    ("sheet_track_menu", 2, 13, "sheets", "Any song's More, or a long press; drawn over Home"),
    ("sheet_playlist_picker", 3, 13, "sheets", None),
    ("dialog_new_playlist", 4, 13, "sheets", None),
    ("sheet_language_picker", 3, 14, "sheets", None),
]

# Boxes that group the cards on the canvas: title, area colour, (col0, row0, col1, row1)
BANDS = [
    ("First run and launch", "first-run", (0, 0, 0, 8)),
    ("Tabs", "tabs", (1, 0, 1, 8)),
    ("Settings", "settings", (2, 0, 5, 2)),
    ("Recap", "recap", (2, 3, 7, 4)),
    ("Collections", "collections", (2, 5, 2, 9)),
    ("Now Playing, queue and lyrics", "player", (3, 6, 7, 9)),
    ("Playlist editing", "sheets", (3, 10, 4, 12)),
    ("Track menu and pickers", "sheets", (2, 13, 4, 14)),
]

# src, dst, action label, back (dashed)
EDGES = [
    ("onboarding_welcome", "onboarding_look", "Get started", False),
    ("onboarding_look", "onboarding_access", "Continue", False),
    ("onboarding_access", "onboarding_access_granted", "Allow", False),
    ("onboarding_access_granted", "onboarding_library_scanning", "Continue", False),
    ("onboarding_library_scanning", "onboarding_library_found", "Scan finishes", False),
    ("onboarding_library_found", "home", "Start listening", False),
    ("permission", "loading", "Allow access", False),
    ("loading", "home", "Scan finishes", False),
    ("home", "home_albums", "Tap a filter chip", False),
    ("search_browse", "search_typing", "Tap the search field", False),
    ("search_typing", "search_results", "Type a query", False),
    ("library_list", "library_playlists", "Tap a filter chip", False),
    ("home", "settings", "Tap the mark", False),
    ("home", "recap", "Tap the Recap card", False),
    ("home", "favourites", "Tap Favourites", False),
    ("home", "album", "Tap an album", False),
    ("home", "sheet_track_menu", "Long-press a mix", False),
    ("search_results", "album", "Tap a result", False),
    ("search_results", "artist", "Tap an artist", False),
    ("library_list", "favourites", "Tap Favourites", False),
    ("library_list", "album", "Tap an album", False),
    ("library_list", "artist", "Tap an artist", False),
    ("library_list", "folder", "Tap a folder", False),
    ("library_list", "playlist", "Tap a playlist", False),
    ("album", "artist", "Tap the artist", False),
    ("settings", "settings_backup", "Scroll to Backup", False),
    ("settings", "handoff_style_switch", "Tap Comfort", False),
    ("settings", "licences", "Tap Licences", False),
    ("settings_backup", "dialog_restore_confirm", "Restore from a file", False),
    ("settings_backup", "handoff_file_picker", "Back up to a file", False),
    ("dialog_restore_confirm", "dialog_restore_result", "Tap Restore", False),
    ("licences", "licence_text", "Tap a licence", False),
    ("recap", "recap_story_intro", "Play recap", False),
    ("recap_story_intro", "recap_story_minutes", "Tap", False),
    ("recap_story_minutes", "recap_story_top_song", "Tap", False),
    ("recap_story_top_song", "recap_story_top_songs", "Tap", False),
    ("recap_story_top_songs", "recap_story_top_artist", "Tap", False),
    ("recap_story_top_artist", "recap_story_top_artists", "Tap", False),
    ("recap_story_top_artists", "recap_story_languages", "Tap", False),
    ("recap_story_languages", "recap_story_listening_clock", "Tap", False),
    ("recap_story_listening_clock", "recap_story_highlights", "Tap", False),
    ("recap_story_highlights", "recap_story_summary", "Tap", False),
    ("recap_story_summary", "recap", "Close", True),
    ("album", "now_playing", "Tap the mini player", False),
    ("now_playing", "album", "Swipe down", True),
    ("now_playing", "queue", "Tap Queue", False),
    ("queue", "now_playing", "Close", True),
    ("now_playing", "lyrics_synced", "Tap the lyrics", False),
    ("lyrics_synced", "now_playing", "Close", True),
    ("now_playing", "sheet_track_menu_now_playing", "Tap More", False),
    ("sheet_track_menu_now_playing", "sheet_sleep_timer", "Tap Sleep timer", False),
    ("sheet_sleep_timer", "snackbar_sleep_timer", "Choose 30 minutes", False),
    ("snackbar_sleep_timer", "sleep_timer_active", "Tap More", False),
    ("sheet_track_menu_now_playing", "handoff_share", "Tap Share", False),
    ("playlist", "sheet_add_songs", "Tap Add songs", False),
    ("playlist", "menu_playlist_overflow", "Tap More", False),
    ("menu_playlist_overflow", "dialog_rename_playlist", "Rename", False),
    ("menu_playlist_overflow", "dialog_delete_playlist", "Delete playlist", False),
    ("sheet_track_menu", "sheet_playlist_picker", "Add to playlist", False),
    ("sheet_track_menu", "sheet_language_picker", "Language", False),
    ("sheet_playlist_picker", "dialog_new_playlist", "New playlist", False),
]

CARD_W, CARD_H = 360, 176
PX, PY = 1000, 300           # column and row pitch
MARGIN = 120
LABEL_ZONE = 270             # room for a label between a lane and the card it points at
LANE_GAP = 24


def _pill(d: Doc, parent, text: str, cx: float, cy: float) -> int:
    p = d.frame(parent, name=f"Label · {text}", layout="h", pad=[4, 14, 4, 14], fill="Canvas/card",
                stroke="Canvas/divider", stroke_w=1, radius=19, sizing=("hug", "hug"))
    d.text(p, text, style="Doc/Body", color="Canvas/label", name="Action")
    b = d.get(p)
    d.set(p, x=round(cx - b["width"] / 2), y=round(cy - b["height"] / 2))
    return p


def build_overview(d: Doc, ctx: dict):
    from expressive.foundations import DocPage
    for var, hexv, desc in AREAS.values():
        try:
            d.variable("Canvas", var, "color", value=hexv, description=f"Flow overview: {desc}")
        except Exception:
            pass
    d._vars = {}   # refresh the name cache
    nodes = [n for n in NODES if n[0] in d.screens]
    missing = [n[0] for n in NODES if n[0] not in d.screens]
    pos = {k: (c, r) for k, c, r, _, _ in nodes}
    edges = [e for e in EDGES if e[0] in pos and e[1] in pos]

    p = DocPage(d, "Flow overview", "Flow overview",
                "How a person moves through Attic, from the first run to the deepest sheet. Each card is a frame on the "
                "Screens page (its name and key); the stripe is its area. Arrows are labelled with what the person does: "
                "solid ones go forward, dashed ones go back. The navigation bar switches between the three tabs from "
                "any screen, and the mini player opens Now Playing from any screen; those arrows are left out. The "
                "prototype on the Screens page follows the same links.", intro_width=1800)

    # Legend
    legend = d.frame(None, name="Legend", x=0, y=p.y, layout="h", gap=40, pad=[24, 32, 24, 32], fill="Canvas/card",
                     radius=24, cross="center", sizing=("hug", "hug"))
    for key, (var, _, desc) in AREAS.items():
        it = d.frame(legend, name=f"Legend · {desc}", layout="h", gap=12, cross="center", sizing=("hug", "hug"))
        d.rect(it, name="Swatch", w=28, h=28, radius=8, fill=f"Canvas/{var}")
        d.text(it, desc, style="Doc/Body", color="Canvas/label", name="Area")
    for nm, dashed in (("Forward", False), ("Back or close", True)):
        it = d.frame(legend, name=f"Legend · {nm}", layout="h", gap=12, cross="center", sizing=("hug", "hug"))
        ln = d.vector(it, name=f"Arrow sample · {nm.lower()}", path="M 0 10 L 64 10", stroke="Canvas/label-muted", stroke_w=3)
        d.set(ln, endCap="triangleArrow", **({"dashes": [10, 8]} if dashed else {}))
        d.text(it, nm, style="Doc/Body", color="Canvas/label", name="Kind")
    y0 = p.y + d.get(legend)["height"] + 80

    cols = max(c for _, c, _, _, _ in nodes) + 1
    rows = max(r for _, _, r, _, _ in nodes) + 1
    W = MARGIN * 2 + (cols - 1) * PX + CARD_W
    H = MARGIN * 2 + 80 + (rows - 1) * PY + CARD_H
    dia = d.frame(None, name="Diagram", x=0, y=y0, w=W, h=H, fill="Canvas/card", radius=32)

    def box(k):
        c, r = pos[k]
        x, y = MARGIN + c * PX, MARGIN + 80 + r * PY
        return {"x": x, "y": y, "r": x + CARD_W, "b": y + CARD_H, "cx": x + CARD_W / 2, "cy": y + CARD_H / 2}

    # Area boxes
    for title, area, (c0, r0, c1, r1) in BANDS:
        x0, y0b = MARGIN + c0 * PX - 32, MARGIN + 80 + r0 * PY - 76
        x1, y1 = MARGIN + c1 * PX + CARD_W + 32, MARGIN + 80 + r1 * PY + CARD_H + 28
        g = d.frame(dia, name=f"Area · {title}", x=x0, y=y0b, w=x1 - x0, h=y1 - y0b, fill="Canvas/section", radius=24)
        d.rect(g, name="Area colour", x=0, y=0, w=x1 - x0, h=6, fill=f"Canvas/{AREAS[area][0]}")
        d.text(g, title, style="Doc/Heading", color="Canvas/label", name="Title", x=32, y=22)

    # Route every edge
    kind = {}
    for e in edges:
        (sc, sr), (dc, dr) = pos[e[0]], pos[e[1]]
        if sc == dc and dr == sr + 1:
            kind[e] = "down"
        elif dc == sc + 1:
            kind[e] = "right"
        elif dc == sc - 1:
            kind[e] = "left"
        elif sc == dc and sr == dr:
            kind[e] = "skip"
        else:
            raise ValueError(f"flow overview: no route for {e[0]} -> {e[1]}")
    # Ports along card sides, ordered by the other end's height.
    sides: dict = {}
    for e in edges:
        k = kind[e]
        if k == "right":
            sides.setdefault((e[0], "r"), []).append(e); sides.setdefault((e[1], "l"), []).append(e)
        elif k == "left":
            sides.setdefault((e[0], "l"), []).append(e); sides.setdefault((e[1], "r"), []).append(e)
    port = {}
    for (k, side), es in sides.items():
        b = box(k)
        es.sort(key=lambda e: (box(e[1] if e[0] == k else e[0])["cy"], e[2]))
        for i, e in enumerate(es):
            port[(e, k)] = b["y"] + CARD_H * (i + 1) / (len(es) + 1)
    # Lanes in each gutter, the shortest vertical runs nearest the cards they point at.
    lanes = {}
    gutters: dict = {}
    for e in edges:
        if kind[e] in ("right", "left"):
            sy, dy = port[(e, e[0])], port[(e, e[1])]
            if abs(sy - dy) > 0.5:
                g = min(pos[e[0]][0], pos[e[1]][0])
                gutters.setdefault(g, []).append(e)
    for g, es in gutters.items():
        has_left = any(kind[e] == "left" for e in edges if min(pos[e[0]][0], pos[e[1]][0]) == g)
        lo = MARGIN + g * PX + CARD_W + (LABEL_ZONE if has_left else 40)
        hi = MARGIN + (g + 1) * PX - LABEL_ZONE
        es.sort(key=lambda e: abs(port[(e, e[0])] - port[(e, e[1])]))
        n = len(es)
        step = min(LANE_GAP * 2, (hi - lo) / max(n, 1))
        for i, e in enumerate(es):
            lanes[e] = hi - step * (i + 0.5) if kind[e] == "right" else lo + step * (i + 0.5)
    arrows = d.frame(dia, name="Arrows", x=0, y=0, w=W, h=H)
    labels = []
    for e in edges:
        k = kind[e]
        if k == "skip":
            continue
        s, t = box(e[0]), box(e[1])
        if k == "down":
            pts = [(s["cx"], s["b"]), (t["cx"], t["y"])]
            lab = (s["cx"], (s["b"] + t["y"]) / 2)
        elif k == "right":
            sy, dy = port[(e, e[0])], port[(e, e[1])]
            if abs(sy - dy) > 0.5:
                lx = lanes[e]
                pts = [(s["r"], sy), (lx, sy), (lx, dy), (t["x"], dy)]
            else:
                pts = [(s["r"], sy), (t["x"], dy)]
            lab = (t["x"] - LABEL_ZONE / 2, dy)
        else:
            sy, dy = port[(e, e[0])], port[(e, e[1])]
            if abs(sy - dy) > 0.5:
                lx = lanes[e]
                pts = [(s["x"], sy), (lx, sy), (lx, dy), (t["r"], dy)]
            else:
                pts = [(s["x"], sy), (t["r"], dy)]
            lab = (t["r"] + LABEL_ZONE / 2, dy)
        path = "M " + " L ".join(f"{x:.1f} {y:.1f}" for x, y in pts)
        v = d.vector(arrows, name=f"Arrow · {e[0]} to {e[1]}", path=path, stroke="Canvas/label-muted", stroke_w=3)
        d.set(v, endCap="triangleArrow", strokeJoin="round", **({"dashes": [10, 8]} if e[3] else {}))
        labels.append((e[2], lab))

    # Cards on top of the arrows
    names = {k: d.screens[k]["name"] for k, *_ in nodes}
    for k, c, r, area, caption in nodes:
        b = box(k)
        card = d.frame(dia, name=f"Card · {k}", x=b["x"], y=b["y"], w=CARD_W, h=CARD_H, layout="h", fill="Canvas/card",
                       stroke="Canvas/divider", stroke_w=2, radius=18, clip=True)
        stripe = d.rect(card, name="Area colour", w=12, h=CARD_H, fill=f"Canvas/{AREAS[area][0]}")
        d.set(stripe, sizingV="fill")
        col = d.frame(card, name="Card text", layout="v", gap=6, pad=[18, 20, 18, 18], sizing=("fill", "fill"))
        t = d.text(col, names[k], style="Doc/Heading", color="Canvas/label", name="Screen")
        d.set(t, sizingH="fill", textAutoResize="height", maxLines=2)
        d.text(col, k, style="Doc/Label", color="Canvas/label-muted", name="Key")
        if caption:
            cp = d.text(col, caption, style="Doc/Label", color="Canvas/label", name="Note")
            d.set(cp, sizingH="fill", textAutoResize="height", maxLines=2)
    lab_layer = d.frame(dia, name="Action labels", x=0, y=0, w=W, h=H)
    for text, (cx, cy) in labels:
        _pill(d, lab_layer, text, cx, cy)
    rep = ctx.setdefault("flow_report", {})
    rep["overview"] = {"cards": len(nodes), "arrows": sum(1 for e in edges if kind[e] != "skip"), "missing": missing}
    print(f"    overview: {len(nodes)} cards, {rep['overview']['arrows']} arrows" + (f", missing {missing}" if missing else ""))
