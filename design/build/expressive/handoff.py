"""Handoff pass: source annotations, ready-for-dev status, In progress marks, Code Connect, coverage.

Owner: finishing agent. Runs last.

Screens (every key in d.screens, Light and Dark)
- A "Source" annotation naming the Compose source; added from d.screens[key]["source"] when missing.
- status "ready": devStatus readyForDev. status "in-progress": no dev status, and an "In progress" annotation
  (none are expected at ui-redesign 4fe4dab). kind "prototype note" (the Outside Attic frames): no dev status.

Component sets (every COMPONENT_SET on the component pages) and the single main components the modules
registered (not Icon/* or Shape/*, which document a glyph or a shape)
- A description naming the Compose source ("Compose: ..."), added from ctx["components"] when missing.
- A Code Connect link for compose, importPath app.attic.player.ui.expressive, named after the composable.

Dimension tokens: Spec.make (components_core.py) binds each component's gaps, paddings and named sizes as it makes
it, through foundations.bind_dimension_tokens, before anything places an instance (a binding added to a main component
later resets its instances' size and image overrides). The pass here only counts what is left and reports the total.

"Code differs from mock" notes name the mock file they compare with (docs/design/2026-11-21-ui-directions/pixel/).

Writes /tmp/attic-design/expressive-coverage.json (screens and component sets) for design/COVERAGE.md, and
leaves the summary in ctx["handoff_report"].
"""
from __future__ import annotations

import json
import re
import zipfile
from pathlib import Path

from figbuild.kit import Doc
from expressive import CODE_PACKAGE
from expressive.foundations import bind_dimension_tokens

REFS: dict[str, str] = {}

COVERAGE = Path("/tmp/attic-design/expressive-coverage.json")
COMPONENT_PAGES_PREFIX = "Components / "


def _annotations(d: Doc, fid: int) -> list[dict]:
    return d.cmd("annotation.list", ids=[fid]).get("annotations", [])


def _composable(source: str | None) -> str | None:
    """'ui/expressive/Buttons.kt · ExpressiveButton' -> 'ExpressiveButton'."""
    if not source or "·" not in source:
        return None
    name = source.split("·", 1)[1].strip().split()[0].strip(".,;")
    return name if re.match(r"^[A-Z][A-Za-z0-9_]*$", name) else None


def check_screens(d: Doc, ctx: dict, rep: dict) -> dict:
    d.ann_category("Source")
    out = {}
    for key, s in d.screens.items():
        kind = s.get("kind", "screen")
        entry = {"name": s["name"], "page": s["page"], "area": None, "source": s["source"], "status": s["status"],
                 "kind": kind, "light": s.get("light"), "dark": s.get("dark")}
        sec = s.get("section")
        if sec:
            try:
                entry["area"] = d.get(sec).get("name")
            except Exception:
                pass
        counts, ready = {}, {}
        for theme in ("light", "dark"):
            fid = s.get(theme)
            if not fid:
                continue
            anns = _annotations(d, fid)
            cats = [((a.get("category") or {}).get("name")) for a in anns]
            if "Source" not in cats:
                d.annotate(fid, f"Compose: {s['source']}", "Source")
                rep["source_added"].append(f"{key} ({theme})")
            if s["status"] == "in-progress":
                if "In progress" not in cats:
                    d.annotate(fid, "In progress: this feature isn't on ui-redesign yet.", "In progress")
                    rep["in_progress_added"].append(f"{key} ({theme})")
                if d.get(fid, fields=["devStatus"]).get("devStatus"):
                    d.set(fid, devStatus=None)
            elif kind == "screen":
                if d.get(fid, fields=["devStatus"]).get("devStatus") != "readyForDev":
                    d.ready_for_dev(fid)
                    rep["ready_added"].append(f"{key} ({theme})")
            got = d.get(fid, fields=["devStatus"]).get("devStatus")
            ready[theme] = got == "readyForDev"
            counts[theme] = len(_annotations(d, fid))
        entry["annotations"] = counts
        entry["ready"] = all(ready.values()) if ready else False
        out[key] = entry
    return out


SNAPSHOT = Path("/tmp/attic-design/expressive-handoff-snapshot.figcraft")


def snapshot(d: Doc) -> dict:
    """The document's nodes as the saved file stores them: get_node doesn't report a component's description
    or its property definitions, the saved document.json does."""
    d.save(SNAPSHOT)
    try:
        with zipfile.ZipFile(SNAPSHOT) as z:
            doc = json.loads(z.read("document.json"))
    finally:
        SNAPSHOT.unlink(missing_ok=True)
    return {int(k): v for k, v in doc["nodes"].items()}


def _page(nodes: dict, nid: int, page_names: dict) -> str | None:
    seen = 0
    while nid in nodes and seen < 64:
        if nid in page_names:
            return page_names[nid]
        nid = nodes[nid].get("parent")
        seen += 1
    return page_names.get(nid)


def _variant_axes(names: list[str]) -> list[str]:
    axes = []
    for nm in names:
        for part in nm.split(","):
            if "=" in part:
                k = part.split("=", 1)[0].strip()
                if k not in axes:
                    axes.append(k)
    return axes


def check_components(d: Doc, ctx: dict, rep: dict) -> list[dict]:
    known = ctx.get("components", {})
    nodes = snapshot(d)
    page_names = {pid: name for name, pid in d.pages.items()}
    targets = [nid for nid, n in nodes.items() if n.get("type") == "componentSet"]
    for name, cid in d.comp.items():
        n = nodes.get(cid)
        if (n and n.get("type") == "component" and not name.startswith(("Icon/", "Shape/"))
                and nodes.get(n.get("parent"), {}).get("type") != "componentSet"):
            targets.append(cid)
    out = []
    for cid in targets:
        n = nodes[cid]
        name = n["name"]
        page = _page(nodes, cid, page_names)
        info = known.get(name, {})
        desc = n.get("description") or ""
        m = re.search(r"Compose: ?([^\s]+\.kt ?· ?[A-Za-z0-9_]+)", desc)
        source = info.get("source") or (m.group(1) if m else None)
        if "Compose" not in desc and "Code:" not in desc:
            if source:
                desc = (desc.rstrip() + " " if desc else "") + f"Compose: {source}."
                d.set(cid, description=desc)
                rep["description_added"].append(name)
            else:
                rep["no_source"].append(name)
        links = n.get("code_links") or []
        if not any(l.get("language") == "compose" for l in links):
            comp = _composable(source)
            if comp:
                d.cmd("codeConnect.set", id=cid, language="compose", importPath=CODE_PACKAGE, name=comp)
                rep["code_connect_added"].append(f"{name} -> {comp}")
                links = [{"language": "compose", "import_path": CODE_PACKAGE, "name": comp}]
            else:
                rep["no_code_connect"].append(name)
        if n["type"] == "componentSet":
            kids = [nodes[c] for c in n.get("children", []) if nodes.get(c, {}).get("type") == "component"]
            axes = _variant_axes([k["name"] for k in kids])
            first = kids[0] if kids else {}
        else:
            kids, axes, first = [], [], n
        props = list(first.get("prop_order") or (first.get("props") or {}).keys())
        out.append({
            "name": name, "type": "COMPONENT_SET" if n["type"] == "componentSet" else "COMPONENT", "id": cid,
            "page": page, "variants": len(kids) or 1,
            "variant_properties": axes, "properties": [p.split("#")[0] for p in props],
            "source": source,
            "code_connect": [{"language": l.get("language"), "importPath": l.get("import_path"),
                              "name": l.get("name")} for l in links],
        })
    return out


MOCK_DIR = "docs/design/2026-11-21-ui-directions/pixel/"


def mock_file(key: str, source: str | None) -> str:
    """The HTML mock a screen is compared with."""
    src = source or ""
    if key.startswith(("home", "mini_player", "empty_library")):
        return "01-home.html"
    if key.startswith("search"):
        return "02-search.html"
    if key.startswith("library"):
        return "03-library.html"
    if key.startswith(("album", "artist", "playlist", "folder", "favourites", "item_missing")) or "Collections" in src:
        return "04-collections.html"
    if key.startswith(("now_playing", "open_with")):
        return "05-now-playing.html"
    if key.startswith(("queue", "lyrics")):
        return "06-queue-lyrics.html"
    if key.startswith(("sheet_", "dialog_new", "dialog_name", "dialog_rename", "dialog_delete", "menu_", "snackbar",
                       "sleep_timer")):
        return "07-sheets.html"
    if key.startswith("recap"):
        return "09-recap.html"
    if key.startswith(("widget", "system", "splash", "launcher")):
        return "10-system.html"
    return "08-settings-states.html"


def name_mocks(d: Doc, rep: dict):
    for key, s in d.screens.items():
        for theme in ("light", "dark"):
            fid = s.get(theme)
            if not fid:
                continue
            for a in _annotations(d, fid):
                if (a.get("category") or {}).get("name") != "Code differs from mock":
                    continue
                text = a.get("label") or ""
                if MOCK_DIR in text:
                    continue
                path = MOCK_DIR + mock_file(key, s.get("source"))
                if re.search(r"\b[Tt]he mock\b", text):
                    new = re.sub(r"\b([Tt]he mock)\b", lambda m: f"{m.group(1)} ({path})", text, count=1)
                else:
                    new = f"Mock: {path}. {text}"
                d.cmd("annotation.edit", id=fid, index=a["index"], label=new)
                rep["mock_named"].append(f"{key} ({theme})")


def build(d: Doc, ctx: dict):
    rep = {"mock_named": [], "dimension_bindings": {}, "source_added": [], "ready_added": [], "in_progress_added": [], "description_added": [],
           "code_connect_added": [], "no_source": [], "no_code_connect": []}
    name_mocks(d, rep)
    # Spec.make bound each component's dimensions as it made it (foundations.bind_dimension_tokens); this pass only
    # counts what is left unbound, since binding now would reset the instances' overrides.
    left = bind_dimension_tokens(d, ctx)
    total = sum(ctx.get("dimension_bindings", {}).values())
    print(f"    dimension bindings: {total}; token values left unbound: {left} {ctx.get('dimension_unbound', {})}", flush=True)
    rep["dimension_bindings"] = dict(ctx.get("dimension_bindings", {}))
    rep["dimension_binding_total"] = total
    screens = check_screens(d, ctx, rep)
    comps = check_components(d, ctx, rep)
    app_screens = {k: v for k, v in screens.items() if v["kind"] == "screen"}
    summary = {
        "screens": len(app_screens),
        "screen_frames": sum(1 for v in app_screens.values() for t in ("light", "dark") if v[t]),
        "ready": sum(1 for v in app_screens.values() if v["ready"]),
        "in_progress": sum(1 for v in app_screens.values() if v["status"] == "in-progress"),
        "prototype_notes": len(screens) - len(app_screens),
        "component_sets": sum(1 for c in comps if c["type"] == "COMPONENT_SET"),
        "single_components": sum(1 for c in comps if c["type"] == "COMPONENT"),
        "variants": sum(c["variants"] for c in comps),
        "with_code_connect": sum(1 for c in comps if c["code_connect"]),
    }
    data = {
        "file": d.title, "matches": "ui-redesign at 4fe4dab", "summary": summary,
        "flows": ctx.get("flow_report", {}),
        "screens": screens,
        "components": sorted(comps, key=lambda c: (c["page"] or "", c["name"])),
        "handoff_fixes": rep,
    }
    COVERAGE.parent.mkdir(parents=True, exist_ok=True)
    COVERAGE.write_text(json.dumps(data, indent=2, default=str))
    ctx["handoff_report"] = {"summary": summary, **{k: v for k, v in rep.items()}}
    print(f"    handoff: {summary}")
    for k, v in rep.items():
        if isinstance(v, list) and v:
            print(f"      {k}: {len(v)} {v[:8]}")
        elif isinstance(v, dict) and v:
            print(f"      {k}: {v}")
    print(f"    coverage: {COVERAGE}")
