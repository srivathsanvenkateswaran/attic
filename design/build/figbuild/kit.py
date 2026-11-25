"""A thin layer over FigCraft's MCP tools for building Attic's design files.

Every helper maps to one documented FigCraft command (docs/mcp.md in storytold/figcraft), so a
file built here is an ordinary FigCraft document: main components, instances, variables, styles,
auto layout and prototype links, nothing generated that the app can't edit by hand afterwards.

Conventions the helpers enforce:
- Colours are variables. `V("Color/surface")` binds a fill or stroke; `fill=` and `color=` take a
  variable name and refuse raw hex unless `allow_hex=True` (only for illustration content such as
  demo covers, and for the variable swatches on Foundations pages).
- Text takes a text style by name (`style="Title/Large"`), created on the Foundations page.
- Every created layer gets a name. Frames without one are refused.
- Repeated UI is `instance(...)` of a main component, never a copy.
"""
from __future__ import annotations

import json
import os
import re
import shutil
from pathlib import Path

from .fc import FC, FigCraftError
from . import icons as _icons

REPO = Path(__file__).resolve().parents[3]
FONTS = REPO / "app/src/main/res/font"
SCREEN_W, SCREEN_H = 411, 914          # 1080 x 2400 px at 2.625 px/dp, the reference renders' size
DENSITY = 2.625


def V(name: str) -> dict:
    """A variable reference for `fill` / `stroke`: {"$var": "Collection/name"}."""
    return {"$var": name}


_WEIGHT_NAMES = {100: "Thin", 200: "ExtraLight", 300: "Light", 400: "Regular", 500: "Medium",
                 600: "SemiBold", 700: "Bold", 800: "ExtraBold", 900: "Black"}

_HEX = re.compile(r"^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")


def _paint(value, allow_hex: bool):
    if value is None or value == "none":
        return "none"
    if isinstance(value, dict):
        return value
    if isinstance(value, str) and _HEX.match(value):
        if not allow_hex:
            raise ValueError(f"raw colour {value}: bind a variable (V('Collection/name')) or pass allow_hex=True")
        return value
    if isinstance(value, str):
        return V(value)
    return value


class Doc:
    """One FigCraft document built in a headless session."""

    def __init__(self, title: str, *, read_roots=(), write_roots=(), extra_fonts=(), log=None):
        # FigCraft may touch only the repo's design/ folder, /tmp and folders a caller adds (the
        # private archive). The app's fonts are staged in /tmp so FigCraft never reads app/.
        design = str(REPO / "design")
        roots_r = [design, "/tmp", *map(str, read_roots)]
        roots_w = [design, "/tmp", *map(str, write_roots)]
        self.fc = FC(read_roots=roots_r, write_roots=roots_w, log=log)
        staged = Path("/tmp/attic-design/fonts")
        staged.mkdir(parents=True, exist_ok=True)
        fonts = []
        for f in sorted(FONTS.glob("*.ttf")):
            dst = staged / f.name
            if not dst.exists() or dst.read_bytes() != f.read_bytes():
                shutil.copyfile(f, dst)
            fonts.append(dst)
        self.families = []
        for f in [*fonts, *map(Path, extra_fonts)]:
            self.families += self.cmd("text.loadFont", path=str(f))["families"]
        self.cmd("file.rename", name=title)
        self.title = title
        self.comp: dict[str, int] = {}        # component or component set name -> id
        self.props: dict[str, list] = {}      # component set name -> its property list
        self.text_styles: dict[str, str] = {}
        self.effect_styles: dict[str, str] = {}
        self.pages: dict[str, int] = {}
        self.screens: dict[str, dict] = {}    # screen key -> {light, dark, name, source, page, ...}
        self.icon_names: set[str] = set()
        self._first_page = True
        self._page_cursor: dict[str, list] = {}
        self.ann_categories: dict[str, str] = {}

    # ----------------------------------------------------------------- editing a saved file
    def open_existing(self, path: str | Path):
        """Open a saved `.figcraft` file in this session and index it by name, so the helpers
        (`instance("Mini player")`, `text(style="Title/Large")`, `page("Screens")`, `icon(...)`)
        work on it as they did while it was generated. Use this to edit a file; `save(path)`
        writes it back. Screens already in the file are not indexed into `self.screens`."""
        self.tool("open_file", path=str(Path(path).resolve()))
        self.cmd("text.refit")
        self.comp.clear()
        for c in self.cmd("component.list"):
            # Variants of a set are listed too; index sets and standalone components by name.
            if c.get("type") == "COMPONENT_SET" or "=" not in c.get("name", ""):
                self.comp.setdefault(c["name"], c["id"])
        self.icon_names = {n for n in self.comp if n.startswith("Icon/")}
        for s in self.cmd("style.list"):
            kind = (s.get("type") or "").lower()
            if kind == "text":
                self.text_styles[s["name"]] = s["id"]
            elif kind == "effect":
                self.effect_styles[s["name"]] = s["id"]
        self.pages = {p["name"]: p["id"] for p in self.cmd("page.list") if not p.get("divider")}
        self._first_page = False
        cats = self.cmd("annotation.categories").get("categories", [])
        self.ann_categories = {c["name"]: c["id"] for c in cats}
        return self

    # ----------------------------------------------------------------- raw access
    def cmd(self, command: str, **params):
        return self.fc.cmd(command, **params)

    def tool(self, _tool: str, **args):
        return self.fc.tool(_tool, **args)

    def set(self, ids, **props):
        ids = ids if isinstance(ids, list) else [ids]
        if props.get("absolute") and any(k in props for k in ("x", "y", "width", "height")):
            # Taking a child out of the flow and placing it in one call gives it zero width.
            self.cmd("node.set", ids=ids, absolute=True)
            props.pop("absolute")
        return self.cmd("node.set", ids=ids, **props)

    def get(self, id, depth=0, **kw):
        return self.tool("get_node", id=id, depth=depth, **kw)

    def find(self, root, **filters) -> list[int]:
        return self.cmd("node.find", root=root, **filters)["ids"]

    def child(self, root, name: str) -> int:
        """The first descendant of `root` named exactly `name` (case-sensitive check after FigCraft's substring match)."""
        for i in self.find(root, name=name, limit=200):
            if self.get(i).get("name") == name:
                return i
        raise FigCraftError(f"no layer named {name!r} under {root}")

    def node(self, type: str, parent=None, *, name: str, x=None, y=None, w=None, h=None, text=None,
             path=None, index=None, **props) -> int:
        if not name:
            raise ValueError("every layer needs a name")
        args = {"type": type, "name": name, "props": props, "select": False}
        for k, v in (("parent", parent), ("x", x), ("y", y), ("width", w), ("height", h),
                     ("text", text), ("path", path), ("index", index)):
            if v is not None:
                args[k] = v
        return self.tool("create_node", **args)["id"]

    # ----------------------------------------------------------------- pages and canvas layout
    def page(self, name: str) -> int:
        """Create (or go to) a page. The document's first page is renamed rather than added."""
        if name in self.pages:
            self.cmd("page.select", id=self.pages[name])
            return self.pages[name]
        if self._first_page:
            pid = self.cmd("page.list")[0]["id"]
            self.cmd("page.rename", id=pid, name=name)
            self._first_page = False
        else:
            pid = self.cmd("page.add", name=name)["id"]
        self.cmd("page.select", id=pid)
        self.pages[name] = pid
        return pid

    def divider(self, name: str = "---"):
        """A page-list divider (a page named with a leading hyphen)."""
        nm = name if name.startswith("-") else "-" + name
        return self.cmd("page.add", name=nm)["id"]

    def section(self, name: str, *, x: float, y: float, w: float, h: float, fill="Canvas/section") -> int:
        # Bind the edge too, so no section keeps FigCraft's default raw grey border.
        return self.node("section", name=name, x=x, y=y, w=w, h=h, fill=_paint(fill, True),
                         stroke=V("Canvas/divider"), strokeWeight=1)

    def label(self, parent, chars: str, *, x=None, y=None, style="Doc/Label", color="Canvas/label", name=None, **kw) -> int:
        """Canvas annotation text (frame titles, notes) outside screens."""
        return self.text(parent, chars, style=style, color=color, name=name or f"Label: {chars[:40]}", x=x, y=y, **kw)

    # ----------------------------------------------------------------- layers
    def frame(self, parent=None, *, name: str, w=None, h=None, x=None, y=None, layout=None, gap=None,
              pad=None, fill=None, stroke=None, stroke_w=None, radius=None, smoothing=None,
              sizing=None, main=None, cross=None, clip=None, allow_hex=False, component=False,
              **extra) -> int:
        """A frame (or a main component when component=True), with auto layout when `layout` is
        'h'/'v'. `sizing` is (H, V) from fixed|hug|fill."""
        props = dict(extra)
        if layout:
            props["layout"] = {"h": "horizontal", "v": "vertical"}.get(layout, layout)
            if gap is not None: props["gap"] = gap
            if pad is not None: props["padding"] = pad
            if main: props["mainAlign"] = main
            if cross: props["crossAlign"] = cross
        if fill is not None: props["fill"] = _paint(fill, allow_hex)
        else: props.setdefault("fills", [])
        if stroke is not None:
            props["stroke"] = _paint(stroke, allow_hex)
            props["strokeWeight"] = stroke_w or 1
        if radius is not None: props["cornerRadius"] = radius
        if smoothing is not None: props["cornerSmoothing"] = smoothing
        if clip is not None: props["clip"] = clip
        if sizing:
            props["sizingH"], props["sizingV"] = sizing
        return self.node("component" if component else "frame", parent, name=name, x=x, y=y, w=w, h=h, **props)

    def rect(self, parent=None, *, name: str, w, h, x=None, y=None, fill=None, radius=None,
             smoothing=None, stroke=None, stroke_w=None, allow_hex=False, **extra) -> int:
        props = dict(extra)
        props["fill"] = _paint(fill, allow_hex) if fill is not None else "none"
        if stroke is not None:
            props["stroke"] = _paint(stroke, allow_hex); props["strokeWeight"] = stroke_w or 1
        if radius is not None: props["cornerRadius"] = radius
        if smoothing is not None: props["cornerSmoothing"] = smoothing
        return self.node("rectangle", parent, name=name, x=x, y=y, w=w, h=h, **props)

    def ellipse(self, parent=None, *, name: str, w, h, x=None, y=None, fill=None, allow_hex=False, **extra) -> int:
        return self.node("ellipse", parent, name=name, x=x, y=y, w=w, h=h, fill=_paint(fill, allow_hex), **extra)

    def vector(self, parent=None, *, name: str, path: str, x=0, y=0, w=None, h=None, fill=None,
               stroke=None, stroke_w=None, allow_hex=False, **extra) -> int:
        props = dict(extra)
        props["fill"] = _paint(fill, allow_hex) if fill is not None else "none"
        if stroke is not None:
            props["stroke"] = _paint(stroke, allow_hex); props["strokeWeight"] = stroke_w or 1
        return self.node("vector", parent, name=name, x=x, y=y, w=w, h=h, path=path, **props)

    def text(self, parent, chars: str, *, style: str, color, name=None, x=None, y=None,
             allow_hex=False, **extra) -> int:
        if style not in self.text_styles:
            raise KeyError(f"text style {style!r} isn't defined; known: {sorted(self.text_styles)}")
        tid = self.node("text", parent, name=name or chars[:40] or "Text", x=x, y=y, text=chars,
                        fill=_paint(color, allow_hex), **extra)
        self.cmd("style.apply", ids=[tid], style=self.text_styles[style])
        return tid

    def image(self, parent, path: str, *, name: str, w, h, x=None, y=None, radius=None,
              smoothing=None, mode="fill") -> int:
        """A rectangle filled with an image file (demo covers, logo renders)."""
        rid = self.rect(parent, name=name, w=w, h=h, x=x, y=y, radius=radius, smoothing=smoothing)
        self.cmd("image.place", path=str(path), target=rid, mode=mode)
        # image.place adds on top of the rectangle's fills; a "none" fill leaves only the image.
        return rid

    # ----------------------------------------------------------------- styles and variables
    def canvas_basics(self):
        """The documentation chrome every Attic file shares: a single-mode `Canvas` collection for
        section fills, labels and component-set borders, and `Doc/*` text styles for page titles,
        notes and spec labels. Run it first, before any page content."""
        self.collection("Canvas", modes=["Value"])
        for n, v, desc in [
            ("section", "#F4F4F5", "Section background on the canvas"),
            ("label", "#3F3F46", "Titles and notes on the canvas"),
            ("label-muted", "#71717A", "Secondary notes on the canvas"),
            ("component-border", "#9747FF", "Dashed border around component sets"),
            ("card", "#FFFFFF", "Spec cards on documentation pages"),
            ("divider", "#E4E4E7", "Rules between documentation rows"),
            ("accent", "#9747FF", "Callouts and flow arrows on documentation pages"),
            ("in-progress", "#F59E0B", "Marks work not merged into the app yet"),
        ]:
            self.variable("Canvas", n, "color", value=v, description=desc)
        fam = "Google Sans Flex"
        self.text_style("Doc/Page title", family=fam, size=96, line=104, weight=600)
        self.text_style("Doc/Section title", family=fam, size=48, line=56, weight=600)
        self.text_style("Doc/Heading", family=fam, size=28, line=36, weight=600)
        self.text_style("Doc/Body", family=fam, size=20, line=30, weight=400)
        self.text_style("Doc/Label", family=fam, size=16, line=22, weight=500)
        self.text_style("Doc/Caption", family=fam, size=13, line=18, weight=400)
        self.text_style("Doc/Code", family="Google Sans Flex", size=14, line=20, weight=400,
                        description="Code paths and token names in notes")

    def collection(self, name: str, modes=("Light", "Dark")) -> str:
        return self.cmd("variable.createCollection", name=name, modes=list(modes))["id"]

    def variable(self, collection: str, name: str, type: str, values=None, value=None,
                 description=None, android=None, scopes=None) -> str:
        args = {"collection": collection, "name": name, "type": type}
        if values is not None: args["values"] = values
        if value is not None: args["value"] = value
        if description: args["description"] = description
        vid = self.cmd("variable.create", **args)["id"]
        upd = {}
        if android: upd["codeSyntax"] = {"ANDROID": android}
        if scopes: upd["scopes"] = scopes
        if upd:
            self.cmd("variable.update", variable=vid, **upd)
        return vid

    def text_style(self, name: str, *, family: str, size: float, line: float | str, weight=None,
                   tracking=None, variations=None, features=None, description=None) -> str:
        # A text style keeps a named instance (fontStyle) and axis values under the engine's own
        # key, `variations`; `fontVariations` is a node.set spelling that style values ignore.
        value = {"fontFamily": family, "fontSize": size, "lineHeight": line}
        if tracking is not None: value["letterSpacing"] = tracking
        axes = dict(variations or {})
        if weight is not None:
            axes.setdefault("wght", weight)
        if "wght" in axes:
            value["fontStyle"] = _WEIGHT_NAMES.get(int(round(axes["wght"] / 100.0)) * 100, "Regular")
        if axes:
            value["variations"] = axes
        if features: value["features"] = features
        args = {"type": "text", "name": name, "value": value}
        if description: args["description"] = description
        sid = self.cmd("style.create", **args)["id"]
        self.text_styles[name] = sid
        return sid

    def effect_style(self, name: str, effects: list, description=None) -> str:
        args = {"type": "effect", "name": name, "value": effects}
        if description: args["description"] = description
        sid = self.cmd("style.create", **args)["id"]
        self.effect_styles[name] = sid
        return sid

    def set_mode(self, ids, collection: str, mode: str | None):
        ids = ids if isinstance(ids, list) else [ids]
        self.cmd("variable.setMode", ids=ids, collection=collection, mode=mode)

    def var_id(self, name: str) -> str:
        """A variable's id from its id, its name, or `Collection/name` (names may contain `/`)."""
        if name.startswith("VariableID:"):
            return name
        if not hasattr(self, "_vars") or name not in self._vars:
            self._vars = {}
            listing = self.cmd("variable.list")
            cols = {c["id"]: c["name"] for c in listing["collections"]}
            for v in listing["variables"]:
                self._vars.setdefault(v["name"], v["id"])
                self._vars[f"{cols[v['collection']]}/{v['name']}"] = v["id"]
        if name not in self._vars:
            raise KeyError(f"no variable {name!r}")
        return self._vars[name]

    def bind(self, ids, field: str, variable: str | None):
        ids = ids if isinstance(ids, list) else [ids]
        self.cmd("variable.bind", ids=ids, field=field,
                 variable=None if variable is None else self.var_id(variable))

    # ----------------------------------------------------------------- components
    def component(self, parent=None, *, name: str, description=None, **frame_kw) -> int:
        cid = self.frame(parent, name=name, component=True, **frame_kw)
        if description:
            self.set(cid, description=description)
        return cid

    def register(self, name: str, cid: int):
        if name in self.comp:
            raise ValueError(f"component {name!r} registered twice")
        self.comp[name] = cid

    def combine(self, name: str, variant_ids: list[int], *, description=None, x=None, y=None,
                layout=True, max_width=1600) -> int:
        """Combine variants (each named `Prop=value, Prop2=value`) into a set called `name`."""
        sid = self.cmd("component.combineVariants", ids=variant_ids)["id"]
        props = {"name": name}
        if description: props["description"] = description
        if x is not None: props["x"] = x
        if y is not None: props["y"] = y
        self.set(sid, **props)
        if layout:
            # Lay the variants out in a wrapping grid so the set reads like a spec sheet.
            self.set(sid, layout="horizontal", wrap=True, gap=24, crossGap=24, padding=24,
                     sizingH="hug", sizingV="hug",
                     stroke=V("Canvas/component-border"), strokeWeight=1, dashes=[6, 4],
                     cornerRadius=12)
            # A hugging row never wraps, so cap wide sets at max_width and let the rows wrap.
            if self.get(sid).get("width", 0) > max_width:
                self.set(sid, sizingH="fixed", width=max_width)
        self.register(name, sid)
        return sid

    def prop(self, owner: int, name: str, type: str, *, layer=None, default=None, preferred=None) -> str:
        args = {"id": owner, "name": name, "type": type}
        if layer is not None: args["layer"] = layer
        if default is not None: args["default"] = default
        if preferred is not None: args["preferred"] = preferred
        r = self.cmd("component.addProperty", **args)
        return r.get("name", name) if isinstance(r, dict) else name

    def bind_prop(self, layer: int, prop: str, field: str):
        self.cmd("component.bindProperty", layer=layer, property=prop, field=field)

    def instance(self, comp: str | int, parent=None, *, name=None, x=None, y=None, props=None,
                 sizing=None, **set_props) -> int:
        cid = self.comp[comp] if isinstance(comp, str) else comp
        args = {"component": cid}
        if parent is not None: args["parent"] = parent
        # Without a position an instance in a plain (non-auto-layout) frame lands anywhere.
        args["x"] = 0 if x is None else x
        args["y"] = 0 if y is None else y
        iid = self.cmd("instance.create", **args)["id"]
        if props:
            self.cmd("instance.setProperties", ids=[iid], properties=props)
        extra = dict(set_props)
        if name: extra["name"] = name
        if sizing: extra["sizingH"], extra["sizingV"] = sizing
        if extra:
            self.set(iid, **extra)
        return iid

    def override(self, inst: int, child_name: str, **props):
        """Override a property on a named layer inside an instance (prefer component properties)."""
        cid = self.child(inst, child_name)
        if "fill" in props: props["fill"] = _paint(props["fill"], props.pop("allow_hex", False))
        self.set(cid, **props)
        return cid

    # ----------------------------------------------------------------- icons
    def build_icons(self, parent, *, color=None, x=0, y=0, per_row=12, filled=(), wght=400,
                    opsz=24, suffix="") -> dict:
        """One main component per Material Symbols glyph the app ships (`Icon/<ligature>`), plus
        `Icon/<ligature> (filled)` for the glyphs in `filled`, laid out in `parent`."""
        made = {}
        specs = [(lig, cp, 0) for _, lig, cp in _icons.symbols()]
        specs += [(lig, cp, 1) for _, lig, cp in _icons.symbols() if lig in set(filled)]
        for lig, cp, fill in specs:
            nm = f"Icon/{lig}" + (" (filled)" if fill else "") + suffix
            c = self.component(parent, name=nm, w=24, h=24,
                               description=f"Material Symbols Rounded `{lig}` (U+{cp:04X}), FILL {fill}, wght {wght}, opsz {opsz}. Compose: Symbol.{_enum(lig)}.")
            self.vector(c, name="Glyph", path=_round(_icons.path(cp, fill=fill, wght=wght, opsz=opsz)),
                        fill=color or self.icon_color,
                        constraints={"horizontal": "scale", "vertical": "scale"})
            self.register(nm, c)
            made[nm] = c
        self.icon_names |= set(made)
        return made

    def icon(self, parent, ligature: str, *, size=24, color=None, filled=False, name=None) -> int:
        nm = f"Icon/{ligature}" + (" (filled)" if filled else "")
        if nm not in self.comp:
            raise KeyError(f"{nm} isn't in the icon set (Symbols.kt). Add the glyph to the app first.")
        iid = self.instance(nm, parent, name=name or f"Icon {ligature}")
        if size != 24:
            self.set(iid, width=size, height=size)
        if color is not None:
            self.override(iid, "Glyph", fill=color)
        return iid

    # ----------------------------------------------------------------- screens
    def area(self, name: str, *, page: str = "Screens", note: str | None = None) -> "Area":
        """A section of screens on a page: Light row on top, Dark copies below (see Area)."""
        return Area(self, name, page=page, note=note)

    def screen(self, key: str, *, name: str, page: str, source: str, x: float, y: float,
               h: float = SCREEN_H, fill=None, note=None, status="ready",
               **frame_kw) -> int:
        """A 411-wide phone frame (Light). Prefer `Area.screen`, which places it for you.
        `status` is "ready" or "in-progress" (feature not merged into the app yet)."""
        if key in self.screens:
            raise ValueError(f"screen {key!r} built twice")
        self.page(page)
        fid = self.frame(None, name=f"{name} · Light", x=x, y=y, w=SCREEN_W, h=h,
                         fill=fill if fill is not None else self.screen_fill,
                         clip=True, **frame_kw)
        self.screens[key] = {"light": fid, "dark": None, "map": {}, "name": name, "source": source,
                             "page": page, "note": note, "status": status, "h": h}
        return fid

    def dark_copy(self, key: str, *, dy: float, collections=None) -> int:
        """Duplicate a Light screen `dy` below and switch its variable modes to Dark. Keeps the
        id map (light layer -> dark layer) so prototype links can be mirrored."""
        s = self.screens[key]
        self.page(s["page"])
        collections = collections or self.mode_collections
        r = self.cmd("edit.duplicate", ids=[s["light"]], dx=0, dy=dy, map=True)
        did = r["ids"][0]
        s["map"] = {int(k): v for k, v in (r.get("map") or {}).items()}
        self.set(did, name=f"{s['name']} · Dark")
        for c in collections:
            self.set_mode(s["light"], c, "Light")
            self.set_mode(did, c, "Dark")
        s["dark"] = did
        return did

    mode_collections: tuple = ("Color",)
    # Per-style defaults; a style package's setup() sets them (Comfort: Color/ground, Color/on).
    screen_fill: str = "Color/surface"
    icon_color: str = "Color/on-surface"

    def link(self, src_key: str, trigger_layer: int | None, dst_key: str | None, *,
             action="navigate", transition="smartAnimate", direction=None, duration=300,
             easing="easeOut", trigger="onClick", overlay=None):
        """A prototype interaction from a layer in screen `src_key` (None: the whole frame) to
        screen `dst_key`, added to the Light screen and mirrored onto its Dark copy.
        action: navigate | back | openOverlay | closeOverlay | swapOverlay."""
        s = self.screens[src_key]
        pairs = [(trigger_layer or s["light"], self.screens[dst_key]["light"] if dst_key else None)]
        if s["dark"]:
            dl = s["map"].get(trigger_layer, s["dark"]) if trigger_layer else s["dark"]
            pairs.append((dl, self.screens[dst_key]["dark"] if dst_key else None))
        for layer, dest in pairs:
            act = {"type": action}
            if dest is not None and action not in ("back", "closeOverlay"):
                act["dest"] = dest
            if overlay:
                act.update(overlay)
            params = {"id": layer, "trigger": {"type": trigger}, "action": act}
            if transition and action not in ("back", "closeOverlay"):
                params["transition"] = {"type": transition, **({"direction": direction} if direction else {})}
                params["durationMs"] = duration
                params["easing"] = easing
            self.cmd("proto.interaction.add", **params)

    def flow_start(self, key: str, name: str, description: str | None = None):
        s = self.screens[key]
        r = self.cmd("proto.flow.add", start=s["light"], name=name)
        return r

    # ----------------------------------------------------------------- handoff
    def ann_category(self, name: str, color=None) -> str:
        if name not in self.ann_categories:
            # Another module may have made it with a raw command; reuse it rather than fail.
            for c in self.cmd("annotation.categories").get("categories", []):
                self.ann_categories.setdefault(c["name"], c["id"])
        if name not in self.ann_categories:
            args = {"name": name}
            if color: args["color"] = color
            self.ann_categories[name] = self.cmd("annotation.addCategory", **args)["id"]
        return self.ann_categories[name]

    def annotate(self, id: int, text: str, category="Source"):
        self.ann_category(category)
        return self.cmd("annotation.add", id=id, text=text, category=category)

    def ready_for_dev(self, ids):
        self.set(ids if isinstance(ids, list) else [ids], devStatus="readyForDev")

    # ----------------------------------------------------------------- output
    def save(self, path: str | Path):
        return self.tool("save_file", path=str(path))

    def export_png(self, id: int, path: str | Path, scale: float = 1.0):
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        return self.tool("export", ids=[id], format="png", scale=scale, path=str(path))

    def export_fig(self, path: str | Path):
        return self.tool("export", format="fig", path=str(path))

    def close(self):
        self.fc.close()


class Area:
    """Screens of one area of the app, laid out left to right in a section on a page.

        a = d.area("Home")
        s = a.screen("home", name="Home", source="ui/expressive/ExpressiveHome.kt · ExpressiveHome")
        ... build into s ...
        a.close()          # Dark copies below the Light row, then the section around both rows

    Areas stack top to bottom on their page in the order they're opened.
    """
    GAP_X, GAP_Y, PAD, TITLE = 80, 160, 120, 96
    _next_y: dict = {}

    def __init__(self, d: Doc, name: str, *, page: str, note: str | None):
        self.d, self.name, self.page, self.note = d, name, page, note
        d.page(page)
        self.x0 = 0
        self.y0 = Area._next_y.get((id(d), page), 0)
        self.cursor = self.x0 + self.PAD
        self.keys: list[str] = []
        self.row_h = 0

    def screen(self, key: str, *, name: str, source: str, h: float = SCREEN_H, w: float = SCREEN_W,
               **kw) -> int:
        x, y = self.cursor, self.y0 + self.PAD + self.TITLE
        fid = self.d.screen(key, name=name, page=self.page, source=source, x=x, y=y, h=h, **kw)
        if w != SCREEN_W:
            self.d.set(fid, width=w)
        self.cursor += w + self.GAP_X
        self.keys.append(key)
        self.row_h = max(self.row_h, h)
        return fid

    def close(self, dark: bool = True) -> int:
        d = self.d
        d.page(self.page)
        w = self.cursor - self.GAP_X + self.PAD - self.x0
        # Title and note first, as page layers, so the screens can move below a long note.
        title = d.text(None, self.name, style="Doc/Section title", color="Canvas/label",
                       name=f"Title: {self.name}", x=self.x0 + self.PAD, y=self.y0 + self.PAD - 24)
        layers = [title]
        top = self.y0 + self.PAD + self.TITLE
        if self.note:
            note = d.text(None, self.note, style="Doc/Body", color="Canvas/label", name="Note",
                          x=self.x0 + self.PAD, y=self.y0 + self.PAD + 32,
                          width=max(SCREEN_W, min(1200, w - 2 * self.PAD)), textAutoResize="height")
            layers.append(note)
            nb = d.get(note)
            need = nb["y"] + nb["height"] + 40
            if need > top:
                shift = need - top
                for k in self.keys:
                    fid = d.screens[k]["light"]
                    d.set(fid, y=d.get(fid)["y"] + shift)
                top = need
        dy = self.row_h + self.GAP_Y
        if dark:
            for k in self.keys:
                d.dark_copy(k, dy=dy)
        rows = 2 if dark else 1
        h = (top - self.y0) + self.row_h * rows + (self.GAP_Y if dark else 0) + self.PAD
        sec = d.section(self.name, x=self.x0, y=self.y0, w=w, h=h)
        members = [d.screens[k]["light"] for k in self.keys] + ([d.screens[k]["dark"] for k in self.keys] if dark else [])
        d.cmd("node.reparent", ids=layers + members, parent=sec)
        for k in self.keys:
            d.screens[k]["section"] = sec
        Area._next_y[(id(d), self.page)] = self.y0 + h + 200
        return sec


def _enum(lig: str) -> str:
    return "".join(p.capitalize() for p in lig.split("_"))


def _round(d: str) -> str:
    return re.sub(r"-?\d+\.\d+", lambda m: f"{float(m.group()):.3f}".rstrip("0").rstrip("."), d)
