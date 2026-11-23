"""Page System surfaces: widget sizes on a home screen, notification shade, lock screen, splash, launcher icon with its
monochrome layer.

Owner: settings screens agent. Built from Components / System (components_states.py): Widget, Media notification,
Splash, Launcher icon. What Android draws (wallpaper, shade, quick settings, lock screen clock, other apps) is neutral
system chrome: raw colours allowed, every such layer named "(system)".

Areas: "Widget sizes" (one frame per launcher size, as the reference renders crop it, so --compare pairs them),
"Home screen", "Notification shade and lock screen", "Splash and launcher icon".
"""
from __future__ import annotations

from figbuild.kit import Doc, V

from .components_states import setp, inst
from .components_core import drawable_icon
from .foundations import page_header, _modes

PAGE = "System surfaces"
REFS = {k: f"comfort-{{theme}}-{k}" for k in ("widget_small", "widget_wide", "widget_large", "widget_large_empty")}

WALL = {"light": "#C9CED6", "dark": "#2E3138"}   # the render harness's launcher ground
WIDGET_SRC = "widget/PlayerWidget.kt · PlayerWidget"


def _sys_text(d, parent, chars, style, color, name, **kw):
    return d.text(parent, chars, style=style, color=color, allow_hex=True, name=name, **kw)


def build(d: Doc, ctx: dict):
    page_header(d, PAGE, "System surfaces",
                "What Attic puts on Android's own surfaces: the home-screen widget in its three sizes, the media "
                "notification in the shade and on the lock screen, the splash and the launcher icon. Android draws the "
                "rest (wallpaper, shade, clock, other apps' icons) as neutral stand-ins; those layers end in \"(system)\".")
    _widget_sizes(d)
    _home_screen(d)
    _shade_and_lock(d)
    _splash_and_icon(d)
    _section_edges(d)


def _section_edges(d: Doc):
    """Sections come with FigCraft's default grey edge; bind it to Canvas/divider like the rest of the canvas chrome."""
    for page in ("Screens", PAGE):
        pid = d.pages.get(page)
        if pid is None:
            continue
        secs = [c["id"] for c in d.get(pid, depth=1).get("children", []) if c.get("type") == "SECTION"]
        if secs:
            d.set(secs, stroke=V("Canvas/divider"))


def _finish(d: Doc, keys, notes=()):
    for k in keys:
        s = d.screens[k]
        for fid in (s["light"], s["dark"]):
            if not fid:
                continue
            d.annotate(fid, f"Compose: {s['source']}", "Source")
            for cat, txt in notes:
                d.annotate(fid, txt, cat)
            d.ready_for_dev(fid)


def _dark_labels(d: Doc, keys):
    """The launcher's labels read light on the dark ground of the Dark copies."""
    for k in keys:
        s = d.screens[k]
        for nm in ("Label (system)", "Caption (system)"):
            for lid in d.find(s["light"], name=nm, limit=50):
                if d.get(lid).get("type") == "TEXT" and lid in s["map"]:
                    d.set(s["map"][lid], fill="#E3E3E8")


def _dark_wall(d: Doc, keys, layer=None):
    """System chrome doesn't follow Attic's variables: give the Dark copies the dark launcher ground."""
    for k in keys:
        s = d.screens[k]
        if layer is None:
            d.set(s["dark"], fill=WALL["dark"])
        else:
            d.set(s["map"][layer[k]], fill=WALL["dark"])


def _widget_sizes(d: Doc):
    a = d.area("Widget sizes", page=PAGE, note="The widget at each launcher size, on the launcher's plain ground (16dp around "
                                               "it), as PlayerWidget.kt builds it. Playing: the card washed toward the cover; "
                                               "Empty: before anything has played. The ground is the launcher's, not Attic's.")
    for key, name, size, state, w, h in (("widget_small", "Widget / Small, 2 x 2", "Small", "Playing", 530 / 2.625, 530 / 2.625),
                                         ("widget_wide", "Widget / Wide, 4 x 1", "Wide", "Playing", 384, 128),
                                         ("widget_large", "Widget / Large, 4 x 2", "Large", "Playing", 384, 232),
                                         ("widget_large_empty", "Widget / Large, nothing played", "Large", "Empty", 384, 232)):
        f = a.screen(key, name=name, source=WIDGET_SRC, w=w, h=h, fill=WALL["light"], allow_hex=True)
        wi = d.instance("Widget", f, name="Widget", x=16, y=16)
        setp(d, wi, Size=size, State=state)
        d.set(wi, width=w - 32, height=h - 32)
    a.close()
    _dark_wall(d, a.keys)
    _finish(d, a.keys, [("Code differs from mock", "The frame's ground is the render harness's launcher colour, not Attic's; "
                                                   "only the card is the widget.")])


def _wallpaper(d, f, theme="light", name="Wallpaper (system)"):
    top, bottom = ("#D9DEE6", "#AEB6C3") if theme == "light" else ("#3A3F4A", "#1C1F25")
    r = d.rect(f, name=name, w=411, h=914, x=0, y=0,
               fills=[{"type": "linearGradient", "stops": [top, bottom], "from": [0.5, 0], "to": [0.5, 1]}])
    return r


def _home_screen(d: Doc):
    a = d.area("Home screen", page=PAGE, note="Attic's widget in its three sizes on a neutral home screen, with the launcher "
                                              "icon among other apps. Everything but the widgets and Attic's icon is the "
                                              "launcher's.")
    f = a.screen("system_home_screen", name="System / Home screen with widgets", source=WIDGET_SRC)
    wall = _wallpaper(d, f)
    d.instance("Status bar", f, name="Status bar", x=0, y=0)
    y = 64
    for size, w, h in (("Large", 379, 200), ("Wide", 379, 96)):
        wi = d.instance("Widget", f, name=f"Widget: {size}", x=16, y=y)
        setp(d, wi, Size=size, State="Playing")
        d.set(wi, width=w, height=h)
        y += h + 24
    ws = d.instance("Widget", f, name="Widget: Small", x=16, y=y)
    setp(d, ws, Size="Small", State="Playing")
    d.set(ws, width=170, height=170)
    # A row of app icons: Attic's and the launcher's others.
    row = d.frame(f, name="Apps", layout="h", w=411, x=0, y=914 - 24 - 32 - 92, pad=[0, 16, 0, 16], main="spaceBetween",
                  sizing=("fixed", "hug"))
    for i in range(4):
        cell = d.frame(row, name="Attic" if i == 0 else "Other app (system)", layout="v", gap=6, w=76, cross="center",
                       sizing=("fixed", "hug"))
        if i == 0:
            ic = d.instance("Launcher icon", cell, name="Launcher icon")
            d.cmd("transform.scale", ids=[ic], factor=60 / 96)
        else:
            d.ellipse(cell, name="Icon (system)", w=60, h=60, fill="#FFFFFFB3", allow_hex=True)
        _sys_text(d, cell, "Attic" if i == 0 else "App", "System/Home label", "#1B1B1F", "Label (system)")
    d.instance("Gesture bar", f, name="Gesture bar", x=0, y=890)
    a.close()
    s = d.screens["system_home_screen"]
    d.set(s["map"][wall], fills=[{"type": "linearGradient", "stops": ["#3A3F4A", "#1C1F25"], "from": [0.5, 0], "to": [0.5, 1]}])
    _dark_labels(d, a.keys)
    _finish(d, a.keys, [("Code differs from mock", "The wallpaper, the other apps and their labels are the launcher's "
                                                   "(system), drawn neutral. The widget sizes are the renders' cells; a "
                                                   "launcher may give the 4 x 2 widget more height, and the album tray then "
                                                   "keeps its 56dp covers.")])


def _shade_top(d: Doc, f):
    """The pulled-down shade's clock, date and quick settings tiles: all Android's, drawn neutral."""
    _sys_text(d, f, "10:24", "System/Clock large", "#E3E3E8", "Clock (system)", x=24, y=36)
    _sys_text(d, f, "Mon, 23 November", "System/Body", "#C4C6CF", "Date (system)", x=26, y=112)
    tiles = d.frame(f, name="Quick settings (system)", layout="h", wrap=True, gap=8, crossGap=8, w=379, x=16, y=148,
                    sizing=("fixed", "hug"))
    for lig, lab, on in (("wifi", "Internet", True), ("bluetooth", "Bluetooth", False), ("flashlight_on", "Torch", False),
                         ("do_not_disturb_on", "Do Not Disturb", False), ("airplanemode_active", "Aeroplane mode", False),
                         ("screen_rotation", "Auto-rotate", True)):
        t = d.frame(tiles, name=f"Tile: {lab} (system)", layout="h", gap=10, w=185.5, h=64, pad=[0, 16, 0, 16], cross="center",
                    fill="#B7CAFF" if on else "#2D2F36", allow_hex=True, sizing=("fixed", "fixed"))
        d.set(t, cornerRadius=32)
        ic = drawable_icon(d, d.instance(f"Icon/{lig}", t, name="Icon"), wrap=True)
        d.override(ic, "Glyph", fill="#00143B" if on else "#E3E3E8", allow_hex=True)
        _sys_text(d, t, lab, "System/Body", "#00143B" if on else "#E3E3E8", "Label")


def _shade_and_lock(d: Doc):
    a = d.area("Notification shade and lock screen", page=PAGE,
               note="Android draws the media player from Attic's MediaSession (MediaStyle): Attic supplies the title, artist, "
                    "cover, the Headroom small icon and the shuffle and favourite buttons; the card, its colours and layout are "
                    "the system's. The same player shows on the lock screen. Android draws both in its own theme, not "
                    "Attic's, so there is one row here, in the system's dark theme; with the phone in light theme the shade "
                    "and the card turn light and Attic's parts stay the same. Song analysis adds its own progress "
                    "notification to the shade while it runs.")
    # Shade
    f = a.screen("system_notification_shade", name="System / Notification shade", source="playback/PlaybackService.kt · PlaybackService",
                 fill="#1B1C20", allow_hex=True)
    _shade_top(d, f)
    mn = d.instance("Media notification", f, name="Media notification", x=16, y=380)
    _modes(d, d.instance("Analysis notification", f, name="Analysis notification", x=16, y=380 + 184 + 8), "Dark")
    d.instance("Gesture bar", f, name="Gesture bar", x=0, y=890)

    # Song analysis on its own: it runs while the phone charges, often with nothing playing (AnalysisWorker).
    f = a.screen("system_shade_analysis", name="System / Notification shade, analysing songs",
                 source="analysis/AnalysisWorker.kt · progressNotification", fill="#1B1C20", allow_hex=True)
    _shade_top(d, f)
    an = d.instance("Analysis notification", f, name="Analysis notification", x=16, y=380)
    # setColor takes Comfort's accent in the phone's current theme (comfortAccent), dark here like the shade.
    _modes(d, an, "Dark")
    d.instance("Gesture bar", f, name="Gesture bar", x=0, y=890)

    # Lock screen
    f = a.screen("system_lock_screen", name="System / Lock screen player", source="playback/PlaybackService.kt · PlaybackService",
                 fill="#1B1C20", allow_hex=True)
    _wallpaper(d, f, theme="dark")
    d.instance("Status bar", f, name="Status bar", x=0, y=0)
    lk = drawable_icon(d, d.instance("Icon/lock", f, name="Lock (system)", x=(411 - 24) / 2, y=56))
    d.override(lk, "Glyph", fill="#E3E3E8", allow_hex=True)
    clk = d.frame(f, name="Clock (system)", layout="v", w=411, x=0, y=120, cross="center", sizing=("fixed", "hug"))
    _sys_text(d, clk, "10:24", "System/Clock large", "#E3E3E8", "Time")
    _sys_text(d, clk, "Mon, 23 November", "System/Body", "#E3E3E8", "Date")
    d.instance("Media notification", f, name="Media notification", x=16, y=560)
    d.instance("Gesture bar", f, name="Gesture bar", x=0, y=890)
    # One row: the system's theme decides these, and a Dark copy would repeat the same raw colours.
    a.close(dark=False)
    for k in a.keys:
        d.set(d.screens[k]["light"], name=f"{d.screens[k]['name']} · System theme (dark)")
    # The status bar over the dark lock screen and the shade reads light.
    for k in a.keys:
        for fid in (d.screens[k]["light"],):
            for sb in d.find(fid, name="Status bar", limit=5):
                d.set_mode(sb, "Palette", "Dark")
    _finish(d, a.keys, [("Code differs from mock", "Only the cards' content is Attic's: the media player's title, artist, "
                                                   "cover, small icon, shuffle and favourite, and the analysis notification's "
                                                   "icon colour, text, progress and Stop. Shade, quick settings, clock and "
                                                   "wallpaper are Android's, drawn neutral; the media card's colours come from "
                                                   "the cover and differ between Android versions.")])
    s = d.screens["system_shade_analysis"]["light"]
    d.annotate(s, "Drawn from the code; there is no reference render. AnalysisWorker posts this while it analyses songs "
                  "for radio, which it does while the phone charges, often with nothing playing: an ongoing, silent "
                  "notification on the \"Song analysis\" channel (low importance). Attic gives it the graphic_eq small "
                  "icon in a circle of Comfort's accent (setColor, in the phone's current theme, so the Dark accent here), "
                  "\"Analysing your songs\", \"41 of 69\", the progress and Stop, which cancels the run. A tap opens Attic.",
               "Note")


def _splash_and_icon(d: Doc):
    a = d.area("Splash and launcher icon", page=PAGE,
               note="The splash: Android 12+ draws the window background (the Comfort ground) with the launcher icon until "
                    "Attic's first frame. The launcher icon is one adaptive icon for both styles: Headroom in colour on "
                    "terracotta, and the monochrome layer the system tints for themed icons.")
    f = a.screen("system_splash", name="System / Splash", source="res/values/themes.xml · Theme.Attic")
    d.instance("Splash", f, name="Splash", x=0, y=0)
    d.instance("Status bar", f, name="Status bar", x=0, y=0)
    d.instance("Gesture bar", f, name="Gesture bar", x=0, y=890)

    f = a.screen("system_launcher_icon", name="System / Launcher icon", source="res/mipmap-anydpi-v26/ic_launcher.xml · adaptive-icon",
                 fill=WALL["light"], allow_hex=True)
    col = d.frame(f, name="Icons", layout="v", gap=40, w=411, x=0, y=120, cross="center", sizing=("fixed", "hug"))
    for layer, cap in (("Colour", "Colour: terracotta background layer, Headroom in cream and dark brown"),
                       ("Monochrome", "Monochrome: themed icon, tinted by the system")):
        cell = d.frame(col, name=f"{layer} icon", layout="v", gap=12, cross="center", sizing=("hug", "hug"))
        ic = d.instance("Launcher icon", cell, name="Launcher icon")
        setp(d, ic, Layer=layer)
        d.cmd("transform.scale", ids=[ic], factor=1.5)
        _sys_text(d, cell, "Attic", "System/Home label", "#1B1B1F", "Label (system)")
        _sys_text(d, cell, cap, "System/Small", "#3F3F46", "Caption (system)", width=300, textAlign="center",
                  textAutoResize="height")
    a.close()
    _dark_wall(d, ["system_launcher_icon"])
    _dark_labels(d, ["system_launcher_icon"])
    _finish(d, a.keys, [("Code differs from mock", "Comfort and Expressive share one launcher icon; the launcher's own mask "
                                                   "(a circle here) cuts it. The ground and labels are the launcher's.")])
