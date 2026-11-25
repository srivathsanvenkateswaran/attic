"""Smart transitions are in the code: Attic Comfort's Settings frames stop being a proposal, and the Now Playing
mark is recorded as reviewed and not built (version 1.3.0). Pages: Components / Playback, Screens, Cover, Changelog.

Like `transitions.py` this edits the saved file and rebuilds nothing. Settings landed as `transitions.py` drew it,
so the Transitions frames export the same pixels before and after. What changes is the handoff, and one frame of
the Equaliser section.

    cd design/build
    python3 -m comfort.transitions_landed /tmp/attic-design/comfort-transitions.figcraft \
        /tmp/attic-design/comfort-landed.figcraft \
        [--export /tmp/attic-dev/smart-transitions-design/landed] [--check /tmp/attic-design/comfort-landed-check]

Look at the result, then copy it over the file in design/. It refuses a file without the Transitions section and a
file whose Settings frames are already marked ready.

SCREENS (section "Transitions", Light and Dark)
  Settings, off; Settings, Crossfade; Settings, Smart; Transitions pop-over; Crossfade length pop-over: the
  `Proposed` annotations go, each frame is marked ready for development and gains a `Note` saying what the code does
  that the drawing can't show. `Source` names PickerRow, which draws both rows.
  Now Playing, next song marked: not built. The design review judged that the notch says little under the total
  time, where it sits for most songs. The frames stay, still `Proposed` and not ready, with the notes rewritten to
  say so. The section's note and the flow's description follow.

SCREENS (section "Equaliser")
  Settings, Playback: 1.2.0 drew the whole Playback card, with the Crossfade row the code had then (6 seconds). The
  code has Transitions and Crossfade length there now, so the row becomes those two, in the same state: Crossfade,
  6 seconds. The Equaliser row, its link to the Settings page and every other frame of the section are untouched.

COMPONENTS / PLAYBACK
  Seek bar    Show next song mark and its layer stay. The description, the spec card's note and the `Proposed`
              annotations say the mark was reviewed and not built. NowPlayingParts.kt SeekBar has no mark.

HANDOFF
  Cover 1.3.0, Matches naming smart transitions; the 1.3.0 Changelog entry `transitions.py` wrote is rewritten to
  say what landed, so the version has one entry, dated 2026-11-25.

--export writes the 12 frames, the redrawn Equaliser frame, the Seek bar spec card, the Cover and the Changelog entry
as PNG. --check exports frames this module doesn't redraw from the source and from the result, and fails unless each
pair is pixel-identical.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from PIL import Image

from figbuild.kit import Doc, DENSITY

from . import transitions as T
from .components_states import setp
from .transitions import attach

VERSION = "1.3.0"
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
# On two lines: one line runs under the Now Playing card beside it.
MATCHES = "ui-redesign at 4fe4dab + the equaliser\nand smart transitions"
EQ_FRAME = "Equaliser / Settings, Playback"
EQ_NOTE_WAS = "This frame draws the group as ComfortSettings.kt has it on feature/per-song-equaliser."
EQ_NOTE = ("This frame draws the group as ComfortSettings.kt has it. Since 1.3.0 that is Transitions and Crossfade "
           "length where the Crossfade row was.")

WHY = "the design review judged that the notch says little under the total time, where it sits for most songs"

# What the code does that a frame can't show, in place of each landed frame's Proposed annotation.
NOTES = {
    "transitions_settings_off": [
        "Off is the default and hides Crossfade length (SettingsState.showsCrossfadeLength). The Equaliser row under "
        "it is EqualiserRow, as the Equaliser section draws it."],
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
    "Smart transitions, drawn and matching the code (docs/features/smart-transitions.md). Settings > Playback's "
    "Crossfade row is now Transitions: Off, Crossfade (a fixed fade) or Smart, which starts the next song where this "
    "one winds down. Crossfade length (2 to 12 seconds, 6 to start with) is a row under it while the choice is "
    "Crossfade or Smart.",
    "Screens: the section Transitions, 6 screens in Light and Dark. Settings at Playback with transitions off, on "
    "Crossfade and on Smart, the Transitions pop-over and the Crossfade length pop-over are marked ready for "
    "development. Their Source names PickerRow in ComfortSettings.kt, and a Note on each frame says what the code "
    "does that the drawing can't show.",
    f"Not built: Now Playing, next song marked. On review {WHY.replace('the design review judged that ', '')}, so "
    "the code draws no mark. The frames stay as the record of the proposal, with a Proposed annotation that says "
    "so, and aren't marked ready for development.",
    "Changed component: Seek bar gains Show next song mark, the notch of that proposal. Its description, the spec "
    "card's note and its annotation say it was reviewed and not built: leave it off on screens that match the code.",
    "Equaliser / Settings, Playback: the Crossfade row is redrawn as Transitions and Crossfade length, in the same "
    "state (Crossfade, 6 seconds). The Equaliser row is as 1.2.0 drew it.",
    "Prototype: the flow Transitions, from Settings at Playback.",
]

# Frames this module doesn't redraw, for --check: the Transitions frames, and these.
UNTOUCHED = ["Settings / Top", "Settings / Top (full length)", "Settings / Backup", "Now Playing / Playing",
             "Home / All", "Queue / Album", "Search / Results", "Equaliser / Settings page", "Equaliser / Settings page, off",
             "Equaliser / Sheet, default", "Equaliser / Sheet, this song", "Equaliser / Now Playing, this song",
             "Equaliser / Track menu"]


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


# ============================================================================== the equaliser's Settings frame
def _equaliser_frame(d: Doc, ctx: dict):
    """Equaliser / Settings, Playback draws the whole Playback card. Its Crossfade row (6 seconds) becomes the two
    rows the code has now, in the same state: Transitions on Crossfade, then Crossfade length at 6 seconds."""
    d.page(SCREENS)
    mode, length = T.S["crossfade"], T.seconds(T.LENGTH)
    frames = {}
    for theme in ("Light", "Dark"):
        f = ctx["frames"][f"{EQ_FRAME} · {theme}"]
        card = d.child(f, "Playback")
        slot = d.child(card, "Content")
        rows = d.get(slot, depth=1)["children"]
        names = [r["name"] for r in rows]
        if "Crossfade" not in names or d.get(d.child(rows[names.index("Crossfade")]["id"], "Value"))["characters"] != length:
            raise SystemExit(f"transitions_landed: {EQ_FRAME} · {theme} has no Crossfade row at {length}")
        at = names.index("Crossfade")
        old = rows[at]["id"]
        setp(d, old, Title=T.S["transitions"], Value=mode, Summary=T.summary(mode), **{"Show summary": True})
        d.set(old, name="Transitions")
        slot_prop = next(p["name"] for p in d.cmd("component.properties", id=card)["properties"] if p["type"] == "slot")
        r = d.cmd("instance.slotInsert", id=card, slot=slot_prop, component=d.comp["Settings row"], index=at + 1)["id"]
        d.set(r, name="Crossfade length", sizingH="fill")
        setp(d, r, Control="Value", Title=T.S["length"], Value=length, **{"Show summary": False, "Show hairline": True})
        got = [k["name"] for k in d.get(slot, depth=1)["children"]]
        if got != names[:at] + ["Transitions", "Crossfade length"] + names[at + 1:]:
            raise SystemExit(f"transitions_landed: {EQ_FRAME} · {theme}'s rows came out as {got}")
        # 1.2.0's note on where the group comes from named the branch the frame was drawn from.
        for a in annotations(d, f):
            if EQ_NOTE_WAS in a.get("label", ""):
                d.cmd("annotation.edit", id=f, index=a["index"], text=a["label"].replace(EQ_NOTE_WAS, EQ_NOTE))
        d.ready_for_dev(f)          # marked again, so the frame doesn't show as edited since it was marked
        frames[theme.lower()] = f
    ctx["landed_equaliser_frames"] = frames


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
        raise SystemExit("transitions_landed: Seek bar's description or spec note isn't the one transitions.py wrote")
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
    """Version, date and what the file matches on the Cover, and the version's Changelog entry rewritten to say what
    landed. `transitions.py` wrote the entry as a proposal; the two steps are one version."""
    cover = d.child(d.page("Cover"), "Cover")
    for meta, value in (("Meta: Version", VERSION), ("Meta: Last updated", UPDATED), ("Meta: Matches", MATCHES)):
        d.set(d.child(d.child(cover, meta), "Value"), characters=value)
    pid = d.page("Changelog")
    tops = {c["name"]: c for c in d.get(pid, depth=1)["children"]}
    entries, howto = tops["Entries"], tops["How to add an entry"]
    top = d.get(entries["id"], depth=1)["children"][0]
    if top["name"] != T.ENTRY:
        raise SystemExit(f"transitions_landed: the top Changelog entry is {top['name']!r}, not {T.ENTRY!r}")
    i = top["id"]
    d.cmd("instance.setProperties", ids=[i], properties={"Version": VERSION, "Date": UPDATED, "Author": "Attic maintainers",
                                                         "Changes": "\n".join(CHANGES)})
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
    _equaliser_frame(d, ctx)
    _seek_bar(d, ctx)
    _cover(d, ctx)
    d.page("Cover")


def export(d: Doc, ctx: dict, out: Path, style: str = "comfort"):
    """The 12 frames at the renders' density, named as `transitions.py` names them, the redrawn Equaliser frame, then
    the spec card, Cover and entry."""
    d.page(SCREENS)
    for (key, theme), f in ctx["landed_frames"].items():
        d.export_png(f, out / f"{style}-{theme}-{T.EXPORT_NAMES[key]}.png", scale=DENSITY)
    for theme, f in ctx["landed_equaliser_frames"].items():
        d.export_png(f, out / f"{style}-{theme}-equaliser-settings-playback.png", scale=DENSITY)
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
        # Bytes, not ImageChops.difference(x, y).getbbox(): on an RGBA image Pillow 10 looks at alpha alone there.
        if x.size != y.size or x.tobytes() != y.tobytes():
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
