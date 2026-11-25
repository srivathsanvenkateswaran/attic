"""Smart transitions are in the code: Attic Comfort's Settings frames stop being a proposal, and the Now Playing
mark is recorded as reviewed and not built (version 1.1.1). Pages: Components / Playback, Screens, Cover, Changelog.

Like `transitions.py` this edits the saved file and rebuilds nothing. It draws nothing either: Settings landed as
1.1.0 drew it, so every frame exports the same pixels before and after. What changes is the handoff.

    cd design/build
    python3 -m comfort.transitions_landed "../Attic Comfort.figcraft" /tmp/attic-design/comfort-landed.figcraft \
        [--export /tmp/attic-dev/smart-transitions-design/landed] [--check /tmp/attic-design/comfort-landed-check]

Look at the result, then copy it back. It refuses a file without the Transitions section and a file whose Settings
frames are already marked ready.

SCREENS (section "Transitions", Light and Dark)
  Settings, off; Settings, Crossfade; Settings, Smart; Transitions pop-over; Crossfade length pop-over: the
  `Proposed` annotations go, each frame is marked ready for development and gains a `Note` saying what the code does
  that the drawing can't show. `Source` names PickerRow, which draws both rows.
  Now Playing, next song marked: not built. The design review judged that the notch says little under the total
  time, where it sits for most songs. The frames stay, still `Proposed` and not ready, with the notes rewritten to
  say so. The section's note and the flow's description follow.

COMPONENTS / PLAYBACK
  Seek bar    Show next song mark and its layer stay. The description, the spec card's note and the `Proposed`
              annotations say the mark was reviewed and not built. NowPlayingParts.kt SeekBar has no mark.

HANDOFF
  Cover 1.1.1; a Changelog entry dated 2026-11-25.

--export writes the 12 frames, the Seek bar spec card, the Cover and the new Changelog entry as PNG. --check exports
frames this module doesn't touch from the source and from the result, and fails unless each pair is pixel-identical.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from PIL import Image, ImageChops

from figbuild.kit import Doc, DENSITY

from . import transitions as T
from .transitions import attach

VERSION = "1.1.1"
UPDATED = "2026-11-25"
NOTE = "Note"
SCREENS = "Screens"

NAMES = {
    "transitions_settings_off": "Transitions / Settings, off",
    "transitions_settings_crossfade": "Transitions / Settings, Crossfade",
    "transitions_settings_smart": "Transitions / Settings, Smart",
    "transitions_picker": "Transitions / Transitions pop-over",
    "transitions_length_picker": "Transitions / Crossfade length pop-over",
    "transitions_now_playing": "Transitions / Now Playing, next song marked",
}
NOT_BUILT = "transitions_now_playing"
SETTINGS_SRC = "ui/comfort/ComfortSettings.kt · ComfortSettings (PickerRow draws Transitions and Crossfade length)"

WHY = "the design review judged that the notch says little under the total time, where it sits for most songs"

# What the code does that a frame can't show, in place of each landed frame's Proposed annotation.
NOTES = {
    "transitions_settings_off": [
        "Off is the default and hides Crossfade length (SettingsState.showsCrossfadeLength). Equaliser is there "
        "only when the phone has an equaliser."],
    "transitions_settings_crossfade": [
        "Crossfade length is there only while the choice is Crossfade or Smart. Its pop-over lists 2 to 12 seconds, "
        "6 to start with."],
    "transitions_settings_smart": [
        "With Smart, Crossfade length gains a summary saying what it is for: songs Smart has no analysis for yet "
        "play a crossfade of this length."],
    "transitions_picker": [
        "ComfortMenu over the Transitions row toward its value (PickerRow's offset: the row's width - 214, up by the "
        "row's height): MenuCaption, then a MenuRow per choice with the current one ticked."],
    "transitions_length_picker": [
        "ComfortMenu over the Crossfade length row, placed as the Transitions pop-over is: 2 to 12 seconds, the "
        "current one ticked."],
}
# The Now Playing frames keep the Proposed category.
NOT_BUILT_NOTES = [
    f"Reviewed and not built: {WHY}. ComfortNowPlaying draws no mark and SeekBar has no parameter for one. This "
    "frame stays as the record of the proposal and isn't ready for development.",
    "What was proposed: a notch in on-3, 3dp wide, rising 7dp from the seek bar's lower edge under the place the "
    "fill's tick has when the next song starts coming in, here at 3:41 of 3:52, drawn only while Smart has a plan "
    "for this song.",
]

SECTION_NOTE = (
    "Smart transitions. Settings > Playback is as the code draws it: the Transitions row offers Off, Crossfade (a "
    "fixed fade) or Smart, which starts the next song where this one winds down, and Crossfade length sits under it "
    "while songs overlap. The Now Playing frame is a proposal that was reviewed and not built: a notch on the seek "
    f"bar where the next song comes in. The feature is described in {T.DOC}.")

FLOW_DESCRIPTION = ("Settings > Playback with transitions off. The Transitions row opens its pop-over; Smart shows "
                    "Crossfade length, whose row opens the lengths.")

SEEK_NOTE = ("Show next song mark was proposed for smart transitions, reviewed and not built: "
             f"{WHY}, so the code's SeekBar draws no mark. Leave it off on screens that match the code. As proposed it "
             "draws a notch 3dp wide that rises 7dp from the track's lower edge (top corners 1.5), under the place the "
             "fill's tick has when the next song starts coming in: its left edge at 64 + (351 - 64) x fraction - 12. "
             "On the track it is on-3; once the fill covers it, the tick's own on-accent at 40 %. Shown at 3:41 of "
             "3:52.")
SEEK_ANNOTATION = ("Next song mark and Show next song mark were proposed for smart transitions "
                   f"({T.DOC}, section 4), reviewed and not built: {WHY}. The seek bar in the app has no mark.")

CHANGES = [
    "The Transitions section now matches the code, except the Now Playing mark, which was not built. Smart "
    "transitions landed in Settings as 1.1.0 drew them (docs/features/smart-transitions.md), so nothing is redrawn.",
    "Screens: the Settings and pop-over states of Transitions (5 states, Light and Dark) lose their Proposed "
    "annotation and are marked ready for development. Their Source names PickerRow in ComfortSettings.kt, and a Note "
    "on each frame says what the code does that the drawing can't show.",
    f"Not built: Now Playing, next song marked. On review {WHY.replace('the design review judged that ', '')}, so "
    "the code draws no mark. The frames stay, with a Proposed annotation that says so, and aren't marked ready for "
    "development.",
    "Seek bar: Show next song mark stays as the record of the proposal. Its description, the spec card's note and "
    "its annotation say it was reviewed and not built.",
]

# Frames this module leaves alone, for --check.
UNTOUCHED = ["Settings / Top", "Now Playing / Playing", "Home / All", "Queue / Album", "Search / Results"]


# ============================================================================== screens
def annotations(d: Doc, node: int) -> list[dict]:
    return sorted(d.cmd("annotation.list", ids=[node]).get("annotations", []), key=lambda a: a["index"])


def drop_proposed(d: Doc, node: int) -> int:
    """Remove a layer's Proposed annotations, last first so the indexes left to remove stay true."""
    gone = [a["index"] for a in annotations(d, node) if (a.get("category") or {}).get("name") == T.PROPOSED]
    for index in reversed(gone):
        d.cmd("annotation.remove", id=node, index=index)
    return len(gone)


def _screens(d: Doc, ctx: dict):
    d.page(SCREENS)             # a flow is found by its start frame on the current page
    frames = {}
    for key, name in NAMES.items():
        for theme in ("Light", "Dark"):
            f = ctx["frames"][f"{name} · {theme}"]
            if not drop_proposed(d, f):
                raise SystemExit(f"transitions_landed: {name} · {theme} has no Proposed annotation")
            if key == NOT_BUILT:
                for n in NOT_BUILT_NOTES:
                    d.annotate(f, n, T.PROPOSED)
            else:
                for a in annotations(d, f):
                    if (a.get("category") or {}).get("name") == "Source":
                        d.cmd("annotation.edit", id=f, index=a["index"], text=f"Compose: {SETTINGS_SRC}")
                for n in NOTES[key]:
                    d.annotate(f, n, NOTE)
                d.ready_for_dev(f)
            frames[(key, theme.lower())] = f
    ctx["landed_frames"] = frames
    # The section's note: the same width, so a change of length only moves its own last line.
    section = next(n["id"] for n in d.get(d.pages[SCREENS], depth=1)["children"]
                   if n.get("type") == "SECTION" and n.get("name") == T.SECTION)
    note = next(n["id"] for n in d.get(section, depth=1)["children"] if n.get("name") == "Note")
    d.set(note, characters=SECTION_NOTE)
    nb = d.get(note)
    top = min(d.get(f)["y"] for f in frames.values())
    if nb["y"] + nb["height"] > top - 24:
        raise SystemExit("transitions_landed: the section's note now runs into the screens")
    start = frames[("transitions_settings_off", "light")]
    d.cmd("proto.flow.rename", start=start, name=T.FLOW[1], description=FLOW_DESCRIPTION)


# ============================================================================== the seek bar's mark
def _seek_bar(d: Doc, ctx: dict):
    """The mark stays in the component as a proposal; its three texts say it was reviewed and not built."""
    page = d.page("Components / Playback")
    sid = d.comp["Seek bar"]
    tops = d.get(page, depth=1)["children"]
    spec = next(c["id"] for c in tops if c["name"] == "Spec: Seek bar")
    before = d.get(spec)
    notes = d.child(spec, "Notes")
    shown = d.get(notes)["characters"]
    cur = T._descriptions(d).get(sid, "")
    if T.SEEK_NOTE not in shown or T.SEEK_NOTE not in cur:
        raise SystemExit("transitions_landed: Seek bar's description or spec note isn't the one 1.1.0 wrote")
    d.set(notes, characters=shown.replace(T.SEEK_NOTE, SEEK_NOTE))
    d.set(sid, description=cur.replace(T.SEEK_NOTE, SEEK_NOTE))
    for n in (sid, spec):
        if not drop_proposed(d, n):
            raise SystemExit("transitions_landed: Seek bar has no Proposed annotation")
        d.annotate(n, SEEK_ANNOTATION, T.PROPOSED)
    # A longer note must not run into the row of cards under it.
    sb = d.get(spec)
    for c in tops:
        if c["name"].startswith("Spec: ") and c["id"] != spec and sb["y"] < c["y"] < sb["y"] + sb["height"] + 40 \
                and c["x"] < sb["x"] + sb["width"]:
            raise SystemExit(f"transitions_landed: Spec: Seek bar now overlaps {c['name']} on Components / Playback")
    ctx["landed_spec"] = spec
    ctx["landed_spec_grew"] = sb["height"] - before["height"]


# ============================================================================== cover and changelog
def _cover(d: Doc, ctx: dict):
    """Version and date on the Cover, and the entry on top of the Changelog."""
    cover = d.child(d.page("Cover"), "Cover")
    for meta, value in (("Meta: Version", VERSION), ("Meta: Last updated", UPDATED)):
        d.set(d.child(d.child(cover, meta), "Value"), characters=value)
    pid = d.page("Changelog")
    tops = {c["name"]: c for c in d.get(pid, depth=1)["children"]}
    entries, howto = tops["Entries"], tops["How to add an entry"]
    i = d.instance("Doc/Changelog entry", entries["id"], name=f"Entry {VERSION}")
    d.cmd("node.reorder", id=i, index=0)
    d.cmd("instance.setProperties", ids=[i], properties={"Version": VERSION, "Date": UPDATED, "Author": "Attic maintainers",
                                                         "Changes": "\n".join(CHANGES)})
    d.set(i, sizingH="fill")
    d.set(d.child(i, "Changes"), listStyle="bulleted", paragraphSpacing=10)
    grown = d.get(entries["id"])["height"] - entries["height"]
    d.set(howto["id"], y=howto["y"] + grown)
    ctx["landed_entry"] = i


# ============================================================================== build
def build(d: Doc, ctx: dict):
    frames = attach(d, ctx)
    first = frames.get(f"{NAMES['transitions_settings_off']} · Light")
    if first is None:
        raise SystemExit("transitions_landed: this file has no Transitions section; run comfort.transitions first")
    if d.get(first).get("devStatus"):
        raise SystemExit("transitions_landed: the Transitions frames are already marked ready")
    _screens(d, ctx)
    _seek_bar(d, ctx)
    _cover(d, ctx)
    d.page("Cover")


def export(d: Doc, ctx: dict, out: Path, style: str = "comfort"):
    """The 12 frames at the renders' density, named as 1.1.0 named them, then the spec card, Cover and entry."""
    d.page(SCREENS)
    for (key, theme), f in ctx["landed_frames"].items():
        d.export_png(f, out / f"{style}-{theme}-{T.EXPORT_NAMES[key]}.png", scale=DENSITY)
    d.page("Components / Playback")
    d.export_png(ctx["landed_spec"], out / f"{style}-component-seek-bar.png", scale=2)
    page = d.page("Cover")
    tops = [n["id"] for n in d.get(page, depth=1).get("children", [])]
    d.tool("export", ids=tops, format="png", scale=0.5, path=str(out / f"{style}-cover.png"))
    d.page("Changelog")
    d.export_png(ctx["landed_entry"], out / f"{style}-changelog-entry.png", scale=1)


def _open(path: Path, write_root: Path) -> Doc:
    d = Doc("Attic Comfort", read_roots=[path.resolve().parent], write_roots=[write_root.resolve()])
    d.tool("open_file", path=str(path.resolve()))
    d.cmd("text.refit")
    return d


def _shots(path: Path, out: Path, names: list[str]) -> dict[str, Path]:
    """Export screen frames of a saved file, Light and Dark, at the renders' density."""
    out.mkdir(parents=True, exist_ok=True)
    d = _open(path, out)
    got = {}
    try:
        frames = attach(d, {})
        d.page(SCREENS)
        for name in names:
            for theme in ("Light", "Dark"):
                p = out / (re.sub(r"[^a-z0-9]+", "-", f"{name} {theme}".lower()).strip("-") + ".png")
                d.export_png(frames[f"{name} · {theme}"], p, scale=DENSITY)
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
        if x.size != y.size or ImageChops.difference(x, y).getbbox():
            differ.append(name)
    return differ


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("src", type=Path)
    ap.add_argument("dst", type=Path)
    ap.add_argument("--export", type=Path, help="write the changed frames, spec card, Cover and Changelog entry here")
    ap.add_argument("--check", type=Path, help="compare frames of src and dst pixel for pixel, working in this folder")
    a = ap.parse_args(argv)
    a.dst.parent.mkdir(parents=True, exist_ok=True)
    d = _open(a.src, a.dst.parent)
    try:
        ctx = {"style": "comfort", "opened": True}
        build(d, ctx)
        d.save(a.dst.resolve())
        ready = sum(1 for (key, _) in ctx["landed_frames"] if key != NOT_BUILT)
        print(f"saved {a.dst} ({a.dst.stat().st_size // 1024} KB): {ready} frames marked ready, "
              f"{len(ctx['landed_frames']) - ready} kept as a proposal, Spec: Seek bar grew {ctx['landed_spec_grew']:g}")
        if a.export:
            export(d, ctx, a.export)
    finally:
        d.close()
    if a.check:
        differ = check(a.src, a.dst, a.check)
        if differ:
            raise SystemExit(f"these frames changed: {differ}")
        print(f"pixel-identical before and after: {len(UNTOUCHED) + len(NAMES)} frames in Light and Dark")


if __name__ == "__main__":
    sys.exit(main())
