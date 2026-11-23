"""Screens: Now Playing, Queue, Lyrics, every sheet, dialog, menu and toast, sleep timer, Share, Open with.

Owner: playback screens agent. Three areas on the Screens page: "Now Playing", "Queue and lyrics" and
"Sheets, dialogs and messages". Every screen is built from instances of the Comfort components
(components_core, components_overlays); the screens under sheets and dialogs (Home, the Long drive playlist,
Library, Now Playing) are rebuilt here from instances by the `_home`, `_playlist`, `_library` and `_player`
helpers, since a prototype frame can't show another frame underneath.

Now Playing, the queue, lyrics and the sheets over the player set Color mode `Tinted wash` on the Light frame
before `a.close()`, so the Dark copy inherits it (the Dark copy switches Palette, Tinted and Elevation).
`open_with` stays Base: a file from another app has no cover, so no seed.

Dark copies differ from Light in one place the variables can't express: a tonal control on a dark sheet is one
step up (Quick action's Button Container=Fill 2; the neutral icon box takes fill-2). `_dark_sheet_fix` sets that
on the Dark copy after `a.close()`, as the code does (`if (colors.dark) colors.fill2 else colors.fill`).

Prototype triggers are recorded in ctx["triggers"][(src, dst)] = Light layer id ("back" as dst for close).

COMPONENTS ADDED HERE: none (the overlays are in components_overlays).
Every text style and variable used here is made in foundations.py.
"""
from __future__ import annotations

from figbuild.kit import Doc, V

from .components_core import COVER, cover_image
from .foundations import HARNESS_NOTES, REF_DIFFERS

KEYS = ["now_playing", "now_playing_full", "now_playing_paused_radio", "open_with",
        "queue", "queue_radio", "queue_empty", "lyrics_synced", "lyrics_plain", "lyrics_none", "lyrics_not_downloaded",
        "lyrics_instrumental", "lyrics_downloading",
        "sheet_track_menu", "sheet_track_menu_playlist", "sheet_track_menu_now_playing", "sheet_track_menu_open_with",
        "sheet_sleep_timer", "sleep_timer_active", "sheet_playlist_picker", "sheet_language_picker", "sheet_add_songs",
        "dialog_new_playlist", "dialog_name_playlist", "dialog_rename_playlist", "dialog_delete_playlist",
        "menu_playlist_overflow", "snackbar", "snackbar_over_player"]
REFS: dict[str, str] = {k: "comfort-{theme}-" + k for k in KEYS}
# Frames drawn from the code with no reference render.
NO_REF_KEYS = ["sheet_sleep_timer_running", "snackbar_sleep_timer_set", "snackbar_sleep_timer_off", "snackbar_share_failed",
               "snackbar_open_failed"]

SRC = "ui/comfort/"
W, H = 411, 914
STATUS, NAV = 40, 24
WASH = "Tinted wash"

# The demo library (DemoLibrary.kt): song -> (artist, cover index)
SONGS = {
    "Lights Over the Water": ("Mira Okafor", 0), "Porch Light": ("Mira Okafor", 0), "Moth Letters": ("Mira Okafor", 0),
    "Second Bus Home": ("Mira Okafor", 0), "Salt in the Radio": ("Mira Okafor", 0), "Paper Lanterns": ("Mira Okafor", 0),
    "Small Hours, Big Sky": ("Mira Okafor", 0), "Coming Back Slow": ("Mira Okafor", 0),
    "Clear by Morning": ("Mira Okafor", 11), "Low Tide Hymn": ("Mira Okafor", 11), "Umbrella Song": ("Mira Okafor", 11),
    "Fog on Garden Road": ("Mira Okafor", 11),
    "Glasshouse": ("Juniper Vale", 3), "Neon Peaches": ("Juniper Vale", 3), "Late Bloom": ("Juniper Vale", 3),
    "Sunburn Choir": ("Juniper Vale", 3), "Swimming Pool Static": ("Juniper Vale", 3),
    "Iravu Rail": ("Kavya Raman", 8), "Platform Ettu": ("Kavya Raman", 8), "Kadaisi Vandi": ("Kavya Raman", 8),
    "Night Drive Firmware": ("Velvet Circuit", 10), "404 Heartbeat": ("Velvet Circuit", 10),
    "Afterglow Protocol": ("Velvet Circuit", 10), "Midnight Checksum": ("Velvet Circuit", 10), "Drift Mode": ("Velvet Circuit", 10),
    "Bheegi Sadak": ("Ruhaan Sethi", 6), "Baadal Ka Khat": ("Ruhaan Sethi", 6), "Bijli Gayi": ("Ruhaan Sethi", 6),
    "Aadhi Neend": ("Ruhaan Sethi", 9), "Sheher Ki Shaam": ("Ruhaan Sethi", 9),
    "Auto Rickshaw Waltz": ("Tide & Tamarind", 7), "Azulejo": ("Paloma Ferrer", 2), "Calle de los Naranjos": ("Paloma Ferrer", 2),
    "Faro Apagado": ("Paloma Ferrer", 2), "Brushes": ("Odile Marchetti", 4),
}
QUEUE = ["Porch Light", "Moth Letters", "Second Bus Home", "Salt in the Radio", "Paper Lanterns", "Small Hours, Big Sky",
         "Coming Back Slow", "Glasshouse", "Neon Peaches", "Iravu Rail", "Platform Ettu"]
RADIO_QUEUE = ["Porch Light", "Clear by Morning", "Glasshouse", "Late Bloom", "Moth Letters", "Low Tide Hymn", "Umbrella Song",
               "Sunburn Choir", "Coming Back Slow", "Fog on Garden Road"]
LONG_DRIVE = ["Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak"]
# DemoLibrary.lyrics, in order ("" is a blank line, drawn as the note glyph)
LYRICS = ["", "I left the porch light on for you", "the moths are writing letters on the glass",
          "every bus that turns the corner", "sounds a little like you coming back", "",
          "and the kettle knows your name", "the stairs still creak the way you climbed them",
          "I kept the window open in the rain", "in case the night forgot to bring you", "",
          "paper lanterns over the water", "carry what we couldn't say", "let them drift until the morning",
          "let them burn the long way", "", "I counted boats along the harbour", "named the lights that didn't move",
          "one for every year you've been gone", "one for the way back home to you"]
CURRENT_LINE = 12   # 78,000 ms <= 78,500 (DemoLibrary.POSITION_MS)
ADD_SONGS = ["404 Heartbeat", "Aadhi Neend", "Afterglow Protocol", "Auto Rickshaw Waltz", "Azulejo", "Baadal Ka Khat",
             "Bheegi Sadak", "Bijli Gayi", "Brushes", "Calle de los Naranjos"]
LONG_DRIVE_ALL = {"Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak", "Kadaisi Vandi", "Drift Mode",
                  "Sheher Ki Shaam", "Swimming Pool Static", "Faro Apagado", "Midnight Checksum", "Auto Rickshaw Waltz",
                  "Second Bus Home"}
SLEEP = ["15 minutes", "30 minutes", "45 minutes", "1 hour", "End of this song", "Off"]
LANGUAGES = ["Tamil", "Hindi", "Telugu", "Malayalam", "Kannada", "English", "No vocals"]


# ======================================================================== small helpers
class S:
    """Instance helpers bound to one Doc."""

    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx = d, ctx
        self._props: dict[int, dict] = {}

    # -- properties
    def props(self, inst: int, values: dict):
        d = self.d
        info = d.cmd("component.properties", id=inst).get("properties", [])
        by = {p["name"].split("#")[0]: p for p in info}
        variants, rest = {}, {}
        for k, v in values.items():
            if k not in by:
                raise KeyError(f"{k!r} isn't a property of {d.get(inst)['name']} ({sorted(by)})")
            p = by[k]
            if p["type"] == "variant":
                variants[p["name"]] = str(v)
            elif p["type"] == "boolean":
                rest[p["name"]] = bool(v)
            elif p["type"] == "instanceSwap":
                rest[p["name"]] = d.comp[v] if isinstance(v, str) else v
            else:
                rest[p["name"]] = v
        if variants:
            d.cmd("instance.setProperties", ids=[inst], properties=variants)
        if rest:
            d.cmd("instance.setProperties", ids=[inst], properties=rest)
        return inst

    def inst(self, comp, parent, *, x=0, y=0, name=None, props=None, w=None, h=None, fill_w=False) -> int:
        d = self.d
        i = d.instance(comp, parent, name=name, x=x, y=y)
        if props:
            self.props(i, props)
        if w is not None or h is not None:
            kw = {}
            if w is not None:
                kw["width"] = w
            if h is not None:
                kw["height"] = h
            d.set(i, **kw)
        if fill_w:
            d.set(i, sizingH="fill")
        return i

    def nested(self, inst: int, name: str, values: dict | None = None) -> int:
        n = self.d.child(inst, name)
        if values:
            self.props(n, values)
        return n

    def cover(self, inst: int, idx: int, layer: str = "Art"):
        cover_image(self.d, inst, COVER[idx], layer)

    def mosaic(self, inst: int, covers):
        for i, c in enumerate(covers[:4]):
            cover_image(self.d, inst, COVER[c], f"Art {i + 1}")

    def slot(self, owner: int, comp: str, *, first=False, props=None, name=None) -> int:
        d = self.d
        r = d.cmd("instance.slotInsert", id=owner, slot=self._slot_name(owner), component=d.comp[comp], replace=first)
        iid = r["id"] if isinstance(r, dict) and "id" in r else r["ids"][0]
        if name:
            d.set(iid, name=name)
        d.set(iid, sizingH="fill")
        if props:
            self.props(iid, props)
        return iid

    def _slot_name(self, owner):
        for p in self.d.cmd("component.properties", id=owner).get("properties", []):
            if p["type"] == "slot":
                return p["name"]
        raise KeyError("no slot")

    def trigger(self, src: str, dst: str, layer: int):
        trig = self.ctx.setdefault("triggers", {})
        if (src, dst) in trig and dst != "back":
            self.ctx.setdefault("triggers_more", []).append((src, dst, layer))
            return
        trig[(src, dst)] = layer

    # -- chrome
    def bars(self, f: int, gesture=True):
        self.inst("Status bar", f, x=0, y=0, name="Status bar")
        if gesture:
            self.gesture(f)

    def gesture(self, f: int, h=H):
        return self.inst("Gesture bar", f, x=0, y=h - NAV, name="Gesture bar")

    def scrim(self, f: int, h=H):
        return self.inst("Scrim", f, x=0, y=0, w=W, h=h, name="Scrim")

    def fade(self, parent, *, x, y, w, h, top_clear=True, stop=1.0, name="Fade", to="Color/ground"):
        """A fade to the ground (Color/derived/ground-clear -> Color/ground) or from it, as the code's gradients."""
        d = self.d
        r = d.rect(parent, name=name, w=w, h=h, x=x, y=y)
        a, b = ("Color/derived/ground-clear", to) if top_clear else (to, "Color/derived/ground-clear")
        d.set(r, fills=[{"type": "linearGradient", "stops": [{"color": "#00000000", "position": 0}, {"color": "#000000", "position": stop}],
                         "from": [0.5, 0], "to": [0.5, 1]}])
        d.bind(r, "fills/0/stops/0/color", d.var_id(a))
        d.bind(r, "fills/0/stops/1/color", d.var_id(b))
        return r


def build(d: Doc, ctx: dict):
    s = S(d, ctx)
    _now_playing_area(d, s)
    _queue_lyrics_area(d, s)
    _sheets_area(d, s)


MOCK = "Code differs from mock"
# Where the code differs from the HTML mock (oneui/05-now-playing.html, 06-queue-lyrics.html, 07-sheets.html).
MOCK_DIFFS = {
    "now_playing_full": ["Credits lists Composer and Genre when the song's tags have them; the mock shows Performed by, "
                         "Album and Year only."],
    "open_with": ["The mock has no file opened from another app. The code plays it here with the Art=None cover, no "
                  "artist line, no lyric line and no wash."],
    "sheet_track_menu": ["The code adds Share to the menu's rows (after Go to artist); the mock has no Share."],
    "sheet_track_menu_playlist": ["The code adds Share before Remove from this playlist; the mock has no Share."],
    "sheet_track_menu_now_playing": ["Opened from Now Playing, the code puts Sleep timer first among the rows and adds "
                                     "Share; the mock has neither."],
    "sheet_track_menu_open_with": ["The mock has no menu for a file from another app; the code shows the song and "
                                   "Sleep timer only."],
    "sheet_sleep_timer": ["The mock has no sleep timer. The code opens its six choices as a pop-over on the Sleep timer "
                          "row, the way the mock's language picker opens."],
    "sleep_timer_active": ["The mock has no sleep timer. While one runs, the row shows what is left (\"23 min left\")."],
}


def _annotate(d: Doc, keys: list[str], notes: dict[str, list[str]], extra=lambda k: []):
    for k in keys:
        sc = d.screens[k]
        for fid in (sc["light"], sc["dark"]):
            d.annotate(fid, f"Compose: {sc['source']}", "Source")
            for n in MOCK_DIFFS.get(k, []):
                d.annotate(fid, n, MOCK)
            for n in notes.get(k, []) + extra(k):
                d.annotate(fid, n.removeprefix("Note: "), "Note")
        d.ready_for_dev([sc["light"], sc["dark"]])


def _finish(d: Doc, a, keys: list[str], notes: dict[str, list[str]] | None = None):
    """Close the area, then annotate both copies and mark them ready."""
    a.close()
    _annotate(d, keys, notes or {})


# ======================================================================== the player
def _player(d: Doc, s: S, f: int, *, kind="now", key=None) -> dict:
    """Now Playing's first page (ComfortNowPlaying.kt NowPlayingPage) into frame f. kind: now | paused_radio | open_with."""
    ids = {}
    s.inst("Status bar", f, name="Status bar")
    hd = s.inst("Now playing header", f, y=STATUS, name="Header")
    s.props(hd, {"Context": "Radio · Lights Over the Water" if kind == "paused_radio" else "Paper Lanterns"})
    ids["close"] = d.child(hd, "Close")
    ids["more"] = d.child(hd, "More")
    cv = s.inst("Now playing cover", f, y=STATUS + 60, name="Cover", props={"Paused": kind == "paused_radio"})
    inner = d.child(cv, "Cover")
    if kind == "open_with":
        s.props(inner, {"Art": "None"})
        d.set(inner, width=351, height=351)  # a variant switch takes the variant's own size
    else:
        s.cover(inner, 0)
    tt = s.inst("Now playing title", f, y=STATUS + 60 + 355, name="Title")
    if kind == "open_with":
        s.props(tt, {"Title": "band-practice"})
        d.override(tt, "Artist", visible=False)
    else:
        ly = s.inst("Now playing lyric", f, y=STATUS + 60 + 355 + 74, name="Lyric")
        ids["lyric"] = ly
    deck = s.inst("Deck", f, x=16, y=H - NAV - 16 - 232, name="Deck", props={"Playing": kind != "paused_radio"})
    ids["deck"] = deck
    tg = d.child(deck, "Toggles")
    if kind == "paused_radio":
        s.props(d.child(tg, "Shuffle"), {"Checked": True, "Icon": "Icon/shuffle (filled)"})
        s.props(d.child(tg, "Repeat"), {"Checked": True, "Icon": "Icon/repeat_one (filled)"})
    if kind == "open_with":
        s.props(d.child(tg, "Favourite"), {"Favourite": False})
        seek = d.child(deck, "Seek bar")
        s.props(seek, {"Elapsed": "0:41", "Total": "3:07"})
        pw = round(64 + (351 - 64) * 41 / 187, 1)
        d.override(seek, "Played", width=pw)   # the tick rides on the played part's end
    ids["queue"] = d.child(tg, "Queue")
    ids["play"] = d.child(d.child(deck, "Transport"), "Play button")
    s.gesture(f)
    return ids


def _np_below(d: Doc, s: S, f: int, y0: float):
    """What follows the first page (NowPlayingPage's second Column): lyrics card, About the artist, Credits."""
    col = d.frame(f, name="Below the player", x=0, y=y0, w=W, layout="v", pad=[12, 0, 28, 0], sizing=("fixed", "hug"))
    lc_box = d.frame(col, name="Lyrics card", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    lc = s.inst("Lyrics card", lc_box, name="Lyrics card", props={"State": "Synced"}, fill_w=True)
    lab = s.inst("Section label", col, name="Label: About the artist", fill_w=True,
                 props={"Spacing": "Default", "Label": "About the artist", "Show small": False, "Show end": False})
    g = d.frame(col, name="Group: artist", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    gc = s.inst("Group card", g, name="Artist group", props={"Pad": "False"}, fill_w=True)
    r = s.slot(gc, "Collection row", first=True, name="Row: Mira Okafor",
               props={"Kind": "Artist", "Size": "Large", "Title": "Mira Okafor", "Subtitle": "14 songs in your library",
                      "Show subtitle": True, "Show chevron": True, "Show value": False, "Show overline": False, "Show hairline": False})
    s.cover(d.child(r, "Lead"), 0)
    s.inst("Section label", col, name="Label: Credits", fill_w=True,
           props={"Spacing": "Default", "Label": "Credits", "Show small": False, "Show end": False})
    g = d.frame(col, name="Group: credits", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    gc = s.inst("Group card", g, name="Credits group", props={"Pad": "False"}, fill_w=True)
    for i, (lb, v) in enumerate((("Performed by", "Mira Okafor"), ("Composer", "Mira Okafor, Idris Bello"),
                                 ("Album", "Paper Lanterns"), ("Year", "2025"), ("Genre", "Indie folk"))):
        s.slot(gc, "Key value", first=i == 0, name=f"Credit: {lb}", props={"Label": lb, "Value": v, "Show hairline": i > 0})
    return lc


def _now_playing_area(d: Doc, s: S):
    a = d.area("Now Playing", note="Now Playing is washed with the cover's colour (Color mode Tinted wash) and follows the "
               "app's theme. Pull down or the chevron closes it; the kebab opens the track menu with Sleep timer first. A file "
               "opened from another app plays here too, with no cover and the shorter menu.")
    src = SRC + "ComfortNowPlaying.kt · ComfortNowPlaying"
    notes = {}

    f = a.screen("now_playing", name="Now Playing / Playing", source=src)
    ids = _player(d, s, f, key="now_playing")
    d.set_mode(f, "Color", WASH)
    if "home" in d.screens:  # the browse module's Home, built before this one
        try:
            s.trigger("home", "now_playing", d.child(d.screens["home"]["light"], "Mini player"))
        except Exception:
            pass
    s.trigger("now_playing", "queue", ids["queue"])
    s.trigger("now_playing", "sheet_track_menu_now_playing", ids["more"])
    s.trigger("now_playing", "back", ids["close"])
    s.trigger("now_playing", "lyrics_synced", ids["lyric"])
    # Pause: the prototype's paused player is the radio one (Now Playing / Paused, radio), the paused state drawn.
    s.trigger("now_playing", "now_playing_paused_radio", ids["play"])
    np_ids = ids

    hfull = 4578 / 2.625
    f = a.screen("now_playing_full", name="Now Playing / Playing (full length)", source=src, h=hfull)
    ids = _player(d, s, f, key="now_playing_full")
    d.set(d.child(f, "Gesture bar"), visible=False)
    lc = _np_below(d, s, f, H)
    s.gesture(f, h=hfull)
    d.set_mode(f, "Color", WASH)
    s.trigger("now_playing_full", "lyrics_synced", d.child(lc, "Open lyrics"))
    s.trigger("now_playing_full", "queue", ids["queue"])
    s.trigger("now_playing_full", "back", ids["close"])
    notes["now_playing_full"] = ["Note: the whole page, stitched like the render. Scrolling past the deck pins the compact "
                                 "header (Mini player, Placement=Top) to the top; it is left out here, as in the render. "
                                 "Credits shows composer and genre when the tags have them (ComfortNowPlaying.kt Credits)."]

    f = a.screen("now_playing_paused_radio", name="Now Playing / Paused, radio", source=src)
    ids = _player(d, s, f, kind="paused_radio")
    d.set_mode(f, "Color", WASH)
    s.trigger("now_playing_paused_radio", "queue_radio", ids["queue"])
    s.trigger("now_playing_paused_radio", "back", ids["close"])
    s.trigger("now_playing_paused_radio", "now_playing", ids["play"])
    notes["now_playing_paused_radio"] = ["Note: in the prototype Pause on Now Playing / Playing comes here and Play goes back. "
                                         "In the app Pause only pauses: the radio context, shuffle and repeat one are this "
                                         "frame's own demo state (DesignStates nowPlayingPausedRadio)."]

    f = a.screen("open_with", name="Now Playing / Opened with Attic", source=src)
    ids = _player(d, s, f, kind="open_with")
    s.trigger("open_with", "sheet_track_menu_open_with", ids["more"])
    # Closing the player shows the screen beneath with the file in the mini player (Home / Mini player, opened with Attic).
    s.trigger("open_with", "mini_player_open_with", ids["close"])
    notes["open_with"] = ["Note: a file opened from a file manager (a VIEW intent, playback/OpenWith.kt) isn't in the library: no "
                          "cover (the Art=None box), no artist, no lyrics, so no wash; Color mode stays Base. \"Playing from\" "
                          "keeps the queue's context. Its menu has only Sleep timer."]
    _finish(d, a, ["now_playing", "now_playing_full", "now_playing_paused_radio", "open_with"], notes)


# ======================================================================== queue and lyrics
def _bar(d: Doc, s: S, f: int, title: str) -> int:
    """Bars.kt Bar with the expand_more back symbol (Queue, Lyrics), on the ground."""
    bg = d.frame(f, name="Bar ground", x=0, y=0, w=W, h=STATUS + 56, fill="Color/ground")
    b = s.inst("Top bar", f, y=STATUS, name="Bar", props={"Back": "Close", "Lift": "False", "Count": "False", "Title": title,
                                                         "Show action 1": False, "Show action 2": False})
    d.cmd("instance.swap", ids=[d.child(d.child(b, "Back"), "Icon")], component=d.comp["Icon/expand_more"])
    s.inst("Status bar", f, name="Status bar")
    return b


def _queue(d: Doc, s: S, f: int, key: str, *, radio=False, empty=False):
    songs = RADIO_QUEUE if radio else QUEUE
    col = d.frame(f, name="Queue list", x=0, y=STATUS + 56, w=W, layout="v", sizing=("fixed", "hug"))
    lab = s.inst("Section label", col, name="Label: Now playing", fill_w=True,
                 props={"Spacing": "Tight", "Label": "Now playing", "Show small": False, "Show end": True,
                        "End": "Radio · Lights Over the Water" if radio else "Paper Lanterns",
                        "End icon": "Icon/sensors" if radio else "Icon/album"})
    d.override(lab, "End icon", width=18, height=18)   # a swapped icon comes in at its own 24dp
    nb = d.frame(col, name="Now playing row", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    now = s.inst("Queue row", nb, name="Now playing", props={"State": "Current"}, fill_w=True)
    s.cover(d.child(now, "Cover"), 0)
    if empty:
        es = d.frame(col, name="Empty state", layout="v", pad=[48, 40, 48, 40], cross="center", sizing=("fill", "hug"))
        ib = s.inst("Icon box", es, name="Icon box", props={"Style": "Neutral", "Icon": "Icon/queue_music"}, w=76, h=76)
        tf = d.frame(es, name="Title", layout="v", pad=[22, 0, 0, 0], sizing=("hug", "hug"))
        d.text(tf, "Nothing up next", style="Scale/Bar title", color="Color/on", name="Title text", textAlign="center")
        return now
    s.inst("Section label", col, name="Label: Up next", fill_w=True,
           props={"Spacing": "Default", "Label": f"Up next, {len(songs)} songs", "Show small": False, "Show end": False})
    g = d.frame(col, name="Up next", layout="h", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
    gc = s.inst("Group card", g, name="Up next group", props={"Pad": "False"}, fill_w=True)
    for i, t in enumerate(songs[:9]):
        artist, c = SONGS[t]
        r = s.slot(gc, "Song row", first=i == 0, name=f"Row: {t}",
                   props={"Lead": "Cover", "Current": "False", "Title": t, "Subtitle": artist, "Show hairline": i > 0,
                          "Show meta": False, "Show trailing": True})
        s.props(d.child(r, "Trailing"), {"Kind": "Drag handle", "Tone": "Default"})
        s.cover(d.child(r, "Cover"), c)
    # ClearFoot: 118dp + the inset, fading to the ground by 62/118, the pill 28dp over the inset
    s.fade(f, x=0, y=H - NAV - 118, w=W, h=118 + NAV, stop=62 / 118, name="Clear fade")
    cl = s.inst("Queue clear", f, name="Clear", x=0, y=0)
    g = d.get(cl)
    d.set(cl, x=(W - g["width"]) / 2, y=H - NAV - 28 - g["height"])
    return now


def _lyrics_deck(d: Doc, s: S, f: int):
    """LyricsDeck: the dock's mini-player row alone, over a fade to the ground (ComfortLyrics.kt)."""
    dk = s.inst("Dock", f, name="Deck", props={"Tab": "None", "Mini player": "True"})
    g = d.get(dk)
    d.set(dk, x=0, y=H - g["height"])
    s.cover(d.child(dk, "Cover"), 0)
    return dk


def _lyrics(d: Doc, s: S, f: int, key: str, state: str):
    if state == "synced":
        clip = d.frame(f, name="Lyrics list", x=0, y=0, w=W, h=H, clip=True)
        # Each text line above the current one is 2dp shorter than its 30dp line height (Lyrics line's note), so the
        # column starts that much lower to keep the current line where the code anchors it.
        above = sum(1 for ln in LYRICS[8:CURRENT_LINE] if ln)
        col = d.frame(clip, name="Lines", x=16, y=83 + 2 * above, w=W - 32, layout="v", sizing=("fixed", "hug"))
        for i in range(8, len(LYRICS)):
            ln = LYRICS[i]
            st = "Blank" if not ln else ("Current" if i == CURRENT_LINE else ("Sung" if i < CURRENT_LINE else "Upcoming"))
            li = s.inst("Lyrics line", col, name=f"Lyric {i}", props={"State": st}, fill_w=True)
            if ln:
                s.props(li, {"Line": ln})
            elif i < CURRENT_LINE:
                d.override(li, "Glyph", fill="Color/derived/lyric-sung")
        s.fade(f, x=0, y=STATUS + 56, w=W, h=72, top_clear=False, stop=1.0, name="Fade under the bar")
    elif state == "plain":
        col = d.frame(f, name="Lyrics text", x=30, y=STATUS + 56 + 6, w=W - 60, layout="v", sizing=("fixed", "hug"))
        paras, cur = [], []
        for ln in LYRICS[1:]:
            if ln:
                cur.append(ln)
            elif cur:
                paras.append(cur)
                cur = []
        paras.append(cur)
        for i, p in enumerate(paras):
            pf = d.frame(col, name=f"Paragraph {i + 1}", layout="v", pad=[0, 0, 22, 0], sizing=("fill", "hug"))
            t = d.text(pf, "\n".join(p), style="Component/Lyrics plain", color="Color/on", name="Paragraph text")
            d.set(t, sizingH="fill", textAutoResize="height")
    else:
        msg = {"none": "No lyrics were found for this song.", "not_downloaded": "Download once and they stay on this phone.",
               "instrumental": "This song is instrumental.", "downloading": "Download once and they stay on this phone."}[state]
        top, bottom = STATUS + 56, NAV + 86
        col = d.frame(f, name="No lyrics", x=0, y=top, w=W, h=H - top - bottom, layout="v", pad=[0, 40, 0, 40],
                      main="center", cross="center", sizing=("fixed", "fixed"))
        s.inst("Icon box", col, name="Icon box", props={"Style": "Neutral", "Icon": "Icon/lyrics"}, w=76, h=76)
        mf = d.frame(col, name="Message", layout="v", pad=[22, 0, 0, 0], cross="center", sizing=("fill", "hug"))
        t = d.text(mf, msg, style="Scale/Body", color="Color/on-2", name="Message text", textAlign="center")
        d.set(t, sizingH="fill", textAutoResize="height")
        af = d.frame(col, name="Action", layout="h", h=48 + 24, pad=[24, 0, 0, 0], main="center", cross="center",
                     sizing=("hug", "fixed"))
        if state == "downloading":
            s.inst("Spinner", af, name="Spinner", props={"Size": "Default"})
        else:
            s.inst("Button", af, name="Download lyrics", props={"Style": "Filled", "Size": "Medium", "State": "Default",
                                                                 "Tone": "Default", "Label": "Download lyrics",
                                                                 "Show leading icon": False})
    _bar(d, s, f, "Lyrics")
    _lyrics_deck(d, s, f)
    s.gesture(f)


def _queue_lyrics_area(d: Doc, s: S):
    a = d.area("Queue and lyrics", note="The queue and full-screen lyrics rise over Now Playing on its washed ground "
               "(Tinted wash). The chevron closes either. In the queue a row moves by its handle, goes with a swipe, and "
               "opens the track menu on a long press; Clear empties Up next.")
    qsrc = SRC + "ComfortQueue.kt · ComfortQueue"
    lsrc = SRC + "ComfortLyrics.kt · ComfortLyrics"
    for key, name, kw in (("queue", "Queue / Album", {}), ("queue_radio", "Queue / Radio", {"radio": True}),
                          ("queue_empty", "Queue / Nothing up next", {"empty": True})):
        f = a.screen(key, name=name, source=qsrc)
        _queue(d, s, f, key, **kw)
        b = _bar(d, s, f, "Queue")
        s.gesture(f)
        d.set_mode(f, "Color", WASH)
        s.trigger(key, "back", d.child(b, "Back"))
    for key, name, st in (("lyrics_synced", "Lyrics / Synced", "synced"), ("lyrics_plain", "Lyrics / Plain text", "plain"),
                          ("lyrics_none", "Lyrics / None found", "none"),
                          ("lyrics_not_downloaded", "Lyrics / Not downloaded", "not_downloaded"),
                          ("lyrics_instrumental", "Lyrics / Instrumental", "instrumental"),
                          ("lyrics_downloading", "Lyrics / Downloading", "downloading")):
        f = a.screen(key, name=name, source=lsrc)
        _lyrics(d, s, f, key, st)
        d.set_mode(f, "Color", WASH)
        s.trigger(key, "back", d.child(d.child(f, "Bar"), "Back"))
    notes = {"lyrics_instrumental": ["Note: an instrumental song still offers Download lyrics on this screen (NoLyrics draws "
                                     "the button for every state but downloading); the card on Now Playing shows the "
                                     "sentence alone."],
             "lyrics_synced": ["Note: the line being sung sits a third of the way down (anchor = height / 3) and grows "
                               "onto its bed over 240 ms; a tap on any line seeks to it."]}
    lyric_wrap = ("FigCraft sets Onest about 2 % wider than Android does, so \"one for the way back home to you\" wraps here "
                  "and not in the render. Sizes, tracking and the 14dp sides are the code's.")
    _finish(d, a, ["queue", "queue_radio", "queue_empty", "lyrics_synced", "lyrics_plain", "lyrics_none",
                   "lyrics_not_downloaded", "lyrics_instrumental", "lyrics_downloading"], notes)
    for fid in (d.screens["lyrics_synced"]["light"], d.screens["lyrics_synced"]["dark"]):
        d.annotate(fid, lyric_wrap, REF_DIFFERS)


# ======================================================================== screens under the overlays
def _dock(d: Doc, s: S, f: int, tab: str) -> int:
    dk = s.inst("Dock", f, name="Dock", props={"Tab": tab, "Mini player": "True"})
    g = d.get(dk)
    d.set(dk, x=0, y=H - g["height"])
    s.cover(d.child(dk, "Cover"), 0)
    return dk


def _home(d: Doc, s: S, f: int) -> dict:
    """ComfortHome.kt as the renders show it: hero, sub-tabs, the shortcuts card, the Recap entry, Instant mixes, dock."""
    s.inst("Status bar", f, name="Status bar")
    s.inst("Hero", f, y=STATUS, name="Hero", props={"Mark": "True", "Short": "False", "Title": "Home", "Show subtitle": False})
    s.inst("Tools", f, y=STATUS + 228, name="Tools", props={"Leading": "Sub-tabs"})
    g = d.frame(f, name="Shortcuts", x=16, y=STATUS + 228 + 50, w=W - 32, layout="v", sizing=("fixed", "hug"))
    gc = s.inst("Group card", g, name="Shortcuts group", props={"Pad": "True"}, fill_w=True)
    # The Pad slot is one row; Home's shortcuts are two rows of three, so a grid frame goes in the slot.
    first = s.slot(gc, "Tile", first=True)
    grid = d.frame(f, name="Grid", layout="v", gap=14, sizing=("hug", "hug"))
    d.cmd("node.reparent", ids=[grid], parent=d.child(gc, "Content"))
    d.cmd("edit.delete", ids=[first])
    tiles = (("Favourites", None), ("Paper Lanterns", 0), ("Iravu Rail", 8), ("Chhat Pe Dhoop", 9),
             ("Glasshouse Summer", 3), ("Afterglow Protocol", 10))
    for rix in range(2):
        row = d.frame(grid, name=f"Row {rix + 1}", layout="h", gap=6, sizing=("hug", "hug"), cross="min")
        for t, c in tiles[rix * 3:rix * 3 + 3]:
            tl = s.inst("Tile", row, name=f"Tile: {t}", props={"Kind": "Shortcut", "Current": "False", "Title": t,
                                                               "Show icon box": c is None})
            d.set(tl, width=113)
            if c is not None:
                s.cover(d.child(tl, "Cover"), c)
    gy = d.get(g)
    r = d.frame(f, name="Recap", x=16, y=gy["y"] + gy["height"] + 12, w=W - 32, layout="v", sizing=("fixed", "hug"))
    rc = s.inst("Group card", r, name="Recap group", props={"Pad": "False"}, fill_w=True)
    re = s.slot(rc, "Recap entry", first=True, name="Recap entry")
    s.mosaic(d.child(re, "Mosaic"), [0, 8, 9, 3])
    ry = d.get(r)
    s.inst("Section label", f, y=ry["y"] + ry["height"], w=W, name="Label: Instant mixes",
           props={"Spacing": "Default", "Label": "Instant mixes", "Show small": False, "Show end": False})
    dk = _dock(d, s, f, "Home")
    s.gesture(f)
    return {"mini": d.child(dk, "Mini player")}


def _playlist(d: Doc, s: S, f: int) -> dict:
    """ComfortCollections.kt ComfortPlaylist for Long drive: bar, mosaic header, play row, tools and songs, dock."""
    s.inst("Status bar", f, name="Status bar")
    bar = s.inst("Top bar", f, y=STATUS, name="Bar", props={"Back": "Arrow", "Lift": "False", "Count": "False", "Title": "",
                                                           "Show action 1": False, "Show action 2": True})
    hd = s.inst("Collection header", f, y=STATUS + 56, name="Header", props={"Kind": "Album", "Title": "Long drive",
                                                                                 "Meta": "12 songs, 44 min", "Show byline": False})
    art = d.child(hd, "Art")
    d.cmd("instance.swap", ids=[art], component=d.comp["Cover/Mosaic"])
    s.props(art, {"Shape": "Squircle large"})
    d.set(art, width=176, height=176)  # ComfortCollections.kt HeaderArt
    s.mosaic(art, [10, 8, 3, 6])
    hg = d.get(hd)
    pr = s.inst("Collection actions", f, x=0, y=hg["y"] + hg["height"], name="Play row", props={"Playing": "False"})
    pg = d.get(pr)
    col = d.frame(f, name="Songs", x=16, y=pg["y"] + pg["height"] + 16, w=W - 32, layout="v", sizing=("fixed", "hug"))
    gc = s.inst("Group card", col, name="Songs group", props={"Pad": "False"}, fill_w=True)
    # The songs card opens with Playlist tools (screens_browse's component), then the songs.
    s.slot(gc, "Playlist tools", first=True, name="Playlist tools", props={"Reorder": "True"})
    for i, t in enumerate(LONG_DRIVE):
        artist, c = SONGS[t]
        r = s.slot(gc, "Song row", name=f"Row: {t}",
                   props={"Lead": "Cover", "Current": "False", "Title": t, "Subtitle": artist, "Show hairline": i > 0,
                          "Show meta": False, "Show trailing": True})
        s.props(d.child(r, "Trailing"), {"Kind": "More", "Tone": "Default"})
        s.cover(d.child(r, "Cover"), c)
    dk = _dock(d, s, f, "Library")
    s.gesture(f)
    kebab = d.child(bar, "Action 2")
    d.set(kebab, name="More")
    return {"more": kebab, "back": d.child(bar, "Back")}


def _library(d: Doc, s: S, f: int):
    """ComfortLibrary.kt as the dialog_name_playlist render shows it."""
    s.inst("Status bar", f, name="Status bar")
    s.inst("Hero", f, y=STATUS, name="Hero", props={"Mark": "False", "Short": "False", "Title": "Library", "Show subtitle": False})
    s.inst("Tools", f, y=STATUS + 228, name="Tools", props={"Leading": "None"})
    s.inst("Chip row", f, y=STATUS + 228 + 50, name="Chips")
    s.inst("Section label", f, y=STATUS + 228 + 50 + 48, w=W, name="Label: Playlists",
           props={"Spacing": "Default", "Label": "Playlists", "Show small": False, "Show end": False})
    col = d.frame(f, name="Playlists", x=16, y=STATUS + 228 + 50 + 48 + 41 + 10, w=W - 32, layout="v", sizing=("fixed", "hug"))
    gc = s.inst("Group card", col, name="Playlists group", props={"Pad": "False"}, fill_w=True)
    # Counts as the playlists hold them (screens_browse.LIBRARY_COUNTS explains the render's 42, 25, 32 and 39).
    for i, (t, sub, kind, cov) in enumerate((("Favourites", "12 songs", "Favourites", None), ("Long drive", "12 songs", "Playlist", [10, 8, 3, 6]),
                                            ("Sunday cleaning", "5 songs", "Playlist", [3, 9, 2, 7]), ("Focus", "4 songs", "Album", [1]))):
        r = s.slot(gc, "Collection row", first=i == 0, name=f"Row: {t}",
                   props={"Kind": kind, "Size": "Medium", "Title": t, "Subtitle": sub, "Show subtitle": True,
                          "Show hairline": i > 0, "Show chevron": False, "Show value": False, "Show overline": False})
        if cov and len(cov) == 4:
            s.mosaic(d.child(r, "Lead"), cov)
        elif cov:  # fewer than four different covers: the one cover (Artwork.kt Mosaic)
            s.cover(d.child(r, "Lead"), cov[0])
    _dock(d, s, f, "Library")
    s.gesture(f)


# ======================================================================== overlays
def _sheet(d: Doc, s: S, f: int, name="Sheet") -> int:
    """A Sheet as a phone draws it: ComfortSheet's navigationBarsPadding() puts the 24dp navigation inset under the 30dp
    foot (the JVM renders have no inset there; see HARNESS_NOTES)."""
    sh = s.inst("Sheet", f, name=name)
    d.override(sh, "Foot", height=30 + NAV)
    return sh


def _place_bottom(d: Doc, node: int, gap: float = 0):
    g = d.get(node)
    d.set(node, x=(W - g["width"]) / 2, y=H - gap - g["height"])
    return node


def _track_menu(d: Doc, s: S, f: int, *, title="Porch Light", artist="Mira Okafor", cover=0, liked=False, playlist=False,
                now_playing=False, open_with=False, timer=None) -> dict:
    """ComfortSheets.kt ComfortTrackMenu."""
    ids = {}
    sh = _sheet(d, s, f, "Track menu")
    hd = s.slot(sh, "Sheet header", first=True, name="Sheet header", props={"Title": title, "Show subtitle": not open_with})
    if artist:
        s.props(hd, {"Subtitle": artist})
    if open_with:
        hc = d.child(hd, "Cover")
        s.props(hc, {"Art": "None"})
        d.set(hc, width=48, height=48)
    else:
        s.cover(d.child(hd, "Cover"), cover)
    rows = []
    if not open_with:
        qa = s.slot(sh, "Quick actions", name="Quick actions")
        if liked:
            fav = d.child(qa, "Favourite")
            s.props(fav, {"Label": "Remove from favourites"})
            s.props(d.child(fav, "Button"), {"Checked": "True", "Icon": "Icon/favorite (filled)"})
        ids["quick"] = qa
    if now_playing or open_with:
        rows.append(("Sleep timer", "bedtime", "Pop-over", "Default", timer))
    if not open_with:
        rows += [("Add to playlist", "playlist_add", "Chevron", "Default", None),
                 ("Language: English", "language", "Pop-over", "Default", None),
                 ("Go to album", "album", "None", "Default", None), ("Go to artist", "person", "None", "Default", None),
                 ("Share", "share", "None", "Default", None)]
        if playlist:
            rows.append(("Remove from this playlist", "do_not_disturb_on", "None", "Danger", None))
    for i, (label, lig, end, kind, value) in enumerate(rows):
        r = s.slot(sh, "Sheet row", name=f"Row: {label}",
                   props={"Kind": kind, "End": end, "Label": label, "Icon": f"Icon/{lig}", "Show icon": True,
                          "Show hairline": i > 0, "Show value": value is not None})
        if value:
            s.props(r, {"Value": value})
        ids[label] = r
    _place_bottom(d, sh)
    s.gesture(f)
    ids["sheet"] = sh
    return ids


def _popover(d: Doc, s: S, f: int, caption: str, options: list[str], selected: str, *, x, y, name) -> int:
    m = s.inst("Menu", f, x=x, y=y, name=name, props={"On sheet": "True"})
    s.slot(m, "Menu caption", first=True, name="Caption", props={"Text": caption})
    rows = {}
    for o in options:
        r = s.slot(m, "Menu row", name=f"Row: {o}", props={"Kind": "Selected" if o == selected else "Default", "Label": o,
                                                           "Show icon": False, "Show check": o == selected})
        rows[o] = r
    return m, rows


def _picker(d: Doc, s: S, f: int, gesture: bool = True) -> dict:
    """ComfortSheets.kt ComfortPlaylistPicker."""
    sh = _sheet(d, s, f, "Playlist picker")
    s.slot(sh, "Sheet title", first=True, name="Sheet title", props={"Title": "Add to playlist", "Show subtitle": False})
    new = s.slot(sh, "Collection row", name="Row: New playlist",
                 props={"Kind": "Favourites", "Size": "Medium", "Title": "New playlist", "Show subtitle": False,
                        "Show hairline": False, "Show chevron": False, "Show value": False, "Show overline": False})
    s.props(d.child(new, "Lead"), {"Style": "Neutral", "Icon": "Icon/add"})
    rows = {"new": new}
    for t, n, cov, tick in (("Long drive", "12 songs", [10, 8, 3, 6], False), ("Sunday cleaning", "5 songs", [3, 9, 2, 7], True),
                            ("Focus", "4 songs", [1], False)):
        r = s.slot(sh, "Collection row", name=f"Row: {t}",
                   props={"Kind": "Playlist" if len(cov) == 4 else "Album", "Size": "Medium", "Title": t, "Subtitle": n,
                          "Show subtitle": True, "Show hairline": True, "Show chevron": tick, "Show value": False,
                          "Show overline": False})
        if tick:
            # Swap first: swapping a nested instance drops the row's other overrides (the mosaic's images).
            ic = d.child(r, "Chevron icon")
            d.cmd("instance.swap", ids=[ic], component=d.comp["Icon/check"])
            d.override(r, "Chevron icon", width=24, height=24)
            d.override(ic, "Glyph", fill="Color/accent-ink")
        if len(cov) == 4:
            s.mosaic(d.child(r, "Lead"), cov)
        else:
            s.cover(d.child(r, "Lead"), cov[0])
        rows[t] = r
    for r in [new] + [rows[t] for t in ("Long drive", "Sunday cleaning", "Focus")]:
        d.set(r, paddingLeft=24, paddingRight=18)
    _place_bottom(d, sh)
    if gesture:
        s.gesture(f)
    rows["sheet"] = sh
    return rows


def _add_songs(d: Doc, s: S, f: int):
    """ComfortSheets.kt ComfortAddSongs: the tall sheet, 90 % of the height under the status bar."""
    sh = _sheet(d, s, f, "Add songs")
    s.slot(sh, "Sheet title", first=True, name="Sheet title", props={"Title": "Add songs", "Subtitle": "Long drive, 12 songs",
                                                                       "Show subtitle": True})
    # SearchField keeps its 16dp margins and takes 6 under it (Modifier.padding(bottom = 6.dp)).
    box = d.frame(f, name="Search box", w=W, layout="v", pad=[0, 16, 6, 16], sizing=("fixed", "hug"))
    d.cmd("node.reparent", ids=[box], parent=d.child(sh, "Content"))
    d.set(box, sizingH="fill")
    s.inst("Search field", box, name="Search", props={"State": "Empty", "Container": "Card", "Placeholder": "Find a song"},
           fill_w=True)
    for i, t in enumerate(ADD_SONGS):
        artist, c = SONGS[t]
        r = s.slot(sh, "Song row", name=f"Row: {t}",
                   props={"Lead": "Cover", "Current": "False", "Title": t, "Subtitle": artist, "Show hairline": i > 0,
                          "Show meta": False, "Show trailing": True})
        s.props(d.child(r, "Trailing"), {"Kind": "Added" if t in LONG_DRIVE_ALL else "Add", "Tone": "Default"})
        s.cover(d.child(r, "Cover"), c)
        d.set(r, paddingLeft=24)
    # Column(Modifier.fillMaxHeight(0.9f)) inside navigationBarsPadding() and the 30dp foot: 0.9 x (914 - 28 - 24 - 30)
    # = 749 of list, plus the 28dp grab and the 54dp foot, so the sheet is 831 tall and its top 83dp down. (The JVM
    # render, with no inset: 829, 85 down.)
    foot = 30 + NAV
    hsheet = round(28 + 0.9 * (H - 28 - foot) + foot)
    d.set(sh, height=hsheet, sizingV="fixed", y=H - hsheet, x=0)
    # The list fades out at the sheet's foot: Transparent to raised by 62 % of 64dp, over the last rows.
    s.fade(f, x=0, y=H - foot - 64, w=W, h=64, stop=0.62, name="Add songs foot fade", to="Color/raised")
    s.fade(f, x=0, y=H - foot, w=W, h=foot, stop=0.0, name="Add songs foot", to="Color/raised")
    s.gesture(f)
    return sh


def _dialog(d: Doc, s: S, f: int, *, content, buttons="Two", confirm="Default", title, body=None, value=None,
            dismiss="Cancel", confirm_label="Create") -> int:
    dl = s.inst("Dialog", f, name="Dialog", props={"Content": content, "Buttons": buttons, "Confirm": confirm, "Title": title,
                                                    "Confirm label": confirm_label})
    if buttons == "Two":
        s.props(dl, {"Dismiss label": dismiss})
    if body:
        s.props(dl, {"Body": body})
    if content == "Field":
        fld = d.child(dl, "Field")
        s.props(fld, {"State": "Filled" if value else "Empty", "Label": "Playlist name"})
        if value:
            s.props(fld, {"Value": value})
    g = d.get(dl)
    # ComfortDialog: navigationBarsPadding(), then 12dp sides and 22 below (the JVM render has no inset: 22 from the edge).
    d.set(dl, x=12, y=H - NAV - 22 - g["height"])
    return dl


def _dark_sheet_fix(d: Doc, s: S, key: str):
    """On a dark sheet a tonal control is one step up: Quick action's Button Container=Fill 2, the neutral icon box fill-2."""
    dark = d.screens[key]["dark"]
    for i in d.find(dark, name="Button", limit=50):
        n = d.get(i)
        if n.get("name") == "Button" and n.get("type") == "INSTANCE" and abs(n.get("width", 0) - 54) < 1:
            props = {p["name"].split("#")[0]: p.get("value") for p in d.cmd("component.properties", id=i).get("properties", [])}
            if str(props.get("Checked")) != "True":  # a checked toggle is the accent whatever its container
                s.props(i, {"Container": "Fill 2"})
    for i in d.find(dark, name="Row: New playlist", limit=5):
        lead = d.child(i, "Lead")
        d.override(lead, "Container", fill="Color/fill-2")


def _toast(d: Doc, s: S, f: int, text: str, bottom: float) -> int:
    t = s.inst("Toast", f, name="Toast", props={"Text": text})
    g = d.get(t)
    d.set(t, x=(W - g["width"]) / 2, y=H - bottom - g["height"])
    return t


def _sheets_area(d: Doc, s: S):
    a = d.area("Sheets, dialogs and messages", note="Sheets and dialogs sit at the bottom over the scrim; a short choice "
               "(sleep timer, song language) opens as a pop-over on the row that asked. Over Now Playing they take the "
               "player's wash. Share hands the song to the Android share sheet: Attic draws nothing for it, so Share is the "
               "track menu's Share row.")
    sheets = SRC + "ComfortSheets.kt · "
    notes: dict[str, list[str]] = {}
    keys = []

    def screen(key, name, source):
        keys.append(key)
        return a.screen(key, name=name, source=source)

    # -- track menus
    f = screen("sheet_track_menu", "Track menu / From a song", sheets + "ComfortTrackMenu")
    _home(d, s, f)
    s.scrim(f)
    ids = _track_menu(d, s, f)
    s.trigger("sheet_track_menu", "snackbar", d.child(ids["quick"], "Add to queue"))
    s.trigger("sheet_track_menu", "album", ids["Go to album"])
    s.trigger("sheet_track_menu", "sheet_playlist_picker", ids["Add to playlist"])
    s.trigger("sheet_track_menu", "sheet_language_picker", ids["Language: English"])
    s.trigger("sheet_track_menu", "back", d.child(f, "Scrim"))
    notes["sheet_track_menu"] = ["Note: Share opens the Android share sheet with the song's file (ui/SongShare.kt); Attic "
                                 "draws no screen of its own for it, so the prototype stops at this row."]

    f = screen("sheet_track_menu_playlist", "Track menu / In a playlist", sheets + "ComfortTrackMenu")
    _playlist(d, s, f)
    s.scrim(f)
    ids = _track_menu(d, s, f, title="Iravu Rail", artist="Kavya Raman", cover=8, liked=True, playlist=True)
    s.trigger("sheet_track_menu_playlist", "back", d.child(f, "Scrim"))
    s.trigger("sheet_track_menu_playlist", "artist", ids["Go to artist"])

    def over_player(key, name, kind="now", **kw):
        f = screen(key, name, sheets + "ComfortTrackMenu")
        _player(d, s, f, kind=kind)
        s.scrim(f)
        if kind == "open_with":
            ids = _track_menu(d, s, f, title="band-practice", artist=None, open_with=True, **kw)
        else:
            ids = _track_menu(d, s, f, title="Lights Over the Water", artist="Mira Okafor", cover=0, liked=True,
                              now_playing=True, **kw)
            d.set_mode(f, "Color", WASH)
        return f, ids

    f, ids = over_player("sheet_track_menu_now_playing", "Track menu / From Now Playing")
    s.trigger("sheet_track_menu_now_playing", "sheet_sleep_timer", ids["Sleep timer"])
    s.trigger("sheet_track_menu_now_playing", "sheet_playlist_picker", ids["Add to playlist"])
    s.trigger("sheet_track_menu_now_playing", "snackbar_over_player", d.child(ids["quick"], "Play next"))
    s.trigger("sheet_track_menu_now_playing", "album", ids["Go to album"])
    s.trigger("sheet_track_menu_now_playing", "back", d.child(f, "Scrim"))

    f, ids = over_player("sheet_track_menu_open_with", "Track menu / Opened with Attic", kind="open_with")
    s.trigger("sheet_track_menu_open_with", "back", d.child(f, "Scrim"))
    notes["sheet_track_menu_open_with"] = ["Note: a song from another app isn't in the library, so the menu keeps only the "
                                           "sleep timer (ComfortTrackMenu: state.external)."]

    f, ids = over_player("sheet_sleep_timer", "Sleep timer / Choices")
    m, rows = _popover(d, s, f, "Sleep timer", SLEEP, "Off", x=W - 214, y=169 - NAV, name="Sleep timer pop-over")
    s.trigger("sheet_sleep_timer", "snackbar_sleep_timer_set", rows["30 minutes"])
    s.trigger("sheet_sleep_timer", "back", d.child(f, "Scrim"))
    notes["sheet_sleep_timer"] = ["Note: the choices open as a pop-over on the Sleep timer row (Menu, On sheet), toward the "
                                  "row's end; a choice sets the timer and closes the menu, a tap outside closes only the "
                                  "pop-over. \"Music will pause in 30 minutes\" follows as a toast."]

    f, ids = over_player("sleep_timer_active", "Sleep timer / Running", timer="23 min left")
    s.trigger("sleep_timer_active", "back", d.child(f, "Scrim"))
    s.trigger("sleep_timer_active", "sheet_sleep_timer_running", ids["Sleep timer"])

    f, ids = over_player("sheet_sleep_timer_running", "Sleep timer / Choices, running", timer="23 min left")
    m, rows = _popover(d, s, f, "Sleep timer", SLEEP, "30 minutes", x=W - 214, y=169 - NAV, name="Sleep timer pop-over")
    s.trigger("sheet_sleep_timer_running", "snackbar_sleep_timer_off", rows["Off"])
    s.trigger("sheet_sleep_timer_running", "back", d.child(f, "Scrim"))
    notes["sheet_sleep_timer_running"] = ["Note: drawn from the code; there is no reference render. With a timer running "
                                          "the pop-over ticks the running choice; Off cancels it."]
    notes["sleep_timer_active"] = ["Note: a 30-minute timer with 22:41 left reads \"23 min left\" (minutes rounded up). Attic "
                                   "shows the running timer nowhere else."]

    # -- language picker
    f = screen("sheet_language_picker", "Track menu / Song language", sheets + "ComfortTrackMenu")
    _home(d, s, f)
    s.scrim(f)
    _track_menu(d, s, f)
    _popover(d, s, f, "Song language", LANGUAGES, "English", x=W - 214, y=223 - NAV, name="Language pop-over")
    s.trigger("sheet_language_picker", "back", d.child(f, "Scrim"))

    # -- playlist picker, add songs
    f = screen("sheet_playlist_picker", "Playlist picker", sheets + "ComfortPlaylistPicker")
    _home(d, s, f)
    s.scrim(f)
    rows = _picker(d, s, f)
    s.trigger("sheet_playlist_picker", "dialog_new_playlist", rows["new"])
    s.trigger("sheet_playlist_picker", "back", d.child(f, "Scrim"))

    f = screen("sheet_add_songs", "Add songs", sheets + "ComfortAddSongs")
    _playlist(d, s, f)
    s.scrim(f)
    _add_songs(d, s, f)
    s.trigger("sheet_add_songs", "back", d.child(f, "Scrim"))

    # -- dialogs
    f = screen("dialog_new_playlist", "Playlist name / New, from the picker", sheets + "ComfortPlaylistNameDialog")
    _home(d, s, f)
    s.scrim(f)
    _picker(d, s, f, gesture=False)
    s.scrim(f)
    s.gesture(f)
    dl = _dialog(d, s, f, content="Field", confirm="Disabled", title="New playlist", confirm_label="Create")
    s.trigger("dialog_new_playlist", "back", d.child(dl, "Dismiss"))

    f = screen("dialog_name_playlist", "Playlist name / From Library", sheets + "ComfortPlaylistNameDialog")
    _library(d, s, f)
    s.scrim(f)
    s.gesture(f)
    dl = _dialog(d, s, f, content="Field", title="Name this playlist", value="Road trip", confirm_label="Create")
    s.trigger("dialog_name_playlist", "back", d.child(dl, "Dismiss"))
    s.trigger("dialog_name_playlist", "playlist_empty", d.child(dl, "Confirm"))
    notes["dialog_name_playlist"] = ["Note: Create opens the new, empty playlist (Playlist / Empty)."]

    f = screen("dialog_rename_playlist", "Playlist name / Rename", sheets + "ComfortPlaylistNameDialog")
    _playlist(d, s, f)
    s.scrim(f)
    s.gesture(f)
    dl = _dialog(d, s, f, content="Field", title="Rename playlist", value="Long drive", confirm_label="Rename")
    s.trigger("dialog_rename_playlist", "back", d.child(dl, "Dismiss"))
    s.trigger("dialog_rename_playlist", "playlist", d.child(dl, "Confirm"))

    f = screen("dialog_delete_playlist", "Delete playlist", sheets + "ComfortDeletePlaylistDialog")
    _playlist(d, s, f)
    s.scrim(f)
    s.gesture(f)
    dl = _dialog(d, s, f, content="Body", confirm="Danger", title="Delete Long drive?",
                 body="The playlist goes away. The songs stay in your library.", confirm_label="Delete playlist")
    s.trigger("dialog_delete_playlist", "back", d.child(dl, "Dismiss"))
    s.trigger("dialog_delete_playlist", "library_list", d.child(dl, "Confirm"))
    notes["dialog_delete_playlist"] = ["Note: Delete playlist removes it and goes back to Library."]

    f = screen("menu_playlist_overflow", "Playlist menu", SRC + "ComfortCollections.kt · ComfortPlaylist")
    pids = _playlist(d, s, f)
    m = s.inst("Menu", f, x=W - 10 - 200, y=94, name="Playlist menu", props={"On sheet": "False"})
    s.slot(m, "Menu row", first=True, name="Row: Rename", props={"Kind": "Default", "Label": "Rename", "Icon": "Icon/edit",
                                                                 "Show icon": True, "Show check": False})
    dr = s.slot(m, "Menu row", name="Row: Delete playlist", props={"Kind": "Danger", "Label": "Delete playlist",
                                                                    "Icon": "Icon/delete", "Show icon": True, "Show check": False})
    s.trigger("menu_playlist_overflow", "dialog_rename_playlist", d.child(m, "Row: Rename"))
    s.trigger("menu_playlist_overflow", "dialog_delete_playlist", dr)

    # -- messages
    f = screen("snackbar", "Message / Over Home", SRC + "Overlays.kt · ToastHost")
    hids = _home(d, s, f)
    dk = d.child(f, "Dock")
    _toast(d, s, f, "Added to queue", d.get(dk)["height"] + 6)
    s.trigger("snackbar", "now_playing", hids["mini"])
    notes["snackbar"] = ["Note: messages are snackbars drawn as the toast, centred 6dp above the dock (ComfortShell.kt "
                         "ToastHost), for about four seconds."]

    f = screen("snackbar_over_player", "Message / Over Now Playing", SRC + "Overlays.kt · ToastHost")
    _player(d, s, f)
    d.set_mode(f, "Color", WASH)
    _toast(d, s, f, "Playing next", NAV + 32)
    notes["snackbar_over_player"] = ["Note: over Now Playing, the queue or lyrics the toast sits 32dp above the gesture inset."]

    for key, name, text, kind in (
            ("snackbar_sleep_timer_set", "Message / Sleep timer set", "Music will pause in 30 minutes", "player"),
            ("snackbar_sleep_timer_off", "Message / Sleep timer off", "Sleep timer off", "player"),
            ("snackbar_share_failed", "Message / Couldn't share", "Couldn't share this song", "home"),
            ("snackbar_open_failed", "Message / Couldn't open a file", "Couldn't open this file", "home")):
        f = screen(key, name, SRC + "Overlays.kt · ToastHost")
        if kind == "player":
            pids = _player(d, s, f)
            d.set_mode(f, "Color", WASH)
            _toast(d, s, f, text, NAV + 32)
        else:
            hids = _home(d, s, f)
            _toast(d, s, f, text, d.get(d.child(f, "Dock"))["height"] + 6)
            s.trigger(key, "now_playing", hids["mini"])
        if key == "snackbar_sleep_timer_set":
            s.trigger(key, "sleep_timer_active", pids["more"])
        why = {"snackbar_sleep_timer_set": "a sleep timer choice (\"Music will pause after this song\" for End of this song)",
               "snackbar_sleep_timer_off": "Off in the sleep timer pop-over",
               "snackbar_share_failed": "Share, when no app can take the file or the song isn't in the library "
                                        "(SheetModels.kt)",
               "snackbar_open_failed": "a file another app hands over that Attic can't read (playback/OpenWith.kt); "
                                       "nothing else changes"}[key]
        notes[key] = [f"Note: drawn from the code; there is no reference render. Shown after {why}, as the toast "
                      + ("32dp above the gesture inset." if kind == "player" else "6dp above the dock.")]

    a.close()
    for k in ("sheet_track_menu", "sheet_track_menu_playlist", "sheet_track_menu_now_playing", "sheet_sleep_timer",
              "sleep_timer_active", "sheet_sleep_timer_running", "sheet_language_picker", "sheet_playlist_picker", "dialog_new_playlist"):
        _dark_sheet_fix(d, s, k)
    _annotate(d, keys, notes)
    for k in keys:
        kind = "dialog" if k.startswith("dialog_") else "sheet" if k.startswith("sheet_") or k == "sleep_timer_active" else None
        if kind and k not in NO_REF_KEYS:
            for fid in (d.screens[k]["light"], d.screens[k]["dark"]):
                d.annotate(fid, HARNESS_NOTES[kind], REF_DIFFERS)
    from .screens_browse import LIBRARY_COUNTS
    for fid in (d.screens["dialog_name_playlist"]["light"], d.screens["dialog_name_playlist"]["dark"]):
        d.annotate(fid, LIBRARY_COUNTS, REF_DIFFERS)
