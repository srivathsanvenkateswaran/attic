"""Build one design file from its style package.

    cd design/build
    python3 -m figbuild.run expressive --out /tmp/attic-design/expressive.figcraft \
        [--only foundations,components,screens_home] [--export /tmp/attic-design/expressive] \
        [--compare] [--fig /tmp/attic-design/expressive.fig]

A style package (`expressive/`, `comfort/`) has `__init__.py` with TITLE, MODULES (module names in
build order) and optionally REFS ({screen key: reference stem}, see below). Each module defines
`build(d: Doc, ctx: dict)`. `--only` keeps the listed modules plus every module before the first
of them that is marked as a dependency (FOUNDATION_MODULES), so a screen module can be built
alone on top of the foundations and components it uses.

`--export DIR` writes every screen as DIR/<key>-light.png and DIR/<key>-dark.png at 2.625x (the
reference renders' density). `--compare` then writes DIR/compare/<key>-<theme>.png side by side
with the reference render REFS names: a stem like "expressive-{theme}-home" is looked up in the
reference folders (REF_DIRS) with {theme} = light | dark.
"""
from __future__ import annotations

import argparse
import importlib
import sys
import time
from pathlib import Path

from .kit import Doc, DENSITY
from .compare import compare

REF_DIRS = [Path("/tmp/attic-dev/design-refs"), Path("/tmp/attic-dev/store-shots/raw")]


def find_ref(stem: str) -> Path | None:
    for d in REF_DIRS:
        for p in [d / f"{stem}.png", *d.glob(f"**/{stem}.png")]:
            if p.exists():
                return p
    return None


def build(style: str, out: Path, only=None, export=None, do_compare=False, fig=None, log=None):
    pkg = importlib.import_module(style)
    mods = list(pkg.MODULES)
    keep = set(getattr(pkg, "FOUNDATION_MODULES", []))
    if only:
        wanted = set(only)
        unknown = wanted - set(mods)
        if unknown:
            raise SystemExit(f"unknown modules: {sorted(unknown)}; modules are {mods}")
        mods = [m for m in mods if m in wanted or m in keep]
    d = Doc(pkg.TITLE, **getattr(pkg, "DOC_ARGS", {}), log=log)
    ctx: dict = {"style": style}
    t0 = time.time()
    try:
        if hasattr(pkg, "setup"):
            pkg.setup(d, ctx)
        refs = dict(getattr(pkg, "REFS", {}))
        for m in mods:
            t = time.time()
            mod = importlib.import_module(f"{style}.{m}")
            refs.update(getattr(mod, "REFS", {}))
            mod.build(d, ctx)
            print(f"  {m:28s} {time.time() - t:6.1f}s", flush=True)
        out.parent.mkdir(parents=True, exist_ok=True)
        d.save(out)
        print(f"saved {out} ({out.stat().st_size // 1024} KB) in {time.time() - t0:.0f}s", flush=True)
        if export:
            export = Path(export)
            for key, s in d.screens.items():
                for theme in ("light", "dark"):
                    if not s.get(theme):
                        continue
                    png = export / f"{key}-{theme}.png"
                    d.export_png(s[theme], png, scale=DENSITY)
                    if do_compare and key in refs:
                        ref = find_ref(refs[key].format(theme=theme))
                        if ref:
                            score = compare(ref, png, export / "compare" / f"{key}-{theme}.png", f"{key} {theme}")
                            print(f"    compare {key}-{theme}: {score:.1f}")
            print(f"exported {len(d.screens)} screens to {export}", flush=True)
        if fig:
            r = d.export_fig(fig)
            for w in r.get("warnings", []):
                print(f"fig warning: {w}")
        return d, ctx
    finally:
        d.close()


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("style")
    ap.add_argument("--out", required=True, type=Path)
    ap.add_argument("--only")
    ap.add_argument("--export")
    ap.add_argument("--compare", action="store_true")
    ap.add_argument("--fig")
    a = ap.parse_args(argv)
    build(a.style, a.out, only=a.only.split(",") if a.only else None, export=a.export,
          do_compare=a.compare, fig=a.fig)


if __name__ == "__main__":
    sys.exit(main())
