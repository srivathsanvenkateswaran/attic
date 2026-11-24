"""Screens: Now Playing, Queue, Lyrics, every sheet, dialog, menu and snackbar, the sleep timer, Share, Open with.

Owner: playback screens agent. Pages: Screens (areas "Now Playing", "Queue and lyrics", "Sheets, dialogs and
messages"), and Components / Playback for the parts below that the core module does not draw.

Components added on Components / Playback (below the core sets, all Cover mode)
-------------------------------------------------------------------------------
Now playing/Cover pager     CoverPager (ExpressiveNowPlaying.kt)  Playing=True|False, Art=Cover|None
Now playing/Title block     TitleBlock + InlineLyric (ExpressiveNowPlaying.kt)  Lyric=True|False
                            [Lyric line, Title, Artist]
Lyrics/Top bar              TopBar (ExpressiveLyrics.kt)  [Title, Artist]
Lyrics/Bottom bar           BottomBar (ExpressiveLyrics.kt)  Playing=True|False  [Elapsed, Total]
Lyrics/Empty                NoLyrics (ExpressiveLyrics.kt)  State=Not found|Not downloaded|Instrumental|Downloading

Screens are built from those, the core sets and the overlay sets (components_overlays.py). Backgrounds under
overlays (Home, the Long drive playlist, Library, Now Playing) are rebuilt here from instances by helpers that
each overlay screen calls. Prototype triggers go to ctx["triggers"][(source key, destination key or "back")].
"""
from __future__ import annotations

from figbuild.kit import Doc, V, SCREEN_W, SCREEN_H
from expressive.foundations import (DocPage, hrow, bind_dim, cover_path, dialog_y, paragraph_line_heights, RENDER_DIFFERS, SHEET_INSET_NOTE,
                                    DIALOG_INSET_NOTE)
from expressive.components_core import (Spec, comp, frame, txt, inst, set_text, set_cover, spacer, vname, set_progress,
                                        wave_path, stroke_vec, PROGRESS)
from expressive.components_overlays import set_glyph, set_mosaic, wallpaper, set_nested_text

KEYS = ["now_playing", "now_playing_full", "now_playing_paused_radio", "open_with",
        "queue", "queue_radio", "queue_empty",
        "lyrics_synced", "lyrics_plain", "lyrics_none", "lyrics_not_downloaded", "lyrics_instrumental",
        "lyrics_downloading",
        "sheet_track_menu", "sheet_track_menu_playlist", "sheet_track_menu_now_playing", "sheet_track_menu_open_with",
        "sheet_sleep_timer", "sleep_timer_active", "sheet_playlist_picker", "sheet_language_picker", "sheet_add_songs",
        "dialog_new_playlist", "dialog_name_playlist", "dialog_rename_playlist", "dialog_delete_playlist",
        "menu_playlist_overflow", "snackbar", "snackbar_over_player"]
REFS: dict[str, str] = {k: f"expressive-{{theme}}-{k}" for k in KEYS}

SRC = "ui/expressive/"
NP = SRC + "ExpressiveNowPlaying.kt · ExpressiveNowPlaying"
SHEETS = SRC + "ExpressiveSheets.kt · "

# DemoLibrary.lyrics, the words written for the store shots; a blank line shows as ♪ (lyric_blank_line).
LYRICS = ["♪", "I left the porch light on for you", "the moths are writing letters on the glass",
          "every bus that turns the corner", "sounds a little like you coming back", "♪",
          "and the kettle knows your name", "the stairs still creak the way you climbed them",
          "I kept the window open in the rain", "in case the night forgot to bring you", "♪",
          "paper lanterns over the water", "carry what we couldn't say", "let them drift until the morning",
          "let them burn the long way", "♪", "I counted boats along the harbour", "named the lights that didn't move",
          "one for every year you've been gone", "one for the way back home to you"]
CURRENT = 12   # "carry what we couldn't say" at 1:18.5
# Where the renders break the lines (LineBreak.Heading balances them; a design tool wraps greedily).
BREAKS = {"I kept the window open in the rain": "I kept the window\nopen in the rain",
          "in case the night forgot to bring you": "in case the night\nforgot to bring you",
          "carry what we couldn't say": "carry what we\ncouldn't say",
          "I counted boats along the harbour": "I counted boats\nalong the harbour",
          "named the lights that didn't move": "named the lights\nthat didn't move",
          "one for every year you've been gone": "one for every year\nyou've been gone",
          "one for the way back home to you": "one for the way\nback home to you",
          "the moths are writing letters on the glass": "the moths are writing\nletters on the glass",
          "sounds a little like you coming back": "sounds a little like\nyou coming back",
          "the stairs still creak the way you climbed them": "the stairs still creak the\nway you climbed them",
          "every bus that turns the corner": "every bus that turns the corner"}

# The plain text wraps these lines at the render's width (same LineBreak.Heading); the rest fit on one line.
PLAIN_BREAKS = {k: BREAKS[k] for k in ("the moths are writing letters on the glass", "sounds a little like you coming back",
                                       "the stairs still creak the way you climbed them", "I kept the window open in the rain",
                                       "in case the night forgot to bring you", "I counted boats along the harbour")}

QUEUE = [("Porch Light", "Mira Okafor", 0), ("Moth Letters", "Mira Okafor", 0), ("Second Bus Home", "Mira Okafor", 0),
         ("Salt in the Radio", "Mira Okafor", 0), ("Paper Lanterns", "Mira Okafor", 0),
         ("Small Hours, Big Sky", "Mira Okafor", 0), ("Coming Back Slow", "Mira Okafor", 0),
         ("Glasshouse", "Juniper Vale", 3), ("Neon Peaches", "Juniper Vale", 3), ("Iravu Rail", "Kavya Raman", 8),
         ("Platform Ettu", "Kavya Raman", 8)]
RADIO = [("Porch Light", "Mira Okafor", 0), ("Clear by Morning", "Mira Okafor", 11), ("Glasshouse", "Juniper Vale", 3),
         ("Late Bloom", "Juniper Vale", 3), ("Moth Letters", "Mira Okafor", 0), ("Low Tide Hymn", "Mira Okafor", 11),
         ("Umbrella Song", "Mira Okafor", 11), ("Sunburn Choir", "Juniper Vale", 3),
         ("Coming Back Slow", "Mira Okafor", 0), ("Fog on Garden Road", "Mira Okafor", 11)]


class Kit:
    """Shortcuts over the doc for this module."""

    def __init__(self, d: Doc, ctx: dict):
        self.d, self.ctx, self.C = d, ctx, ctx["components"]

    def p(self, set_name, label):
        return self.C[set_name]["props"][label]

    def setp(self, instance, set_name, **values):
        """Text, boolean and swap properties by label (underscores for spaces)."""
        out = {self.p(set_name, k.replace("_", " ")): v for k, v in values.items()}
        self.d.cmd("instance.setProperties", ids=[instance], properties=out)

    def variant(self, instance, **vp):
        self.d.cmd("instance.setProperties", ids=[instance], properties={k.replace("_", " "): str(v) for k, v in vp.items()})

    def icon_id(self, lig, filled=False):
        return self.d.comp[f"Icon/{lig}" + (" (filled)" if filled else "")]

    def nested(self, root, name):
        return self.d.child(root, name)

    def triggers(self):
        return self.ctx.setdefault("triggers", {})


# ───────────────────────────────────────────────────────────────── components on Components / Playback
def build_parts(d: Doc, ctx: dict):
    K = Kit(d, ctx)
    S = Spec(d, ctx, DocPage.below(d, "Components / Playback"))
    ib_v = dict(Style="Standard", Size="Default", Width="Default", State="Enabled")

    # Cover pager --------------------------------------------------------------------------------------
    def pager(vp):
        playing, art = vp["Playing"] == "True", vp["Art"] == "Cover"
        c = comp(d, "Now playing/Cover pager", w=411, h=323)
        scale = 1.0 if playing else 0.86
        size = round(323 * scale, 1)
        off = round((323 - size) / 2, 1)
        cv = inst(d, "Cover", c, name="Cover", Kind="Album", Size="208", x=20 + off, y=off)
        d.set(cv, width=size, height=size)
        if playing:
            bind_dim(d, cv, "cornerRadius", "radius/extra-large")
        else:
            d.set(cv, cornerRadius=round(48 * 0.86, 1))
        if not art:
            d.set(d.find(cv, name="Picture", limit=1)[0], visible=False)
        sl = frame(d, c, "Next cover", w=40, h=size, x=351, y=off, clip=True)
        d.set(sl, cornerRadius=20)
        nx = inst(d, "Cover", sl, name="Next cover art", Kind="Album", Size="208", x=round(20 - size / 2, 1), y=0)
        d.set(nx, width=size, height=size, cornerRadius=0)
        return c, {}

    S.make("Now playing/Cover pager", "Cover pager",
           "The song's cover, 323dp on a 411dp screen (the width less 88), 20dp in from the left, and the next song in the "
           "queue as a 40dp sliver cut from the middle of its cover, 8dp to the right, 20dp corners. Swiping sideways "
           "changes song and the sliver widens into the cover. Paused, both step back to 86 % and the cover's corners "
           "round from 28 to 48 (41dp at that scale), on the play control's spring. Art None is a song with no cover.",
           "ExpressiveNowPlaying.kt", "CoverPager",
           [dict(Playing="True", Art="Cover"), dict(Playing="False", Art="Cover"), dict(Playing="True", Art="None")],
           pager, cover=True)

    # Title block -------------------------------------------------------------------------------------
    def title_block(vp):
        lyric = vp["Lyric"] == "True"
        c = comp(d, "Now playing/Title block", layout="v", w=411, pad=[26, 24, 0, 24], sizing=("fixed", "hug"))
        lay = {}
        if lyric:
            row = frame(d, c, "Inline lyric", layout="h", h=20, gap=8, cross="center", sizing=("fill", "fixed"))
            lay["lyr_box"] = frame(d, c, "Lyric spacing", w=1, h=10)
            d.icon(row, "lyrics", size=18, color="Color/primary", name="Lyric icon")
            lay["l"] = txt(d, row, "carry what we couldn't say", "Kit/Context", "Color/primary", "Lyric line", fill=True, lines=1)
        lay["t"] = txt(d, c, "Lights Over the Water", "Kit/Player title", "Color/on-surface", "Title", fill=True, lines=1)
        abox = frame(d, c, "Artist box", layout="v", pad=[2, 0, 0, 0], sizing=("fill", "hug"))
        lay["a"] = txt(d, abox, "Mira Okafor", "Body/Large", "Color/on-surface-variant", "Artist", fill=True, lines=1)
        return c, lay

    S.make("Now playing/Title block", "Now Playing title block",
           "The line being sung over the title (synced lyrics only: the lyrics glyph and the line in primary, 14/20 "
           "semibold, 10dp above the title), the title in the player's 32/40 rounded style and the artist, which opens "
           "the artist. A title too wide narrows to width 75, then 50, before it ellipsises (Kit/Player title w75, w50).",
           "ExpressiveNowPlaying.kt", "TitleBlock", [dict(Lyric="True"), dict(Lyric="False")], title_block, cover=True,
           props=[("Lyric line", "text", "l", None, None), ("Title", "text", "t", None, None),
                  ("Artist", "text", "a", None, None)])

    # Lyrics bars ------------------------------------------------------------------------------------
    def top_bar(vp):
        c = comp(d, "Lyrics/Top bar", layout="v", w=411, sizing=("fixed", "hug"), fill="Color/surface-container",
                 cornerRadius=[0, 0, 28, 28])
        spacer(d, c, "Status bar inset", w=411, h=40)
        row = frame(d, c, "Bar", layout="h", w=411, h=64, gap=4, pad=[0, 16, 0, 8], cross="center")
        b = inst(d, "Icon button", row, name="Close", **ib_v)
        set_glyph(d, ctx, b, "Icon button", "keyboard_arrow_down", color="Color/on-surface-variant")
        col = frame(d, row, "Text column", layout="v", sizing=("fill", "hug"))
        t = txt(d, col, "Lights Over the Water", "Kit/Compact title", "Color/on-surface", "Title", fill=True, lines=1)
        a = txt(d, col, "Mira Okafor", "Body/Small", "Color/on-surface-variant", "Artist", fill=True, lines=1)
        return c, {"t": t, "a": a}

    S.make("Lyrics/Top bar", "Lyrics top bar",
           "The lyrics screen's top bar: close, then the title over the artist, on surface container with 28dp bottom "
           "corners, over the status bar.", "ExpressiveLyrics.kt", "TopBar", [{}], top_bar, single=True, cover=True,
           props=[("Title", "text", "t", None, None), ("Artist", "text", "a", None, None)])

    W = 211

    def bottom_bar(vp):
        playing = vp["Playing"] == "True"
        c = comp(d, "Lyrics/Bottom bar", layout="v", w=411, sizing=("fixed", "hug"), fill="Color/surface-container",
                 cornerRadius=[28, 28, 0, 0])
        row = frame(d, c, "Bar", layout="h", w=411, h=88, gap=12, pad=[0, 20, 0, 24], cross="center")
        e = txt(d, row, "1:18", "Kit/Figures", "Color/on-surface-variant", "Elapsed")
        pr = inst(d, "Progress/Wavy", row, name="Progress", Running=str(playing))
        d.set(pr, sizingH="fill")
        set_progress(d, pr, PROGRESS)
        t = txt(d, row, "3:52", "Kit/Figures", "Color/on-surface-variant", "Total")
        pbox = frame(d, row, "Play box", layout="h", pad=[0, 0, 0, 8], sizing=("hug", "hug"))
        inst(d, "Play button", pbox, name="Play", Size="Large", Playing=str(playing), Ring="False", Tonal="False")
        spacer(d, c, "Gesture inset", w=411, h=24)
        return c, {"e": e, "t": t}

    S.make("Lyrics/Bottom bar", "Lyrics bottom bar",
           "Elapsed time, the progress line (wavy while playing, flat when paused; it shows where the song is and does "
           "not seek: tap a line to seek), the length and the 56dp play control, on surface container with 28dp top "
           "corners, above the gesture area.", "ExpressiveLyrics.kt", "BottomBar",
           [dict(Playing="True"), dict(Playing="False")], bottom_bar, cover=True,
           props=[("Elapsed", "text", "e", None, None), ("Total", "text", "t", None, None)])

    notes = {"Not found": "No lyrics were found for this song.", "Not downloaded": "Download once and they stay on this phone.",
             "Instrumental": "This song is instrumental.", "Downloading": "Download once and they stay on this phone."}

    def empty(vp):
        st = vp["State"]
        c = comp(d, "Lyrics/Empty", layout="v", w=411, pad=[0, 32, 0, 32], cross="center", sizing=("fixed", "hug"))
        art = frame(d, c, "Clover", w=128, h=128)
        sh = d.instance("Shape/Clover 4", art, name="Clover shape")
        d.set(sh, x=0, y=0, width=128, height=128)
        d.override(sh, "Outline", fill="Color/secondary-container")
        g = d.icon(art, "lyrics", size=48, color="Color/on-secondary-container", name="Lyrics icon")
        d.set(g, x=40, y=40)
        spacer(d, c, "Gap 1", w=24, h=24)
        n = txt(d, c, notes[st], "Body/Large", "Color/on-surface-variant", "Note", fill=True, align="center")
        d.set(n, textAutoResize="height")
        spacer(d, c, "Gap 2", w=24, h=24)
        if st == "Downloading":
            box = frame(d, c, "Loading", layout="h", w=48, h=48, cross="center", main="center")
            if "Loading indicator" in d.comp:
                inst(d, "Loading indicator", box, name="Loading indicator")
            else:
                sh2 = d.instance("Shape/Loader four leaf", box, name="Loading indicator")
                d.set(sh2, width=32, height=32)
        else:
            retry = st != "Not downloaded"
            b = inst(d, "Button", c, name="Download lyrics", Style="Outlined" if retry else "Filled", Size="Small",
                     Icon="Leading", State="Enabled", Squared="False")
            set_text(d, b, K.p("Button", "Label"), "Download lyrics")
            d.cmd("instance.setProperties", ids=[b], properties={K.p("Button", "Leading icon"): K.icon_id("refresh" if retry else "download")})
            lead = d.find(b, name="Leading icon", limit=1)[0]
            d.set(d.find(lead, name="Glyph", limit=1)[0], fill=V("Color/on-surface-variant" if retry else "Color/on-primary"))
        return c, {}

    S.make("Lyrics/Empty", "Lyrics, nothing to show",
           "The lyrics screen with no text: the 128dp clover on secondary container with the lyrics glyph, the card's "
           "sentence for this state, and the download action: filled the first time, outlined as a retry (after nothing "
           "was found, or for a song tagged instrumental), the loading indicator while it runs. 120dp under the top bar.",
           "ExpressiveLyrics.kt", "NoLyrics", [dict(State=s) for s in notes], empty, cover=True)


# ───────────────────────────────────────────────────────────────── screen helpers
def build(d: Doc, ctx: dict):
    build_parts(d, ctx)
    K = Kit(d, ctx)
    T = K.triggers()
    notes: dict[str, list] = {}

    def note(key, text, cat="Code differs from mock"):
        notes.setdefault(key, []).append((text, cat))

    def bars(fr, h=SCREEN_H):
        inst(d, "Status bar", fr, name="Status bar", x=0, y=0)
        inst(d, "Gesture bar", fr, name="Gesture bar", x=0, y=h - 24)

    def cover_fr(fr):
        d.set_mode(fr, "Color", "Cover")

    def finish(a):
        a.close()
        # Other modules may have made a category without going through d.annotate.
        for c in d.cmd("annotation.categories").get("categories", []):
            d.ann_categories.setdefault(c["name"], c["id"])
        for key in a.keys:
            s = d.screens[key]
            for fid in (s["light"], s["dark"]):
                d.annotate(fid, f"Compose: {s['source']}", "Source")
                for text, cat in notes.get(key, []):
                    d.annotate(fid, text, cat)
                d.ready_for_dev(fid)

    # Now Playing -------------------------------------------------------------------------------------
    def now_playing(fr, *, playing=True, radio=False, open_with=False, full=False):
        if open_with:
            d.set_mode(fr, "Color", "Wallpaper")
        else:
            cover_fr(fr)
        page = frame(d, fr, "Player", layout="v", w=411, h=838, x=0, y=40, main="spaceBetween")
        hd = inst(d, "Now playing header", page, name="Header")
        K.setp(hd, "Now playing header", Context="Radio · Lights Over the Water" if radio else "Paper Lanterns",
               Context_icon=K.icon_id("sensors" if radio else "album"))
        if radio:
            d.set(K.nested(hd, "Context icon"), name="Context icon")
        pbox = frame(d, page, "Cover pager box", layout="v", pad=[4, 0, 0, 0], sizing=("fill", "hug"))
        pg = inst(d, "Now playing/Cover pager", pbox, name="Cover pager", Playing=str(playing),
                  Art="None" if open_with else "Cover")
        set_cover(d, pg, 0)
        ctl = frame(d, page, "Controls", layout="v", sizing=("fill", "hug"))
        tb = inst(d, "Now playing/Title block", ctl, name="Title block", Lyric=str(not open_with))
        d.set(tb, sizingH="fill")
        if open_with:
            K.setp(tb, "Now playing/Title block", Title="band-practice", Artist="")
        else:
            K.setp(tb, "Now playing/Title block", Lyric_line="carry what we couldn't say", Title="Lights Over the Water",
                   Artist="Mira Okafor")
        for name, comp_name, pad, vp in (("Seek bar", "Seek bar", [10, 24, 0, 24], dict(Playing=str(playing))),
                                         ("Seek times", "Seek times", [0, 24, 0, 24], None),
                                         ("Transport", "Transport", [22, 24, 0, 24], dict(Playing=str(playing))),
                                         ("Tools", "Now playing tools", [20, 24, 24, 24],
                                          dict(Shuffle="On" if radio else "Off", Repeat="One" if radio else "Off"))):
            box = frame(d, ctl, f"{name} box", layout="v", pad=pad, sizing=("fill", "hug"))
            i = inst(d, comp_name, box, name=name, **(vp or {}))
            d.set(i, sizingH="fill")
            if name == "Seek bar" and open_with:
                set_progress(d, i, 41 / 187)
            if name == "Seek times":
                K.setp(i, "Seek times", Elapsed="0:41" if open_with else "1:18", Total="3:07" if open_with else "3:52")
            if name == "Tools":
                tools = i
        if open_with:
            K.variant(K.nested(tools, "Heart"), Favourite="False")
        below = frame(d, fr, "Below the player", layout="v", w=411, gap=12, pad=[0, 16, 52, 16], x=0, y=878,
                      sizing=("fixed", "hug"))
        lc = inst(d, "Lyrics/Card", below, name="Lyrics card", State="Not downloaded" if open_with else "Synced")
        d.set(lc, sizingH="fill")
        if not open_with:
            l4 = K.nested(lc, "Lyric 4")
            K.setp(l4, "Lyrics/Line", Text="♪")
            ac = d.instance("Info card/Artist", below, name="About the artist")
            d.set(ac, sizingH="fill")
            K.setp(ac, "Info card/Artist", Name="Mira Okafor")
            set_cover(d, ac, 0)
            cr = d.instance("Info card/Credits", below, name="Credits")
            d.set(cr, sizingH="fill")
            K.setp(cr, "Info card/Credits", Show_composer=full, Show_genre=full)
            if full:
                K.setp(K.nested(cr, "Composer"), "Key value", Value="Mira Okafor, Idris Bello")
                K.setp(K.nested(cr, "Genre"), "Key value", Value="Indie folk")
        if open_with:
            wallpaper(d, fr)
        bars(fr, d.get(fr)["height"])
        return {"header": hd, "tools": tools, "lyrics": lc, "page": page}

    a = d.area("Now Playing", note="Now Playing on the cover's scheme (Color mode Cover on each frame), the full page "
                                   "with the cards under the player, paused on radio, and a file opened from another "
                                   "app, which has no cover, lyrics or artist and keeps the wallpaper's scheme.")
    fr = a.screen("now_playing", name="Now Playing / Playing", source=NP)
    np = now_playing(fr)
    T[("now_playing", "queue")] = K.nested(np["tools"], "Queue")
    T[("now_playing", "sheet_track_menu_now_playing")] = K.nested(np["header"], "More")
    T[("now_playing", "back")] = K.nested(np["header"], "Close")
    T[("now_playing", "lyrics_synced")] = np["lyrics"]

    full_h = 1684
    fr = a.screen("now_playing_full", name="Now Playing / Playing (full length)", source=NP, h=full_h)
    npf = now_playing(fr, full=True)
    T[("now_playing_full", "lyrics_synced")] = K.nested(npf["lyrics"], "Open lyrics button")
    T[("now_playing_full", "queue")] = K.nested(npf["tools"], "Queue")
    note("now_playing_full", "Composer and genre show because the demo song is tagged with them for this render "
                             "(\"Mira Okafor, Idris Bello\", \"Indie folk\"). Untagged songs leave both rows out.", "Content")
    note("now_playing_full", "The compact header that slides down once the title has scrolled away is not drawn here; "
                             "see Compact header on Components / Playback.", "Content")

    fr = a.screen("now_playing_paused_radio", name="Now Playing / Paused, radio", source=NP)
    npr = now_playing(fr, playing=False, radio=True)
    T[("now_playing_paused_radio", "queue_radio")] = K.nested(npr["tools"], "Queue")

    fr = a.screen("open_with", name="Now Playing / Opened from another app", source=NP)
    npo = now_playing(fr, open_with=True)
    T[("open_with", "sheet_track_menu_open_with")] = K.nested(npo["header"], "More")
    note("open_with", "A file opened from another app has no cover, so FromArt has no seed and the page keeps the "
                      "wallpaper's scheme (Color mode Wallpaper here). The seek bar is at 0:41 of 3:07.", "Content")
    finish(a)

    # Queue and lyrics --------------------------------------------------------------------------------
    sp = K.C["List row/Song"]["props"]

    def song_cell(parent, pos, title, artist, cov, *, state="Default", trailing="drag"):
        c = inst(d, "List group/Cell", parent, name=f"{title} cell", Position=pos, Surface="Default")
        r = K.nested(c, "Row")
        d.set(r, name=title)
        K.variant(r, Lead="Cover", State=state)
        d.cmd("instance.setProperties", ids=[r], properties={sp["Title"]: title, sp["Subtitle"]: artist})
        if trailing == "drag":
            d.cmd("instance.setProperties", ids=[r], properties={sp["Trailing"]: d.comp["List row/Drag handle"]})
        set_cover(d, r, cov)
        return c, r

    def queue(fr, *, radio=False, empty=False):
        cover_fr(fr)
        bar = d.instance("Queue bar", fr, name="Queue bar", x=0, y=40)
        K.setp(bar, "Queue bar", Context="Radio · Lights Over the Water" if radio else "Paper Lanterns")
        if radio:
            ic = K.nested(bar, "Context icon")
            d.cmd("instance.swap", ids=[ic], component=K.icon_id("sensors"))
            d.set(d.find(ic, name="Glyph", limit=1)[0], fill=V("Color/primary"))
        lst = frame(d, fr, "List", layout="v", w=411, x=0, y=104, sizing=("fixed", "hug"))
        lab = d.instance("List label", lst, name="Now playing label")
        K.setp(lab, "List label", Label="Now playing")
        box = frame(d, lst, "Now playing group", layout="v", pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        _, now = song_cell(box, "Single", "Lights Over the Water", "Mira Okafor", 0, state="Current playing", trailing="more")
        rows = RADIO if radio else QUEUE
        if empty:
            # EmptyState(clover = true, symbol = QueueMusic, body = null) in an item padded 48dp on top.
            frame(d, lst, "Empty item top", w=411, h=48)
            es = inst(d, "Empty state", lst, name="Nothing up next", Art="Clover")
            d.set(es, sizingH="fill")
            ep = K.C["Empty state"]["props"]
            d.cmd("instance.setProperties", ids=[es], properties={ep["Title"]: "Nothing up next", ep["Show body"]: False,
                                                                  ep["Icon"]: K.icon_id("queue_music")})
            bars(fr)
            return bar, now
        head = frame(d, lst, "Up next heading", layout="h", pad=[0, 20, 0, 0], cross="max", sizing=("fill", "hug"))
        ul = d.instance("List label", head, name="Up next label")
        d.set(ul, sizingH="fill")
        K.setp(ul, "List label", Label=f"Up next, {len(rows)} songs")
        cl = inst(d, "Button", head, name="Clear", Style="Text", Size="Small", Icon="None", State="Enabled", Squared="False")
        set_text(d, cl, K.p("Button", "Label"), "Clear")
        grp = frame(d, lst, "Up next", layout="v", gap=2, pad=[0, 16, 0, 16], sizing=("fill", "hug"))
        for i, (t, ar, cv) in enumerate(rows):
            song_cell(grp, "Top" if i == 0 else ("Bottom" if i == len(rows) - 1 else "Middle"), t, ar, cv)
        bars(fr)
        return bar, now

    def lyrics(fr, kind):
        cover_fr(fr)
        if kind == "synced":
            col = frame(d, fr, "Lines", layout="v", w=363, x=24, y=0, sizing=("fixed", "hug"))
            cur_id = None
            for i, line in enumerate(LYRICS):
                st = "Current" if i == CURRENT else ("Sung" if i < CURRENT else "Upcoming")
                li = inst(d, "Lyrics/Screen line", col, name=f"Lyric {i + 1}", State=st)
                d.set(li, sizingH="fill")
                K.setp(li, "Lyrics/Screen line", Text=BREAKS.get(line, line))
                if i == CURRENT:
                    cur_id = li
            # The line being sung sits a third of the way down the screen (SyncedLyrics' anchor).
            d.set(col, y=round(SCREEN_H / 3 - d.get(cur_id)["y"], 1))
        elif kind == "plain":
            col = frame(d, fr, "Lyrics text", layout="v", w=363, x=24, y=126, sizing=("fixed", "hug"))
            paras, cur = [], []
            for line in LYRICS[1:]:
                if line == "♪":
                    paras.append(cur); cur = []
                else:
                    cur.append(PLAIN_BREAKS.get(line, line))
            paras.append(cur)
            for i, p in enumerate(paras):
                box = frame(d, col, f"Paragraph {i + 1}", layout="v", pad=[0, 0, 28, 0], sizing=("fill", "hug"))
                t = txt(d, box, "\n".join(p), "Kit/Lyric line", "Color/on-surface", "Paragraph text", fill=True)
                d.set(t, textAutoResize="height")
        else:
            e = inst(d, "Lyrics/Empty", fr, name="No lyrics", State=kind, x=0, y=224)
        top = d.instance("Lyrics/Top bar", fr, name="Top bar", x=0, y=0)
        bot = inst(d, "Lyrics/Bottom bar", fr, name="Bottom bar", Playing="True", x=0, y=802)
        K.setp(top, "Lyrics/Top bar", Title="Lights Over the Water", Artist="Mira Okafor")
        bars(fr)
        return top

    a = d.area("Queue and lyrics", note="The queue and the full-screen lyrics, on the cover's scheme (Color mode Cover).")
    QS = SRC + "ExpressiveQueue.kt · ExpressiveQueue"
    fr = a.screen("queue", name="Queue / Up next", source=QS)
    bar, now = queue(fr)
    T[("queue", "back")] = K.nested(bar, "Close")
    fr = a.screen("queue_radio", name="Queue / Radio", source=QS)
    bar, _ = queue(fr, radio=True)
    T[("queue_radio", "back")] = K.nested(bar, "Close")
    fr = a.screen("queue_empty", name="Queue / Nothing up next", source=QS)
    bar, _ = queue(fr, empty=True)
    T[("queue_empty", "back")] = K.nested(bar, "Close")
    LS = SRC + "ExpressiveLyrics.kt · ExpressiveLyrics"
    for key, kind, name in (("lyrics_synced", "synced", "Lyrics / Synced"), ("lyrics_plain", "plain", "Lyrics / Plain"),
                            ("lyrics_none", "Not found", "Lyrics / None found"),
                            ("lyrics_not_downloaded", "Not downloaded", "Lyrics / Not downloaded"),
                            ("lyrics_instrumental", "Instrumental", "Lyrics / Instrumental"),
                            ("lyrics_downloading", "Downloading", "Lyrics / Downloading")):
        fr = a.screen(key, name=name, source=LS)
        top = lyrics(fr, kind)
        T[(key, "back")] = K.nested(top, "Close")
    note("lyrics_synced", "Lines break where the render breaks them: the code sets LineBreak.Heading, which balances a "
                          "wrapped line; the text layers carry those breaks.", "Content")
    note("lyrics_plain", "Lines break where the render breaks them (LineBreak.Heading balances a wrapped line).", "Content")
    finish(a)

    # Backgrounds for overlays ------------------------------------------------------------------------
    def mini_and_nav(fr, tab):
        m = inst(d, "Mini player", fr, name="Mini player", State="Playing", Title_length="Default", x=12, y=754)
        K.setp(m, "Mini player", Title="Lights Over the Water", Artist="Mira Okafor")
        set_cover(d, m, 0)
        inst(d, "Navigation bar", fr, name="Navigation bar", Selected=tab, x=0, y=826)
        bars(fr)

    def home_bg(fr):
        rb = inst(d, "Top app bar/Root", fr, name="Top app bar", Kind="Home", Scrolled="False", x=0, y=0)
        K.setp(rb, "Top app bar/Root", Title="Attic")
        inst(d, "Connected button group", fr, name="Filter", Options="4", Selected="1", On_cell="False", x=0, y=112)
        fav = inst(d, "Favourites pill", fr, name="Favourites", Height="248", x=16, y=172)
        K.setp(fav, "Favourites pill", Count="42", Unit="songs")
        car = frame(d, fr, "Carousel", w=331, h=200, x=88, y=172, clip=True)
        for nm, idx, x, w in (("Paper Lanterns", 0, 0, 175), ("Iravu Rail", 8, 183, 92), ("Chhat Pe Dhoop", 9, 283, 60)):
            cv = inst(d, "Cover", car, name=nm, Kind="Album", Size="208", x=x, y=0)
            d.set(cv, width=w, height=200)
            bind_dim(d, cv, "cornerRadius", "radius/extra-large")
            set_cover(d, cv, idx)
        cap = d.instance("Hero caption", fr, name="Hero caption", x=88, y=372)
        K.setp(cap, "Hero caption", Title="Paper Lanterns", Subtitle="Mira Okafor, 2025")
        rc = d.instance("Recap entry", fr, name="Recap entry", x=0, y=421)
        K.setp(rc, "Recap entry", Title="Your 2026 so far", Body="18,420 minutes. See your top songs and cards to share.")
        sh = inst(d, "Section header", fr, name="Instant mixes", Overline="False", Trailing="False", x=0, y=550)
        K.setp(sh, "Section header", Title="Instant mixes")
        rows = [[("Porch Light", 0), ("Platform Ettu", 8)], [("Patang", 9), ("Glasshouse", 3)],
                [("Theneer Kadai", 8), ("Room Tone", 4)]]
        for r, row in enumerate(rows):
            rw = frame(d, fr, f"Mix row {r + 1}", layout="h", gap=8, x=16, y=618 + r * 64, sizing=("hug", "hug"))
            for t, idx in row:
                mp = inst(d, "Instant mix pill", rw, name=t, Now="False")
                K.setp(mp, "Instant mix pill", Title=t)
                set_cover(d, mp, idx)
        mini_and_nav(fr, "Home")

    def scheme(root, mode):
        """A cover's Color mode on `root` and on every instance under it (cover-mode sets carry their own)."""
        try:
            d.set_mode([root] + d.find(root, type="INSTANCE", limit=2000), "Color", mode)
        except Exception:
            pass

    def color_mode(fr, mode):
        try:
            d.set_mode(fr, "Color", mode)
            return True
        except Exception:
            return False

    def playlist_bg(fr):
        """The Long drive playlist as screens_browse draws it: the Collection header on its first song's cover
        (Afterglow Protocol), the tools, the songs and the collection bar."""
        col = frame(d, fr, "List", layout="v", w=411, x=0, y=0, sizing=("fixed", "hug"))
        h = inst(d, "Collection header", col, name="Collection header", Kind="Playlist")
        K.setp(h, "Collection header", Title="Long drive", Meta="12 songs, 44 min")
        set_mosaic(d, d.child(h, "Cover"), 10, 8, 3, 6)
        scheme(h, "Afterglow Protocol")
        btns = frame(d, col, "Tools", layout="h", w=411, gap=8, pad=[12, 16, 12, 16], sizing=("fixed", "hug"))
        b1 = inst(d, "Button", btns, name="Add songs", Style="Tonal", Size="Small", Icon="Leading", State="Enabled", Squared="False")
        set_text(d, b1, K.p("Button", "Label"), "Add songs")
        d.cmd("instance.setProperties", ids=[b1], properties={K.p("Button", "Leading icon"): K.icon_id("add")})
        b2 = inst(d, "Button", btns, name="Reorder songs", Style="Outlined", Size="Small", Icon="Leading", State="Enabled", Squared="False")
        set_text(d, b2, K.p("Button", "Label"), "Reorder songs")
        d.cmd("instance.setProperties", ids=[b2], properties={K.p("Button", "Leading icon"): K.icon_id("swap_vert")})
        for b, colour in ((b1, "Color/on-secondary-container"), (b2, "Color/on-surface-variant")):
            lead = d.find(b, name="Leading icon", limit=1)[0]
            d.set(d.find(lead, name="Glyph", limit=1)[0], fill=V(colour))
        grp = frame(d, col, "Songs", layout="v", gap=2, w=411, pad=[0, 16, 0, 16], sizing=("fixed", "hug"))
        songs = [("Night Drive Firmware", "Velvet Circuit", 10), ("Iravu Rail", "Kavya Raman", 8),
                 ("Neon Peaches", "Juniper Vale", 3), ("Bheegi Sadak", "Ruhaan Sethi", 6), ("Kettle On", "Small Hours Tape Club", 1)]
        for i, (t, ar, cv) in enumerate(songs):
            song_cell(grp, "Top" if i == 0 else "Middle", t, ar, cv, trailing="more")
        bar = inst(d, "Top app bar/Collection", fr, name="Collection bar", Collapsed="False", x=0, y=0)
        K.setp(bar, "Top app bar/Collection", Title="Long drive")
        scheme(bar, "Afterglow Protocol")
        mini_and_nav(fr, "Library")
        return bar

    def library_bg(fr):
        rb = inst(d, "Top app bar/Root", fr, name="Top app bar", Kind="Plain", Scrolled="False", x=0, y=0)
        K.setp(rb, "Top app bar/Root", Title="Library")
        inst(d, "Chip row", fr, name="Filters", Selected="None", x=0, y=120)
        grp = frame(d, fr, "Library list", layout="v", gap=2, w=411, pad=[0, 16, 0, 16], x=0, y=168, sizing=("fixed", "hug"))
        cp = K.C["List row/Collection"]["props"]
        items = [("Favourites", "Favourites", "Playlist, 42 songs", None), ("Playlist", "Long drive", "Playlist, 25 songs", (10, 8, 3, 6)),
                 ("Playlist", "Sunday cleaning", "Playlist, 32 songs", (3, 9, 2, 7)), ("Album", "Focus", "Playlist, 39 songs", (1,)),
                 ("Playlist", "English", "Playlist, 24 songs", (0, 3, 11, 4)), ("Album", "Tamil", "Playlist, 16 songs", (8,)),
                 ("Album", "Hindi", "Playlist, 13 songs", (9,)), ("Album", "Español", "Playlist, 5 songs", (2,)),
                 ("Album", "Late Night", "Playlist, 11 songs", (10,))]
        for i, (typ, t, s, covers) in enumerate(items):
            c = inst(d, "List group/Cell", grp, name=f"{t} cell", Position="Top" if i == 0 else "Middle", Surface="Default")
            r = K.nested(c, "Row")
            # A folder with songs from one album shows that cover alone (Mosaic=False).
            kind, mos = ("Folder", "False") if typ == "Album" else (typ, str(typ == "Playlist"))
            d.cmd("instance.swap", ids=[r], component=K.C["List row/Collection"]["variants"][f"Kind={kind}, Mosaic={mos}"])
            d.set(r, name=t, sizingH="fill")
            d.cmd("instance.setProperties", ids=[r], properties={cp["Title"]: t, cp["Subtitle"]: s})
            if covers and len(covers) == 4:
                set_mosaic(d, r, *covers)
            elif covers:
                set_cover(d, r, covers[0])
        fab = inst(d, "Extended FAB", fr, name="New playlist", Expanded="True", x=243, y=666)
        K.setp(fab, "Extended FAB", Label="New playlist")
        mini_and_nav(fr, "Library")

    # Overlay screens ---------------------------------------------------------------------------------
    def scrim(fr, kind="Sheet", h=SCREEN_H):
        s = inst(d, "Scrim", fr, name="Scrim", Kind=kind, x=0, y=0)
        d.set(s, height=h)
        return s

    def place_bottom(fr, inst_id):
        paragraph_line_heights(d, roots=[inst_id])
        h = d.get(inst_id)["height"]
        d.set(inst_id, x=0, y=round(SCREEN_H - h, 1))

    def track_menu(fr, variant, *, title=None, artist=None, cover=None, album=None):
        m = inst(d, "Sheet/Track menu", fr, name="Track menu", **variant)
        if title:
            set_nested_text(d, m, "Header/Title", title)
            set_nested_text(d, m, "Header/Subtitle", artist or "")
        if album:
            set_nested_text(d, m, "Go to album/Supporting", album)
            set_nested_text(d, m, "Go to artist/Supporting", artist)
        if cover is not None:
            set_cover(d, m, cover)
        place_bottom(fr, m)
        return m

    a = d.area("Sheets, dialogs and messages",
               note="Every overlay over the screen it opens from: the screen rebuilt from instances, the scrim, then the "
                    "overlay. Sheets over Now Playing take the cover's scheme. The track menu's Share row hands the file "
                    "to Android's share sheet (ui/SongShare.kt); Attic draws nothing after that, and the Outside Attic "
                    "area holds a stand-in frame for it. Messages appear above the mini player, or 24dp above the "
                    "gesture bar over Now Playing.")

    fr = a.screen("sheet_track_menu", name="Track menu / From Home", source=SHEETS + "ExpressiveTrackMenu")
    home_bg(fr)
    sc = scrim(fr)
    m = track_menu(fr, {"Opened from": "Library", "Sleep timer": "None"}, title="Porch Light", artist="Mira Okafor")
    T[("sheet_track_menu", "sheet_playlist_picker")] = K.nested(m, "Add to playlist")
    T[("sheet_track_menu", "sheet_language_picker")] = K.nested(m, "Language")
    T[("sheet_track_menu", "back")] = sc
    note("sheet_track_menu", "Share closes the menu and hands the song's file to Android's share sheet "
                             "(ui/SongShare.kt). Attic draws nothing after that; the prototype goes to the stand-in "
                             "frame Share / System share sheet in the Outside Attic area.", "Content")
    note("sheet_track_menu", "Any song's More button or a long press opens this menu over the screen the song is on. "
                             "The prototype opens this frame, over Home, from every list.", "Content")

    fr = a.screen("sheet_track_menu_playlist", name="Track menu / From a playlist", source=SHEETS + "ExpressiveTrackMenu")
    playlist_bg(fr)
    sc = scrim(fr)
    m = track_menu(fr, {"Opened from": "Playlist", "Sleep timer": "None"}, title="Iravu Rail", artist="Kavya Raman", cover=8,
                   album="Iravu Rail")
    T[("sheet_track_menu_playlist", "sheet_playlist_picker")] = K.nested(m, "Add to playlist")
    T[("sheet_track_menu_playlist", "sheet_language_picker")] = K.nested(m, "Language")
    T[("sheet_track_menu_playlist", "back")] = sc
    note("sheet_track_menu_playlist", "The playlist header behind takes the scheme of its first song's cover: Color mode "
                                      "Afterglow Protocol.", "Content")

    def np_under(fr, **kw):
        r = now_playing(fr, **kw)
        return r

    fr = a.screen("sheet_track_menu_now_playing", name="Track menu / From Now Playing", source=SHEETS + "ExpressiveTrackMenu")
    np_under(fr)
    sc = scrim(fr)
    m = track_menu(fr, {"Opened from": "Now playing", "Sleep timer": "Off"})
    T[("sheet_track_menu_now_playing", "sheet_sleep_timer")] = K.nested(m, "Sleep timer")
    T[("sheet_track_menu_now_playing", "back")] = sc

    fr = a.screen("sheet_track_menu_open_with", name="Track menu / Opened from another app", source=SHEETS + "ExpressiveTrackMenu")
    np_under(fr, open_with=True)
    sc = scrim(fr)
    track_menu(fr, {"Opened from": "Open with", "Sleep timer": "Off"})
    T[("sheet_track_menu_open_with", "back")] = sc

    fr = a.screen("sheet_sleep_timer", name="Sleep timer / Choices", source=SHEETS + "ExpressiveSleepTimerPicker")
    np_under(fr)
    sc = scrim(fr)
    st = d.instance("Sheet/Sleep timer", fr, name="Sleep timer")
    place_bottom(fr, st)
    T[("sheet_sleep_timer", "snackbar_sleep_timer")] = K.nested(st, "Choice 30 minutes")
    T[("sheet_sleep_timer", "snackbar_sleep_timer_off")] = K.nested(st, "Choice Off")
    T[("sheet_sleep_timer", "back")] = sc

    fr = a.screen("sleep_timer_active", name="Sleep timer / Running, in the track menu", source=SHEETS + "ExpressiveTrackMenu")
    np_under(fr)
    sc = scrim(fr)
    m = track_menu(fr, {"Opened from": "Now playing", "Sleep timer": "Running"})
    T[("sleep_timer_active", "sheet_sleep_timer")] = K.nested(m, "Sleep timer")
    T[("sleep_timer_active", "back")] = sc
    note("sleep_timer_active", "A running timer shows only here, under Sleep timer in the Now Playing menu (\"23 min "
                               "left\"); Attic shows it nowhere else. Choosing a time closes the menu and shows a message "
                               "(Message / Sleep timer set); opening More again shows this.", "Content")

    fr = a.screen("sheet_playlist_picker", name="Add to playlist / Picker", source=SHEETS + "ExpressivePlaylistPicker")
    home_bg(fr)
    sc = scrim(fr)
    pp = d.instance("Sheet/Playlist picker", fr, name="Playlist picker")
    place_bottom(fr, pp)
    T[("sheet_playlist_picker", "dialog_new_playlist")] = K.nested(pp, "New playlist")
    T[("sheet_playlist_picker", "back")] = sc

    fr = a.screen("sheet_language_picker", name="Song language / Picker", source=SHEETS + "ExpressiveLanguagePicker")
    home_bg(fr)
    sc = scrim(fr)
    lp = d.instance("Sheet/Language picker", fr, name="Language picker")
    place_bottom(fr, lp)
    T[("sheet_language_picker", "back")] = sc

    fr = a.screen("sheet_add_songs", name="Add songs / Sheet", source=SHEETS + "ExpressiveAddSongs")
    playlist_bg(fr)
    sc = scrim(fr)
    ad = d.instance("Sheet/Add songs", fr, name="Add songs")
    place_bottom(fr, ad)
    T[("sheet_add_songs", "back")] = sc

    def dialog(fr, variant, *, title, body=None, value=None, confirm, width=411):
        dl = inst(d, "Dialog", fr, name="Dialog", **variant)
        d.set(dl, width=width)
        K.setp(dl, "Dialog", Title=title)
        if body:
            K.setp(dl, "Dialog", Body=body)
        if value is not None:
            K.setp(K.nested(dl, "Text field"), "Text field", Label="Playlist name", Value=value)
        elif variant.get("Content") == "Text field":
            K.setp(K.nested(dl, "Text field"), "Text field", Label="Playlist name")
        set_text(d, K.nested(dl, "Confirm"), K.p("Button", "Label"), confirm)
        try:
            set_text(d, K.nested(dl, "Dismiss"), K.p("Button", "Label"), "Cancel")
        except Exception:
            pass
        paragraph_line_heights(d, roots=[dl])
        h = d.get(dl)["height"]
        d.set(dl, x=round((SCREEN_W - width) / 2, 1), y=dialog_y(h, SCREEN_H))
        return dl

    fr = a.screen("dialog_new_playlist", name="Playlist name / New, over the picker", source=SHEETS + "ExpressivePlaylistNameDialog")
    home_bg(fr)
    scrim(fr)
    pp = d.instance("Sheet/Playlist picker", fr, name="Playlist picker")
    place_bottom(fr, pp)
    scrim(fr, "Dialog")
    dl = dialog(fr, dict(Content="Text field", Buttons="Two", Confirm="Disabled"), title="New playlist", confirm="Create")
    T[("dialog_new_playlist", "back")] = K.nested(dl, "Dismiss")
    note("dialog_new_playlist", "Create stays disabled until there is a name. The field takes focus on open; the render "
                                "shows it unfocused (no cursor, 1dp outline).", "Content")

    fr = a.screen("dialog_name_playlist", name="Playlist name / Name, from Library", source=SHEETS + "ExpressivePlaylistNameDialog")
    library_bg(fr)
    scrim(fr, "Dialog")
    dl = dialog(fr, dict(Content="Text field", Buttons="Two", Confirm="Default"), title="Name this playlist",
                value="Road trip", confirm="Create")
    T[("dialog_name_playlist", "back")] = K.nested(dl, "Dismiss")

    fr = a.screen("dialog_rename_playlist", name="Playlist name / Rename", source=SHEETS + "ExpressivePlaylistNameDialog")
    playlist_bg(fr)
    scrim(fr, "Dialog")
    dl = dialog(fr, dict(Content="Text field", Buttons="Two", Confirm="Default"), title="Rename playlist",
                value="Long drive", confirm="Rename")
    T[("dialog_rename_playlist", "back")] = K.nested(dl, "Dismiss")

    fr = a.screen("dialog_delete_playlist", name="Delete playlist / Confirm", source=SHEETS + "ExpressiveDeletePlaylistDialog")
    playlist_bg(fr)
    scrim(fr, "Dialog")
    dl = dialog(fr, dict(Content="Message", Buttons="Two", Confirm="Destructive"), title="Delete Long drive?",
                body="The playlist goes away. The songs stay in your library.", confirm="Delete playlist", width=401)
    T[("dialog_delete_playlist", "back")] = K.nested(dl, "Dismiss")
    note("dialog_delete_playlist", "The render's dialog is 390dp wide and its body fits one line; text measures a little "
                                   "wider here, so this instance is 401 to keep the line whole. The dialog hugs its "
                                   "content between 280dp and the window.", "Content")

    fr = a.screen("menu_playlist_overflow", name="Playlist menu / Rename or delete",
                  source=SRC + "ExpressiveCollections.kt · ExpressivePlaylist")
    bar = playlist_bg(fr)
    mn = d.instance("Menu", fr, name="Menu", x=214, y=96)
    color_mode(mn, "Afterglow Protocol")
    T[("menu_playlist_overflow", "dialog_rename_playlist")] = K.nested(mn, "Rename")
    T[("menu_playlist_overflow", "dialog_delete_playlist")] = K.nested(mn, "Delete playlist")
    note("menu_playlist_overflow", "The menu is the header's surface container, so with no shadow (Robolectric draws "
                                   "none) it shows no edge in the render; on a phone the 3dp shadow outlines it.", "Content")

    fr = a.screen("snackbar", name="Message / Over Home", source=SRC + "Surfaces.kt · ExpressiveSnackbar")
    home_bg(fr)
    sn = d.instance("Snackbar", fr, name="Snackbar", x=28, y=678)
    K.setp(sn, "Snackbar", Message="Added to queue")

    fr = a.screen("snackbar_over_player", name="Message / Over Now Playing", source=SRC + "Surfaces.kt · ExpressiveSnackbar")
    np_under(fr)
    sn = d.instance("Snackbar", fr, name="Snackbar", x=28, y=806)
    K.setp(sn, "Snackbar", Message="Playing next")
    d.set_mode(sn, "Color", "Cover")
    # The messages the code raises that the flows reach (strings.xml message_*).
    def message_np(key, name, text, more_to=None, src_note=None):
        fr = a.screen(key, name=name, source=SRC + "Surfaces.kt · ExpressiveSnackbar")
        r = np_under(fr)
        sn = d.instance("Snackbar", fr, name="Snackbar", x=28, y=806)
        K.setp(sn, "Snackbar", Message=text)
        d.set_mode(sn, "Color", "Cover")
        if more_to:
            T[(key, more_to)] = K.nested(r["header"], "More")
        if src_note:
            note(key, src_note, "Content")
        return fr

    message_np("snackbar_sleep_timer", "Message / Sleep timer set, over Now Playing", "Music will pause in 30 minutes",
               more_to="sleep_timer_active",
               src_note="Choosing a time sets the timer, closes the picker and shows message_sleep_timer_set "
                        "(SheetModels.kt chooseSleepTimer). End of song reads \"Music will pause after this song\".")
    message_np("snackbar_sleep_timer_off", "Message / Sleep timer off, over Now Playing", "Sleep timer off",
               src_note="Choosing Off clears the timer and shows message_sleep_timer_off (SheetModels.kt chooseSleepTimer).")

    def message_home(key, name, text, src_note):
        fr = a.screen(key, name=name, source=SRC + "Surfaces.kt · ExpressiveSnackbar")
        home_bg(fr)
        sn = d.instance("Snackbar", fr, name="Snackbar", x=28, y=678)
        K.setp(sn, "Snackbar", Message=text)
        note(key, src_note, "Content")

    message_home("snackbar_share_failed", "Message / Couldn't share, over Home", "Couldn't share this song",
                 "When no app can take the file, Share closes the menu and shows message_share_failed (SheetModels.kt share, "
                 "ui/SongShare.kt) over the screen the menu was opened from.")
    message_home("snackbar_open_failed", "Message / Couldn't open a file, over Home", "Couldn't open this file",
                 "A file another app hands to Attic that can't be read or played shows message_open_failed "
                 "(playback/OpenWith.kt) over whatever screen is showing.")

    note("snackbar_over_player", "Over the player, queue or lyrics the message takes the inverse roles of the cover's "
                                 "scheme (FromCover) and sits 24dp above the gesture bar.", "Content")
    for key in ("sheet_track_menu", "sheet_track_menu_playlist", "sheet_track_menu_now_playing",
                "sheet_track_menu_open_with", "sheet_sleep_timer", "sleep_timer_active", "sheet_playlist_picker",
                "sheet_language_picker", "sheet_add_songs", "dialog_new_playlist"):
        note(key, SHEET_INSET_NOTE, RENDER_DIFFERS)
    for key in ("dialog_new_playlist", "dialog_name_playlist", "dialog_rename_playlist", "dialog_delete_playlist"):
        note(key, DIALOG_INSET_NOTE, RENDER_DIFFERS)
    finish(a)
