"""Smart transitions are in the code: Attic Expressive's Transitions frames stop being a proposal (version 1.3.0).

Like `transitions.py` this edits the saved file and rebuilds nothing. The code landed as `transitions.py` drew it,
so the Transitions frames export the same pixels before and after. What changes is the handoff, and one frame of
the Equaliser section.

    cd design/build
    python3 -m expressive.transitions_landed /tmp/attic-design/expressive-transitions.figcraft \
        /tmp/attic-design/expressive-landed.figcraft \
        [--export /tmp/attic-dev/smart-transitions-design/landed] [--check /tmp/attic-design/expressive-landed-check]

Look at the result, then copy it over the file in design/. It refuses a file without the Transitions section and
a file whose Transitions frames are already marked ready.

Screens (section "Transitions", all 6 states, Light and Dark)
-------------------------------------------------------------
The `Proposed` annotations go. Each frame is marked ready for development and gains a `Note` saying what the code
does that the drawing can't show. The Settings frames' `Source` names the composables that draw the rows now:
TransitionsRow and CrossfadeLengthRow. The section's note and the flow's description stop calling it a proposal.

Screens (section "Equaliser")
-----------------------------
Settings, Playback: 1.2.0 drew the whole Playback group, with the Crossfade row the code had then (Off). The code
has Transitions there now, so the row becomes Transitions in the same state: Off, with what Off does as its third
line. The Equaliser row, its link to the Settings page and every other frame of the section are untouched.

Components
----------
Seek bar           the description says what SeekBar's `mark` draws; the `Proposed` annotations on the set and its
                   card go.
List row/Setting   untouched. Its description still gives "Crossfade's note" as the example of Status line=Plain,
                   which is the Transitions row's third line now. FigCraft re-creates the layers inside every
                   instance of a set whose variants' description changes (the rows of every Settings screen), so a
                   change of wording isn't worth it.

Handoff
-------
Cover 1.3.0, without the sentence that called the section a proposal; the 1.3.0 Changelog entry `transitions.py`
wrote is rewritten to say what landed, so the version has one entry, dated 2026-11-25.

--export writes the 12 frames, the redrawn Equaliser frame, Seek bar's spec card, the Cover and the Changelog entry
as PNG. --check exports frames this module doesn't redraw from the source and from the result, and fails unless
each pair is pixel-identical.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from PIL import Image

from figbuild.kit import Doc, DENSITY
from expressive import transitions as T
from expressive.transitions import attach, card_of, restack, screen_frame, top_named

VERSION = "1.3.0"
UPDATED = "2026-11-25"
AUTHOR = "Attic maintainers"
NOTE = "Note"

NAMES = {
    "transitions_settings_off": "Transitions / Settings, off",
    "transitions_settings_crossfade": "Transitions / Settings, Crossfade",
    "transitions_settings_smart": "Transitions / Settings, Smart",
    "transitions_picker": "Transitions / Transitions menu",
    "transitions_length_picker": "Transitions / Crossfade length menu",
    "transitions_now_playing": "Transitions / Now Playing, next song marked",
}

SRC_SETTINGS = "ui/expressive/ExpressiveSettings.kt · ExpressiveSettings (TransitionsRow, CrossfadeLengthRow)"
SOURCES = {key: SRC_SETTINGS for key in NAMES}
SOURCES["transitions_now_playing"] = T.SRC_NP
EQ_FRAME = "Equaliser / Settings, Playback"

# What the code does that a frame can't show, in place of each frame's Proposed annotation.
NOTES = {
    "transitions_settings_off": [
        "Off is the default and hides Crossfade length (SettingsState.showsCrossfadeLength). The Equaliser row under "
        "it is EqualiserRow, as the Equaliser section draws it."],
    "transitions_settings_crossfade": [
        "Crossfade length is there only while the choice is Crossfade or Smart. Its menu lists 2 to 12 seconds, "
        "6 to start with."],
    "transitions_settings_smart": [
        "With Smart, Crossfade length gains a third line saying what it is for: songs Smart has no analysis for "
        "yet play a crossfade of this length."],
    "transitions_picker": [
        "ExpressiveMenu anchored to the Transitions row: under the row, its start edge on the row's. "
        "ExpressiveMenuItem ticks the current choice and keeps the glyph's space on the others."],
    "transitions_length_picker": [
        "ExpressiveMenu anchored to the Crossfade length row: 2 to 12 seconds, the current one ticked."],
    "transitions_now_playing": [
        "SeekBar's mark: a 4dp dot in on-surface-variant on the rest of the track where the next song starts coming "
        "in, here at 3:41 of 3:52. Now Playing passes it only while Smart has a plan for this song "
        "(NowPlayingState.transitionAtMs), and the bar draws it until the handle reaches it. Nothing else on the "
        "screen changes.",
        "The bar hands over to the next song at the dot, before its own end, because the overlap belongs to the next "
        "song's card. For most songs the dot sits in the last few seconds of the track."],
}

SECTION_NOTE = (
    "Smart transitions, as the code draws them. Settings > Playback's Transitions row offers Off, Crossfade (a fixed "
    "fade) or Smart, which starts the next song where this one winds down. Crossfade length sits under it while "
    f"songs overlap. On Now Playing a dot on the seek bar marks where the next song comes in while Smart has a plan. "
    f"The feature is described in {T.DOC}.")

FLOW_DESCRIPTION = ("Settings > Playback with transitions off. The Transitions row opens its menu; Smart shows "
                    "Crossfade length, whose row opens the lengths.")

SEEK_NOTE = (" Show next song mark is SeekBar's mark: a 4dp dot in on-surface-variant on the rest of the track where "
             "the next song starts coming in, at the x the handle's centre has at that moment. Now Playing passes it "
             "only while Smart has a plan for the song, and the bar draws it until the handle reaches it. Shown at "
             "3:41 of 3:52.")

CHANGES = (
    "Smart transitions, drawn and matching the code (docs/features/smart-transitions.md). Settings > Playback's "
    "Crossfade row is now Transitions: Off, Crossfade (a fixed fade) or Smart, which starts the next song where this "
    "one winds down. Crossfade length (2 to 12 seconds, 6 to start with) is a row under it while the choice is "
    "Crossfade or Smart.\n"
    "• Screens: a new Transitions section of 6 states in Light and Dark, marked ready for development. Settings > "
    "Playback with transitions off, on Crossfade and on Smart; the Transitions menu; the Crossfade length menu; Now "
    "Playing with the next song marked on the seek bar. The Settings frames' Source names TransitionsRow and "
    "CrossfadeLengthRow in ExpressiveSettings.kt, and a Note on each frame says what the code does that the drawing "
    "can't show.\n"
    "• Changed component: Seek bar gains Show next song mark, SeekBar's mark parameter in PlayControls.kt: a 4dp dot "
    "on the rest of the track where the next song starts coming in.\n"
    "• Equaliser / Settings, Playback: the Crossfade row is redrawn as Transitions, in the same state (Off). The "
    "Equaliser row is as 1.2.0 drew it.\n"
    "• Prototype: flow 13 Transitions, from Settings > Playback.\n"
    "• The Comfort style's mark for the same point was reviewed and not built. Attic Comfort's Changelog says why.")

# Frames this module doesn't redraw, for --check: the Transitions frames, and these.
UNTOUCHED = ["Settings / Top", "Settings / Whole list (full length)", "Settings / Backup",
             "Now Playing / Playing", "Home / All", "Queue / Up next", "Search / Results", "Equaliser / Settings page",
             "Equaliser / Settings page, nothing saved", "Equaliser / Sheet, default",
             "Equaliser / Sheet, song's own setting", "Equaliser / Now Playing, indicator",
             "Equaliser / Track menu, song's own setting"]


# ───────────────────────────────────────────────────────────────── screens
def annotations(d: Doc, node: int) -> list[dict]:
    return sorted(d.cmd("annotation.list", ids=[node]).get("annotations", []), key=lambda a: a["index"])


def drop_proposed(d: Doc, node: int) -> int:
    """Remove a layer's Proposed annotations, last first so the indexes left to remove stay true."""
    gone = [a["index"] for a in annotations(d, node) if (a.get("category") or {}).get("name") == T.PROPOSED]
    for index in reversed(gone):
        d.cmd("annotation.remove", id=node, index=index)
    return len(gone)


def land_screens(d: Doc, ctx: dict):
    d.page(T.SCREENS)           # a flow is found by its start frame on the current page
    frames = {}
    for key, name in NAMES.items():
        for theme in ("Light", "Dark"):
            f = screen_frame(d, name, theme)
            if not drop_proposed(d, f):
                raise SystemExit(f"transitions_landed: {name} · {theme} has no Proposed annotation")
            for a in annotations(d, f):
                if (a.get("category") or {}).get("name") == "Source":
                    d.cmd("annotation.edit", id=f, index=a["index"], text=f"Compose: {SOURCES[key]}")
            for n in NOTES[key]:
                d.annotate(f, n, NOTE)
            d.ready_for_dev(f)
            frames[(key, theme.lower())] = f
    ctx["landed_frames"] = frames
    # The section's note: the same width, so a change of length only moves its own last line.
    section = top_named(d, d.pages[T.SCREENS], T.SECTION, "SECTION")
    note = top_named(d, section, "Note")
    d.set(note, characters=SECTION_NOTE)
    nb = d.get(note)
    top = min(d.get(f)["y"] for f in frames.values())
    if nb["y"] + nb["height"] > top - 24:
        raise SystemExit("transitions_landed: the section's note now runs into the screens")
    start = frames[("transitions_settings_off", "light")]
    d.cmd("proto.flow.rename", start=start, name=T.FLOW[1], description=FLOW_DESCRIPTION)


# ───────────────────────────────────────────────────────────────── the equaliser's Settings frame
def land_equaliser_frame(d: Doc, ctx: dict):
    """Equaliser / Settings, Playback draws the whole Playback group. Its Crossfade row (Off) becomes the row the
    code has now, in the same state: Transitions, Off, and what Off does as the third line."""
    d.page(T.SCREENS)
    P = ctx["components"]["List row/Setting"]["props"]
    mode = T.S["off"]
    frames = {}
    for theme in ("Light", "Dark"):
        f = screen_frame(d, EQ_FRAME, theme)
        group = top_named(d, top_named(d, f, "Settings list"), "Playback group")
        try:
            cell = top_named(d, group, "Crossfade cell")
            row = top_named(d, cell, "Crossfade")
        except KeyError:
            raise SystemExit(f"transitions_landed: {EQ_FRAME} · {theme} has no Crossfade row")
        if d.get(d.child(row, "Summary"))["characters"] != mode:
            raise SystemExit(f"transitions_landed: {EQ_FRAME} · {theme}'s Crossfade row isn't {mode}")
        d.cmd("instance.setProperties", ids=[row], properties={P["Title"]: T.S["transitions"], P["Summary"]: mode,
                                                               P["Status text"]: T.status(mode)})
        d.set(row, name="Transitions")
        d.set(cell, name="Transitions cell")
        d.ready_for_dev(f)          # marked again, so the frame doesn't show as edited since it was marked
        frames[theme.lower()] = f
    ctx["landed_equaliser_frames"] = frames


# ───────────────────────────────────────────────────────────────── components
def _redescribe(d: Doc, ctx: dict, set_name: str, page: str, was: str, now: str):
    """Swap one passage of a component's description, on its variants and on its spec card's copy."""
    info = ctx["components"][set_name]
    holders = list(info["variants"].values()) or [info["id"]]
    text = d.get(holders[0], fields=["description"])["description"]
    if was not in text:
        raise SystemExit(f"transitions_landed: {set_name}'s description isn't the one transitions.py wrote")
    d.set(holders, description=text.replace(was, now))
    copy = d.child(top_named(d, card_of(d, page, info["id"]), "Heading"), "Description")
    shown = d.get(copy)["characters"]
    if was.strip() not in shown:
        raise SystemExit(f"transitions_landed: {set_name}'s card doesn't show the description transitions.py wrote")
    d.set(copy, characters=shown.replace(was.strip(), now.strip()))


def land_components(d: Doc, ctx: dict):
    C = ctx["components"]
    d.page("Components / Playback")
    _redescribe(d, ctx, "Seek bar", "Components / Playback", T.SEEK_NOTE, SEEK_NOTE)
    for n in (C["Seek bar"]["id"], card_of(d, "Components / Playback", C["Seek bar"]["id"])):
        if not drop_proposed(d, n):
            raise SystemExit("transitions_landed: Seek bar has no Proposed annotation")
    restack(d, "Components / Playback")


# ───────────────────────────────────────────────────────────────── cover and changelog
def update_cover(d: Doc, ctx: dict):
    page = d.page("Cover")
    label = ctx["components"]["Filter chip"]["props"]["Label"]
    for n in d.find(page, type="INSTANCE", name="Chip · ", limit=20):
        nm = d.get(n)["name"]
        new = (f"Version {VERSION}" if nm.startswith("Chip · Version ") else
               f"Last updated {UPDATED}" if nm.startswith("Chip · Last updated ") else None)
        if new and nm != f"Chip · {new}":
            d.cmd("instance.setProperties", ids=[n], properties={label: new})
            d.set(n, name=f"Chip · {new}")
    for t in d.find(page, type="TEXT", name="What", limit=40):
        text = d.get(t)["characters"]
        new = re.sub(r" The Transitions section is a proposal that isn't in the app yet\.$", "", text)
        if new != text:
            d.set(t, characters=new)
            ctx["landed_cover_line"] = new


def update_changelog(d: Doc, ctx: dict):
    """The version's entry, rewritten to say what landed. `transitions.py` wrote it as a proposal; the two steps are
    one version."""
    page = d.page("Changelog")
    entries = top_named(d, page, "Entries")
    before = d.get(entries)["height"]
    P = ctx["components"]["Changelog entry"]["props"]
    top = d.get(entries, depth=1)["children"][0]
    if top["name"] != T.ENTRY:
        raise SystemExit(f"transitions_landed: the top Changelog entry is {top['name']!r}, not {T.ENTRY!r}")
    e = top["id"]
    d.cmd("instance.setProperties", ids=[e], properties={P["Version"]: VERSION, P["Date"]: UPDATED, P["Author"]: AUTHOR,
                                                         P["Changes"]: CHANGES})
    grew = d.get(entries)["height"] - before
    how = top_named(d, page, "Card · Adding a version")
    d.set(how, y=d.get(how)["y"] + grew)
    ctx["landed_entry"] = e


# ───────────────────────────────────────────────────────────────── build
def build(d: Doc, ctx: dict):
    try:
        first = screen_frame(d, NAMES["transitions_settings_off"])
    except KeyError:
        raise SystemExit("transitions_landed: this file has no Transitions section; run expressive.transitions first")
    if d.get(first).get("devStatus"):
        raise SystemExit("transitions_landed: the Transitions frames are already marked ready")
    land_screens(d, ctx)
    land_equaliser_frame(d, ctx)
    land_components(d, ctx)
    update_cover(d, ctx)
    update_changelog(d, ctx)
    d.page("Cover")


def export(d: Doc, ctx: dict, out: Path, style: str = "expressive"):
    """The 12 frames at the renders' density, named as `transitions.py` names them, the redrawn Equaliser frame, then
    the spec card, Cover and entry."""
    d.page(T.SCREENS)
    for (key, theme), f in ctx["landed_frames"].items():
        d.export_png(f, out / f"{style}-{theme}-{T.EXPORT_NAMES[key]}.png", scale=DENSITY)
    for theme, f in ctx["landed_equaliser_frames"].items():
        d.export_png(f, out / f"{style}-{theme}-equaliser-settings-playback.png", scale=DENSITY)
    d.page("Components / Playback")
    seek = ctx["components"]["Seek bar"]["id"]
    d.export_png(card_of(d, "Components / Playback", seek), out / f"{style}-component-seek-bar.png", scale=1)
    page = d.page("Cover")
    tops = [n["id"] for n in d.get(page, depth=1).get("children", [])]
    d.tool("export", ids=tops, format="png", scale=0.5, path=str(out / f"{style}-cover.png"))
    d.page("Changelog")
    d.export_png(ctx["landed_entry"], out / f"{style}-changelog-entry.png", scale=1)


def _shots(path: Path, out: Path, names: list[str]) -> dict[str, Path]:
    """Export screen frames of a saved file, Light and Dark, at the renders' density."""
    out.mkdir(parents=True, exist_ok=True)
    d = Doc("Attic Expressive", read_roots=[path.resolve().parent], write_roots=[out.resolve()])
    got = {}
    try:
        d.tool("open_file", path=str(path.resolve()))
        attach(d, {})
        d.page(T.SCREENS)
        for name in names:
            for theme in ("Light", "Dark"):
                p = out / (re.sub(r"[^a-z0-9]+", "-", f"{name} {theme}".lower()).strip("-") + ".png")
                d.export_png(screen_frame(d, name, theme), p, scale=DENSITY)
                got[f"{name} · {theme}"] = p
    finally:
        d.close()
    return got


def check(src: Path, out: Path, work: Path, names: list[str] | None = None) -> list[str]:
    """Export the same frames from `src` and from `out` and compare them pixel for pixel. Returns the frames that
    differ. The default frames are ones this module doesn't touch; the Transitions frames should pass too."""
    names = names or UNTOUCHED + list(NAMES.values())
    before = _shots(src, work / "before", names)
    after = _shots(out, work / "after", names)
    differ = []
    for name, b in before.items():
        x, y = Image.open(b).convert("RGBA"), Image.open(after[name]).convert("RGBA")
        # Bytes, not ImageChops.difference(x, y).getbbox(): on an RGBA image Pillow 10 looks at alpha alone there.
        if x.size != y.size or x.tobytes() != y.tobytes():
            differ.append(name)
    return differ


def run(src: Path, out: Path, export_dir: Path | None = None, log=None) -> dict:
    d = Doc("Attic Expressive", read_roots=[src.resolve().parent], write_roots=[out.resolve().parent], log=log)
    ctx: dict = {"style": "expressive"}
    try:
        d.tool("open_file", path=str(src.resolve()))
        attach(d, ctx)
        build(d, ctx)
        out.parent.mkdir(parents=True, exist_ok=True)
        d.save(out.resolve())
        if export_dir:
            export(d, ctx, export_dir)
        return ctx
    finally:
        d.close()


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("src", type=Path)
    ap.add_argument("out", type=Path)
    ap.add_argument("--export", type=Path, help="write the changed frames, spec card, Cover and Changelog entry here")
    ap.add_argument("--check", type=Path, help="compare frames of src and out pixel for pixel, working in this folder")
    a = ap.parse_args(argv)
    ctx = run(a.src, a.out, a.export)
    print(f"saved {a.out} ({a.out.stat().st_size // 1024} KB): {len(ctx['landed_frames'])} frames marked ready, "
          f"cover line {ctx.get('landed_cover_line')!r}")
    if a.check:
        differ = check(a.src, a.out, a.check)
        if differ:
            raise SystemExit(f"these frames changed: {differ}")
        print(f"pixel-identical before and after: {len(UNTOUCHED) + len(NAMES)} frames in Light and Dark")


if __name__ == "__main__":
    sys.exit(main())
