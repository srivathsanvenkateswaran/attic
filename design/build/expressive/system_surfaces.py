"""Page System surfaces: the widget in its sizes, on a home screen; the notification shade with the media
notification; the lock-screen player; the splash; the launcher icon in colour and themed monochrome.

Owner: settings screens agent. Attic's parts are instances of the Components / System set (Widget,
System/Media notification, Splash, Launcher icon). The phone around them (wallpaper, shade, quick settings,
other apps' icons, the lock-screen clock) is Android's and is drawn from the ``Phone`` collection
(foundations.py, documented on the Colour page), every layer of it named "System …".
"""
from __future__ import annotations

from figbuild.kit import Doc, V
from expressive.components_core import inst
from expressive.foundations import (tidy_doc_pages, paragraph_line_heights, document_paragraph_styles, bind_section_strokes,
                                    RENDER_DIFFERS)
from expressive.components_states import mark

KEYS = ["widget_small", "widget_wide", "widget_large", "widget_large_empty"]
REFS: dict[str, str] = {k: f"expressive-{{theme}}-{k}" for k in KEYS}
H = 914

def label(d: Doc, parent, text, *, x, y, style="System/Small", color="Phone/on-wallpaper", w=None, name=None):
    t = d.text(parent, text, style=style, color=color, name=name or f"System label {text[:20]}", x=x, y=y)
    if w:
        d.set(t, width=w, textAlign="center", textAutoResize="height")
    return t


def other_app(d: Doc, parent, lig, name, *, x, y, size=56):
    """Another app's icon on the home screen: a grey disc with a glyph and its name (system)."""
    g = d.frame(parent, name=f"System app · {name}", w=size + 16, h=size + 22, x=x, y=y)
    disc = d.frame(g, name="System icon", layout="h", w=size, h=size, x=8, y=0, cross="center", main="center",
                   fill="Phone/icon", radius=size / 2)
    d.icon(disc, lig, size=24, color="Phone/on-icon", name="System glyph")
    label(d, g, name, x=0, y=size + 4, w=size + 16)
    return g


def attic_icon(d: Doc, parent, *, x, y, size=56, layer="Colour", caption=True):
    g = d.frame(parent, name=f"Attic icon · {layer}", w=size + 16, h=size + 22, x=x, y=y)
    ic = inst(d, "Launcher icon", g, name="Launcher icon", Layer=layer, x=8, y=0)
    d.cmd("transform.scale", ids=[ic], factor=size / 108)
    d.set(ic, x=8, y=0)
    if caption:
        label(d, g, "Attic", x=0, y=size + 4, w=size + 16)
    return g


def status(d: Doc, fid):
    return inst(d, "Status bar", fid, name="Status bar", x=0, y=0)


def gesture(d: Doc, fid, h=H):
    return inst(d, "Gesture bar", fid, name="Gesture bar", x=0, y=h - 24)


def annotate(d: Doc, key, text, *notes, render=()):
    """Source, notes (category Content) and the ways the phone differs from the reference render."""
    s = d.screens[key]
    for fid in (s["light"], s["dark"]):
        if fid:
            d.annotate(fid, text, "Source")
            for n in notes:
                d.annotate(fid, n, "Content")
            for n in render:
                d.annotate(fid, n, RENDER_DIFFERS)
            d.ready_for_dev(fid)


def build(d: Doc, ctx: dict):
    page = "System surfaces"
    wsrc = "widget/PlayerWidget.kt · PlayerWidget"
    corner = ("The reference render draws the widget's covers with square corners: Robolectric does not clip a view "
              "to its outline. On a phone the art is clipped to 18, 16 and 20dp and the album covers to 12dp, as drawn here.")

    # Widget sizes, as the renders frame them --------------------------------------------------------
    a = d.area("Widget", page=page, note="The home-screen widget in its three layouts, each as the reference renders frame it: "
                                         "the widget at launcher size with 16dp of plain ground around it. Playing takes "
                                         "the cover's scheme; before anything has played the wallpaper's.")
    for key, name, size, state, w, h in (("widget_small", "Widget / Small (2 x 2)", "Small", "Playing", 202, 202),
                                         ("widget_wide", "Widget / Wide (4 x 1)", "Wide", "Playing", 384, 128),
                                         ("widget_large", "Widget / Large (4 x 2)", "Large", "Playing", 384, 232),
                                         ("widget_large_empty", "Widget / Large, nothing played", "Large", "Empty", 384, 232)):
        fid = a.screen(key, name=name, source=wsrc, w=w, h=h, fill="Phone/backdrop")
        inst(d, "Widget", fid, name="Widget", Size=size, State=state, x=16, y=16)
    a.close()
    for key in ("widget_small", "widget_wide", "widget_large", "widget_large_empty"):
        annotate(d, key, "Code: app/src/main/java/app/attic/player/widget/PlayerWidget.kt · PlayerWidget (RemoteViews, "
                         f"no Compose). Layout: app/src/main/res/layout/widget_expressive_{key.split('_')[1]}.xml; "
                         "colours: widget/WidgetColors.kt.", render=(corner,))

    # Home screen, shade, lock screen ------------------------------------------------------------------
    a = d.area("On the phone", page=page,
               note="Attic's widget and icon on a home screen, the media notification in the shade, and the same player on "
                    "the lock screen. Everything named System is Android's, drawn only to place Attic's parts.")
    fid = a.screen("system_home", name="Home screen / Widget and icon", source=wsrc, fill="Phone/wallpaper")
    d.rect(fid, name="System wallpaper tone", w=411, h=420, x=0, y=494, fill="Phone/wallpaper-deep")
    inst(d, "Widget", fid, name="Widget large", Size="Large", State="Playing", x=29.5, y=72)
    inst(d, "Widget", fid, name="Widget small", Size="Small", State="Playing", x=29.5, y=292)
    other_app(d, fid, "photo_camera", "Camera", x=221, y=300)
    other_app(d, fid, "map", "Maps", x=309, y=300)
    other_app(d, fid, "call", "Phone", x=221, y=390)
    other_app(d, fid, "chat_bubble", "Messages", x=309, y=390)
    dock = d.frame(fid, name="System dock", w=379, h=80, x=16, y=914 - 24 - 104, fill="Phone/icon", radius=40)
    d.set(dock, opacity=1)
    attic_icon(d, fid, x=24, y=640)
    other_app(d, fid, "schedule", "Clock", x=112, y=640)
    other_app(d, fid, "calendar_month", "Calendar", x=200, y=640)
    other_app(d, fid, "settings", "Settings", x=288, y=640)
    for n, lig in enumerate(("call", "chat_bubble", "photo_camera", "search")):
        disc = d.frame(dock, name=f"System dock icon {n + 1}", layout="h", w=56, h=56, x=24 + n * 90, y=12,
                       cross="center", main="center", fill="Phone/card", radius=28)
        d.icon(disc, lig, size=24, color="Phone/on-icon", name="System glyph")
    status(d, fid)
    gesture(d, fid)

    fid = a.screen("system_shade", name="Notification shade / Playing", source="playback/PlaybackService.kt · PlaybackService",
                   fill="Phone/shade")
    status(d, fid)
    label(d, fid, "Sunday, 23 November", x=26, y=52, style="System/Body", color="Phone/on-tile", name="System date")
    tiles = d.frame(fid, name="System quick settings", layout="h", gap=8, w=379, x=16, y=88, sizing=("fixed", "hug"),
                    wrap=True, crossGap=8)
    for lig, name, on in (("wifi", "Internet", True), ("bluetooth", "Bluetooth", False), ("flashlight_on", "Torch", False),
                          ("do_not_disturb_on", "Do not disturb", False)):
        t = d.frame(tiles, name=f"System tile · {name}", layout="h", gap=8, pad=[0, 16, 0, 16], h=64, w=185.5,
                    cross="center", fill="Phone/tile-on" if on else "Phone/tile", radius=32, sizing=("fixed", "fixed"))
        d.icon(t, lig, size=22, color="Phone/on-tile-on" if on else "Phone/on-tile", name="System glyph")
        d.text(t, name, style="System/Body", color="Phone/on-tile-on" if on else "Phone/on-tile", name="System label")
    n = inst(d, "System/Media notification", fid, name="Media notification", x=16, y=248)
    other = d.frame(fid, name="System notification · another app", layout="h", gap=12, pad=16, w=379, x=16, y=440,
                    fill="Phone/card", radius=24, sizing=("fixed", "hug"), cross="center")
    disc = d.frame(other, name="System icon", layout="h", w=36, h=36, cross="center", main="center", fill="Phone/icon", radius=18)
    d.icon(disc, "calendar_month", size=20, color="Phone/on-icon", name="System glyph")
    col = d.frame(other, name="System text", layout="v", sizing=("fill", "hug"))
    d.text(col, "Calendar", style="System/Small", color="Phone/on-tile", name="System app")
    d.text(col, "Dentist at 4 pm", style="System/Title", color="Phone/on-tile", name="System title")
    gesture(d, fid)

    # Song analysis while the phone charges: Attic's progress notification (AnalysisWorker.progressNotification).
    fid = a.screen("system_shade_analysis", name="Notification shade / Analysing songs",
                   source="analysis/AnalysisWorker.kt · progressNotification", fill="Phone/shade")
    status(d, fid)
    label(d, fid, "Sunday, 23 November", x=26, y=52, style="System/Body", color="Phone/on-tile", name="System date")
    mn = inst(d, "System/Media notification", fid, name="Media notification", x=16, y=88)
    an = d.frame(fid, name="Analysis notification", layout="v", gap=8, pad=16, w=379, x=16,
                 y=round(88 + d.get(mn)["height"] + 12, 1), fill="Phone/card",
                 radius=24, sizing=("fixed", "hug"))
    head = d.frame(an, name="Header", layout="h", gap=8, cross="center", sizing=("fill", "hug"))
    disc = d.frame(head, name="Small icon", layout="h", w=24, h=24, cross="center", main="center", fill="Phone/tile-on",
                   radius=12)
    d.icon(disc, "graphic_eq", size=16, color="Phone/on-tile-on", name="Glyph")
    d.text(head, "Attic", style="System/Small", color="Phone/on-tile", name="App name")
    d.text(an, "Analysing your songs", style="System/Title", color="Phone/on-tile", name="Title")
    d.text(an, "41 of 69", style="System/Body", color="Phone/on-tile", name="Progress text")
    bar = d.frame(an, name="Progress", w=347, h=4, fill="Phone/tile", radius=2, sizing=("fill", "fixed"), clip=True)
    d.rect(bar, name="Progress done", w=round(347 * 41 / 69, 1), h=4, x=0, y=0, fill="Phone/tile-on", radius=2)
    act = d.frame(an, name="Actions", layout="h", pad=[4, 0, 0, 0], sizing=("fill", "hug"))
    d.text(act, "Stop", style="System/Title", color="Phone/tile-on", name="Action Stop")
    gesture(d, fid)

    fid = a.screen("system_lock", name="Lock screen / Playing", source="playback/PlaybackService.kt · PlaybackService",
                   fill="Phone/wallpaper")
    d.rect(fid, name="System wallpaper tone", w=411, h=420, x=0, y=494, fill="Phone/wallpaper-deep")
    status(d, fid)
    label(d, fid, "10:24", x=0, y=120, style="System/Clock", w=411, name="System clock")
    label(d, fid, "Sunday, 23 November", x=0, y=208, style="System/Body", w=411, name="System date")
    inst(d, "System/Media notification", fid, name="Media notification", x=16, y=560)
    lock = d.frame(fid, name="System unlock", layout="h", w=48, h=48, x=181.5, y=820, cross="center", main="center")
    d.icon(lock, "lock", size=24, color="Phone/on-wallpaper", name="System glyph")
    gesture(d, fid)
    a.close()
    annotate(d, "system_home", f"Code: app/src/main/java/app/attic/player/{wsrc}; res/mipmap-anydpi-v26/ic_launcher.xml")
    annotate(d, "system_shade_analysis",
             "Code: app/src/main/java/app/attic/player/analysis/AnalysisWorker.kt · progressNotification "
             "(NotificationCompat, channel \"Song analysis\"). Android draws the card; Attic supplies the "
             "graphic_eq small icon, the title, \"41 of 69\", the progress and the Stop action.",
             "Shown while analysis runs, which it does while the phone charges. Expressive leaves the accent to the "
             "system, which takes it from the wallpaper; the tap opens Attic. Low importance and silent, so it "
             "sits in the shade without sound.")
    for key in ("system_shade", "system_lock"):
        annotate(d, key, "Code: app/src/main/java/app/attic/player/playback/PlaybackService.kt · PlaybackService "
                         "(Media3 MediaStyle notification). Android draws the card; Attic supplies the session's metadata, "
                         "art, the favourite and shuffle buttons and the small icon.")

    # Splash and launcher icon ------------------------------------------------------------------------
    a = d.area("Splash and launcher icon", page=page,
               note="The splash Android draws on a cold start, and the adaptive icon: Headroom in colour on terracotta, and "
                    "the one-colour layer a themed launcher tints from the wallpaper.")
    fid = a.screen("splash", name="Splash / Cold start", source="res/values/themes.xml · Theme.Attic.Splash.Expressive")
    inst(d, "Splash", fid, name="Splash", x=0, y=0)
    status(d, fid)
    gesture(d, fid)
    fid = a.screen("launcher_icon", name="Launcher icon / Colour and themed", source="res/mipmap-anydpi-v26/ic_launcher.xml",
                   fill="Phone/wallpaper")
    status(d, fid)
    label(d, fid, "Colour", x=0, y=96, style="System/Title", w=205, name="System caption colour")
    label(d, fid, "Themed", x=206, y=96, style="System/Title", w=205, name="System caption themed")
    for n, layer in enumerate(("Colour", "Monochrome")):
        big = inst(d, "Launcher icon", fid, name=f"Launcher icon {layer}", Layer=layer, x=48.5 + n * 206, y=136)
        attic_icon(d, fid, x=67 + n * 206, y=280, layer=layer)
    for n, (lig, name) in enumerate((("photo_camera", "Camera"), ("map", "Maps"), ("call", "Phone"), ("chat_bubble", "Messages"))):
        other_app(d, fid, lig, name, x=24 + n * 92, y=420)
    attic_icon(d, fid, x=24, y=520)
    attic_icon(d, fid, x=116, y=520, layer="Monochrome")
    gesture(d, fid)
    a.close()
    annotate(d, "splash", "Code: app/src/main/res/values/themes.xml · Theme.Attic.Splash.Expressive "
                          "(windowSplashScreenAnimatedIcon ic_launcher_foreground on headroom_ground)",
             "The splash ground is the wallpaper scheme's neutral at tone 99 / 10 (system_neutral1_10 and _900); this "
             "file shows it with surface.")
    annotate(d, "launcher_icon", "Code: app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml, drawable/ic_launcher_foreground.xml, "
                                 "drawable/ic_launcher_monochrome.xml. The launcher applies its own mask; a circle is shown.")
    # Every module that adds screens, components or cards has run: wrapped text takes the code's line height, and
    # each documentation and component page gets one card width.
    n = paragraph_line_heights(d)
    print(f"    text layers moved between a style and its Paragraph partner: {n}", flush=True)
    document_paragraph_styles(d)
    tidy_doc_pages(d)
    bind_section_strokes(d)
