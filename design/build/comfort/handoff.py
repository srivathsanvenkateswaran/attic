"""Handoff pass: source annotations, ready-for-dev status, In progress marks, Code Connect, coverage.

Owner: finishing agent. Runs last.

SCREENS   Every screen frame, Light and Dark, needs a `Source` annotation ("Compose: <file> · <composable>") and
          devStatus readyForDev. A screen with status "in-progress" gets no Ready for dev and an `In progress`
          annotation instead (none at 4fe4dab). Whatever is missing is added and counted.
SETS      Every component set needs a description naming its Compose source (a .kt file) and a Code Connect link
          (compose, app.attic.player.ui.comfort). A missing link is added, named after the composable the
          description names (or Comfort<Name> when it names none); a missing description can't be invented and is
          listed instead.
WORDING   Annotations and component descriptions must not name another company's phone or app. "the One UI mock"
          (the HTML mock sheets the style started from) is rewritten "the HTML mock"; anything else is listed.
COVERAGE  /tmp/attic-design/comfort-coverage.json: every screen (key, name, area, source, Light and Dark ids,
          annotation counts, ready flag) and every component set (name, page, variant count, property names).
          The lead turns it into design/COVERAGE.md. ctx["handoff_report"] holds what this pass changed.
"""
from __future__ import annotations

import json
import re
import zipfile
from pathlib import Path

from figbuild.kit import Doc

from . import CODE_PACKAGE, VERSION

REFS: dict[str, str] = {}

COVERAGE = Path("/tmp/attic-design/comfort-coverage.json")
PROBE = Path("/tmp/attic-design/.comfort-handoff-probe.figcraft")
BANNED = re.compile(r"Spotify|Pixel|One UI|Samsung")
REWRITES = (("the One UI mock", "the HTML mock"), ("The One UI mock", "The HTML mock"), ("One UI mock", "HTML mock"))
SOURCE_FILE = re.compile(r"[\w/]+\.kt")


def _annotations(d: Doc, fid: int) -> list[dict]:
    return d.cmd("annotation.list", ids=[fid]).get("annotations", [])


def _screens(d: Doc, rep: dict) -> list[dict]:
    out = []
    section_names: dict[int, str] = {}
    for key, s in d.screens.items():
        row = {"key": key, "name": s["name"], "source": s["source"], "status": s["status"],
               "light": s["light"], "dark": s["dark"], "page": s["page"]}
        sec = s.get("section")
        if sec is not None and sec not in section_names:
            section_names[sec] = d.get(sec).get("name", "")
        row["area"] = section_names.get(sec, "")
        ready = True
        for theme in ("light", "dark"):
            fid = s[theme]
            if not fid:
                if theme == "dark":
                    rep["missing_dark"].append(key)
                ready = False
                continue
            anns = _annotations(d, fid)
            cats = [(a.get("category") or {}).get("name") for a in anns]
            if s["status"] == "in-progress":
                if "In progress" not in cats:
                    d.annotate(fid, "In progress: this feature isn't on ui-redesign yet; the drawing follows its branch.",
                               "In progress")
                    rep["fixed"].append(f"{key} {theme}: added In progress annotation")
                if d.get(fid).get("devStatus"):
                    d.set(fid, devStatus=None)
                    rep["fixed"].append(f"{key} {theme}: removed Ready for dev (in progress)")
                ready = False
            else:
                if "Source" not in cats:
                    d.annotate(fid, f"Compose: {s['source']}", "Source")
                    rep["fixed"].append(f"{key} {theme}: added Source annotation")
                status = d.tool("get_node", id=fid, fields=["devStatus"]).get("devStatus")
                if status != "readyForDev":
                    d.ready_for_dev(fid)
                    rep["fixed"].append(f"{key} {theme}: set Ready for dev (was {status})")
            row[f"{theme}_annotations"] = len(_annotations(d, fid))
        row["ready"] = ready and all(
            d.tool("get_node", id=s[t], fields=["devStatus"]).get("devStatus") == "readyForDev"
            for t in ("light", "dark") if s[t])
        out.append(row)
    return out


def _descriptions(d: Doc) -> dict[int, str]:
    """Every component's and set's description. get_node leaves out a component set's description (FigCraft
    0.18.1), so read them from a saved copy of the document."""
    d.save(PROBE)
    try:
        doc = json.loads(zipfile.ZipFile(PROBE).read("document.json"))
    finally:
        PROBE.unlink(missing_ok=True)
    out = {}

    def walk(o):
        if isinstance(o, dict):
            if o.get("type") in ("component", "componentSet") and "id" in o:
                out[o["id"]] = o.get("description") or ""
            for v in o.values():
                if isinstance(v, (dict, list)):
                    walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)
    walk(doc)
    return out


def _composable(desc: str, name: str) -> str:
    """The composable a description names after its .kt file ("Buttons.kt ComfortButton"), else Comfort<Name>."""
    m = re.search(r"\.kt[\s·:,(]*([A-Z]\w+)", desc or "")
    if m:
        return m.group(1)
    return "Comfort" + "".join(w.capitalize() for w in re.split(r"[^A-Za-z0-9]+", name) if w)


def _sets(d: Doc, rep: dict, descs: dict[int, str]) -> list[dict]:
    pages = {p["id"]: p["name"] for p in d.cmd("page.list")}
    out = []
    for c in d.cmd("component.list"):
        if c["type"] != "COMPONENT_SET":
            continue
        sid = c["id"]
        g = d.tool("get_node", id=sid, depth=0, fields=["name", "codeLinks", "childCount"])
        desc = descs.get(sid, "")
        if not SOURCE_FILE.search(desc) and "res/" not in desc:
            rep["needs_description"].append(c["name"])
        links = g.get("codeLinks") or []
        ok = any(l.get("language") == "compose" and l.get("importPath") == CODE_PACKAGE for l in links)
        if not ok:
            name = _composable(desc, c["name"])
            d.cmd("codeConnect.set", id=sid, language="compose", importPath=CODE_PACKAGE, name=name)
            rep["fixed"].append(f"set {c['name']}: added Code Connect ({name})")
            links = d.tool("get_node", id=sid, fields=["codeLinks"]).get("codeLinks") or []
        props = d.cmd("component.properties", id=sid).get("properties", [])
        out.append({
            "name": c["name"], "id": sid, "page": pages.get(c["page"], c["page"]),
            "variants": g.get("childCount", 0),
            "variant_properties": [p["name"] for p in props if p["type"] == "variant"],
            "properties": [p["name"] for p in props if p["type"] != "variant"],
            "source": desc.split("\n")[0][:200],
            "code_connect": next((l.get("name") for l in links if l.get("language") == "compose"), None),
        })
    return out


def _wording(d: Doc, rep: dict, descs: dict[int, str]):
    for a in d.cmd("annotation.list", page="all").get("annotations", []):
        label = a.get("label") or ""
        if not BANNED.search(label):
            continue
        new = label
        for old, repl in REWRITES:
            new = new.replace(old, repl)
        if new != label and not BANNED.search(new):
            d.cmd("annotation.edit", id=a["id"], index=a["index"], label=new)
            rep["fixed"].append(f"annotation on {a['name']}: \"One UI mock\" -> \"HTML mock\"")
        else:
            rep["wording"].append(f"annotation on {a['name']}: {label[:120]}")
    names = {c["id"]: c["name"] for c in d.cmd("component.list")}
    for cid, desc in descs.items():
        if BANNED.search(desc):
            rep["wording"].append(f"description of {names.get(cid, cid)}: {desc[:120]}")


MOCKS = "docs/design/2026-11-21-ui-directions/oneui/"
# Screen key prefix -> the HTML mock its "Code differs from mock" notes compare with (first match wins).
MOCK_FILES = [
    (("search",), "02-search.html"), (("library",), "03-library.html"),
    (("album", "artist", "playlist", "folder", "favourites", "item_missing"), "04-collections.html"),
    (("now_playing", "open_with"), "05-now-playing.html"), (("queue", "lyrics"), "06-queue-lyrics.html"),
    (("sheet_", "sleep_timer", "menu_", "snackbar", "dialog_new", "dialog_name", "dialog_rename", "dialog_delete"),
     "07-sheets.html"),
    (("settings", "licence", "dialog_restore", "onboarding", "permission", "loading", "empty_library"),
     "08-settings-states.html"),
    (("recap",), "09-recap.html"), (("system_", "widget_"), "10-system.html"),
    (("home", "mini_player"), "01-home.html"),
]
MOCK_WORD = re.compile(r"\bmock\b|\bsheet 0\d", re.I)


def _mock_file(key: str) -> str | None:
    for prefixes, f in MOCK_FILES:
        if key.startswith(prefixes):
            return MOCKS + f
    return None


def _mock_paths(d: Doc, rep: dict):
    """A note that compares with "the mock" or "sheet 08" names the mock's file, so a reader can open it."""
    for key, s in d.screens.items():
        path = _mock_file(key)
        if not path:
            continue
        for theme in ("light", "dark"):
            fid = s.get(theme)
            if not fid:
                continue
            for a in _annotations(d, fid):
                label = a.get("label") or ""
                if not MOCK_WORD.search(label) or MOCKS in label:
                    continue
                new = re.sub(r"\bsheet 0\d\b", "the mock", label)
                new = new.removeprefix("Code differs from mock: ")
                new = new[:1].upper() + new[1:]
                new = new.rstrip() + ("" if new.rstrip().endswith(".") else ".") + f" Mock: {path}"
                d.cmd("annotation.edit", id=a["id"], index=a["index"], label=new)
                rep["mock_paths"] += 1


def build(d: Doc, ctx: dict):
    rep = {"fixed": [], "needs_description": [], "wording": [], "missing_dark": [], "mock_paths": 0}
    descs = _descriptions(d)
    _wording(d, rep, descs)
    _mock_paths(d, rep)
    screens = _screens(d, rep)
    sets = _sets(d, rep, descs)
    flows = {k: v for k, v in (ctx.get("flows_report") or {}).items() if k != "added"}
    cov = {
        "file": "Attic Comfort", "version": VERSION,
        "summary": {
            "screens": len(screens),
            "screen_frames": sum(1 for s in screens for t in ("light", "dark") if s[t]),
            "ready": sum(1 for s in screens if s["ready"]),
            "in_progress": sum(1 for s in screens if s["status"] == "in-progress"),
            "component_sets": len(sets),
            "variants": sum(s["variants"] for s in sets),
            "prototype_interactions": flows.get("interactions"),
        },
        "screens": screens,
        "component_sets": sets,
        "prototype": flows,
        "handoff": rep,
    }
    COVERAGE.parent.mkdir(parents=True, exist_ok=True)
    COVERAGE.write_text(json.dumps(cov, indent=1, ensure_ascii=False, default=str))
    ctx["handoff_report"] = rep
    print(f"    handoff: {len(rep['fixed'])} fixes, {len(rep['needs_description'])} sets without a source, "
          f"{len(rep['wording'])} wording hits, {rep['mock_paths']} mock paths added; coverage in {COVERAGE}", flush=True)
