# The generator behind design files 1.0.0

These Python scripts produced version 1.0.0 of `Attic Expressive.figcraft` and
`Attic Comfort.figcraft` from the app's code. They drive FigCraft's own headless automation
(`figcraft-cli mcp`), so everything they made is ordinary FigCraft content: variables, styles,
main components and instances, auto layout, prototype links and annotations.

**The `.figcraft` files are now the source.** Edit them in FigCraft. Running the generator again
rebuilds the files from scratch and would replace every hand edit. Use it only for a deliberate
regeneration, for example after a restyle, and then re-apply or re-check the hand edits.

## Layout

- `figbuild/` is the shared toolkit:
  - `fc.py`: a stdio client for FigCraft's MCP server.
  - `kit.py`: `Doc`, `Area` and `V`, which wrap the FigCraft commands the modules use.
  - `icons.py`: Material Symbols glyphs from the app's font.
  - `pathfit.py`: fits shape outlines with Béziers.
  - `run.py`: builds a file.
  - `compare.py`: compares a design frame with a reference render.
  - `pages.py`: renders pages to PNG.
  - `figexport.py`: the experimental `.fig` export.
  - `coverage.py`: writes `../COVERAGE.md`.
- `expressive/` and `comfort/`: one module per part of the file, built in the order `MODULES` lists in each `__init__.py`. Each module's docstring lists the variables, styles and components it creates.
- `CONVENTIONS.md`: the rules the modules follow (pages, naming, tokens, screens, handoff) and the FigCraft behaviours found while building.

## Building

You need Python 3.9+ with Pillow and fontTools, and the FigCraft CLI 0.18.1
(`figcraft-cli-0.18.1-macos-universal.zip` from the FigCraft release, installed at
`~/.local/share/figcraft-cli/`, or set the path in `figbuild/fc.py`). You also need the reference
renders and demo covers, which `scripts/store_shots/design_refs.sh` writes into
`/tmp/attic-dev/design-refs/`.

```sh
cd design/build
python3 -m figbuild.run comfort --out /tmp/attic-comfort.figcraft --export /tmp/attic-comfort --compare
```

`--only module,module` builds part of a file on top of its foundations and components. `--export`
writes every screen at 2.625×, the renders' density, and `--compare` writes a side-by-side sheet
and a score for each screen that has a render. The handoff module writes
`/tmp/attic-design/<style>-coverage.json`. Then `python3 -m figbuild.coverage
/tmp/attic-design/expressive-coverage.json /tmp/attic-design/comfort-coverage.json` rewrites
`../COVERAGE.md`.

FigCraft only gets access to `design/` and `/tmp`: the server runs with those as its automation
read and write roots, and the app's fonts are copied into `/tmp/attic-design/fonts/` for it.
