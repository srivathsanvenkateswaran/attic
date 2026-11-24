# Building Attic's design files: conventions

These files are generated once, by the Python packages in this folder driving FigCraft's headless
MCP server, and then maintained by hand in FigCraft. The generator exists so the first version is
complete and consistent. Everything it makes must be something a designer could have made by hand:
main components, instances, variables, styles, auto layout, prototype links.

Read this whole file before writing a module.

## Toolchain

- FigCraft CLI 0.18.1: `~/.local/share/figcraft-cli/figcraft-cli` (the desktop app is
  `/Applications/FigCraft.app`). FigCraft's own docs are in `/tmp/figcraft-docs/` (`mcp.md`,
  `cli.md`, `commands-full.txt` lists every command and its parameters). Search them before
  guessing a parameter.
- `figbuild/` is the shared toolkit: `fc.py` (stdio MCP client), `kit.py` (`Doc`, `Area`, `V`),
  `icons.py` (Material Symbols glyphs from the app's font), `compare.py` (side-by-side against a
  reference render), `run.py` (the build runner). Read `kit.py`; it is short.
- Build: `cd design/build && python3 -m figbuild.run <style> --out /tmp/attic-design/<style>-<you>.figcraft [--only a,b] [--export DIR --compare]`.
  Use your own output path so parallel builds don't collide. A full build takes a minute or two.
- Raw commands: `d.cmd("command.id", **params)` and `d.tool("tool_name", **args)`. Every FigCraft
  feature is reachable this way; `kit.py` only wraps the common ones.
- Fonts: the session loads `app/src/main/res/font/*.ttf` (Google Sans Flex, Onest, Material
  Symbols Rounded). Families: `"Google Sans Flex"`, `"Onest"`. Variable axes go in
  `fontVariations` (`{"wght": 500, "wdth": 100, "ROND": 100}`), check which axes a font has with
  `d.cmd("text.fonts", family=..., axes=True)`.

## Package layout and ownership

```
design/build/
  figbuild/          shared toolkit (owned by the lead; ask in your report for changes)
  expressive/        __init__.py (TITLE, MODULES, FOUNDATION_MODULES, REFS, setup), one module per area
  comfort/           same shape
```

A module is a file with `def build(d: Doc, ctx: dict)`. `ctx` carries ids between modules
(`ctx["tokens"]`, component ids are in `d.comp`). Only edit the modules you were given. If you
need a change in someone else's module (a missing component variant, a wrong token), work around
it in your own module only if the workaround is clean, and list the request in your final report.

## Pages, in this order

```
Cover
Changelog
--- Foundations
Colour
Type
Spacing, radii and shapes
Elevation and motion
Icons
--- Components
Components / Actions            buttons, connected button group, chips, toggles, sliders
Components / Lists and cards    list rows, cards, covers, tiles, shelves
Components / Navigation         nav bar or dock, top bars, tabs, mini player, status and gesture bars
Components / Playback           seek and wavy progress, play controls, lyrics lines, queue row
Components / Overlays           sheets, menus, pop-overs, dialogs, toasts
Components / States             empty, loading, permission, onboarding parts
Components / Recap              Recap cards and story chrome
Components / System             widget sizes, notification controls, splash, launcher icon
--- Screens
Screens                         every screen, one section per area, Light row above Dark row
Flow overview                   the user-flow diagram with labelled arrows
System surfaces                 widget on a home screen, notification shade, lock screen, splash, launcher icon
```

Prototype links only work between frames on one page, so every screen lives on the single
`Screens` page, in sections (`d.area(...)`).

## Naming

- Pages: as above, sentence case.
- Components: `Category/Name`, title case words, e.g. `Button/Filled`, `List row/Song`,
  `Cover/Album`. Variant properties are title case with values in title case: `State=Default`,
  `State=Pressed`, `State=Disabled`, `Size=Small|Medium|Large`, `Selected=False|True`,
  `Playing=False|True`. Several: `Size=Medium, State=Default` (comma and space).
- Component properties (text, boolean, instance swap) are named for what they change, in
  sentence case: `Label`, `Show icon`, `Leading icon`, `Title`, `Subtitle`.
- Theme is not a variant. Light and Dark come from variable modes. Every component page carries a
  `Dark preview` frame (mode Dark) holding an instance of each variant, so both modes are visible.
- Layers: every layer has a name that says what it is (`Cover`, `Title`, `Subtitle`, `Trailing
  icon`, `Progress track`). Never `Frame 12`, `Rectangle`, `Group`. `kit.py` refuses unnamed
  layers.
- Screens: `Area / State · Light` and `· Dark`, e.g. `Search / Results · Light`,
  `Library / Albums, grid · Dark`, `Now Playing / Paused, radio · Light`. Keys (for `d.screens`)
  are snake case: `search_results`.
- Variables: kebab case inside slash groups, mirroring the code's names, e.g. `surface/container-high`
  with code syntax `MaterialTheme.colorScheme.surfaceContainerHigh`.
- Text styles: Material role names, `Display/Large` … `Label/Small`, plus style-specific ones
  documented on the Type page.

## Colour, type, effects: tokens only

- Screens and components use variables for every colour (`fill="Color/surface"` or `V(...)`).
  `kit.py` refuses raw hex unless `allow_hex=True`, which is for three cases only: demo-cover
  illustration content, colour swatches that document a raw value on a Foundations page, and
  third-party system chrome that isn't Attic's (e.g. a launcher wallpaper).
- Text uses text styles (`style="Title/Large"`). Override a style only where the code does.
- Shadows use effect styles. Radii come from the radius tokens; bind them with
  `d.bind(id, "cornerRadius", "Dimension/radius-...")` where a token exists.
- The style package's `setup()` sets `d.mode_collections` to the collections that have Light and
  Dark modes, so `Area.close()` switches all of them on the Dark copies.

## Icons

`d.icon(parent, "play_arrow", size=24, color="Color/on-surface", filled=False)` places an instance
of the `Icon/<ligature>` component built from the app's own Material Symbols font. Only the
glyphs in `ui/expressive/Symbols.kt` exist (90 at ui-redesign 4fe4dab); the app can't show any other. Filled variants exist
for the glyphs passed to `build_icons(filled=...)` in the foundations module.

## Content: demo library only

- Public files (Expressive, Comfort) use only the invented demo library in
  `app/src/storeShots/kotlin/app/attic/player/storeshots/DemoLibrary.kt` and `DemoStates.kt`:
  its artists, albums, songs, durations, years, lyrics and counts. Covers are PNGs exported from
  `DemoCovers.kt` into `/tmp/attic-dev/design-refs/covers/` (see that folder's `README.txt` for
  which cover is which album). Place them with `d.image(...)`.
- Never use a real album cover, a real artist, a real song title or real lyrics in a public file.
  The HTML mocks under `docs/design/2026-11-21-ui-directions/` use real Bollywood names: take
  layout from them, never content.
- User-facing text never says "Spotify", "Pixel", "One UI" or "Samsung". Use the BRIEF.md naming
  table (Library, Favourites, Recap, Instant mixes, Songs, …) and the real strings in
  `app/src/main/res/values/strings.xml`. Where code and a mock disagree on copy, the code wins.

## Screens

- Phone frames are 411 x 914 (the reference renders are 1080 x 2400 at 2.625 px per dp). A screen
  that shows a whole scrolled page is taller (`h=`), named `… (full length)`.
- Every screen starts with an instance of the style's status bar component and ends with the
  gesture bar, as the renders show. They sit in the frame as the system draws them (edge to edge).
- Build screens from instances of the components. A screen-only layout frame (a header block, a
  grid of instances) is fine; a copy of a component's layers is not.
- Use `a = d.area("Name", note=...)`, `a.screen(key, name=..., source=...)`, `a.close()`.
  `source` is the Compose file and composable, e.g.
  `"ui/expressive/ExpressiveHome.kt · ExpressiveHome"`.
- Overlays (sheets, dialogs, menus, toasts) are shown in place over a dimmed copy of the screen
  beneath, built from instances, so each overlay state is its own screen frame.
- `status="in-progress"` marks a screen whose feature isn't on `ui-redesign` yet. Its name ends
  `· In progress` and it carries an `In progress` annotation naming the branch. At 4fe4dab every
  feature in scope is merged (first run, backup and restore, sleep timer, Share, Open with, lyrics
  from files and tags, composer and genre in Credits), so nothing should need it; check
  `git log ui-redesign` before using it.

## Handoff, on every screen and component

- `d.annotate(frame, "Compose: ui/expressive/ExpressiveHome.kt · ExpressiveHome", "Source")`.
- Where the code differs from the HTML mock, add a second annotation in category
  `Code differs from mock` saying what the code does. The code wins in the drawing.
- `d.ready_for_dev(frame)` on screens that match merged code.
- Components: `description` names the Compose source; `d.cmd("codeConnect.set", id=set_id,
  language="compose", importPath="app.attic.player.ui.expressive", name="FilledButton")`.

## Checking your work

You are not done until you have looked. After each build:

1. `--export DIR --compare` writes every screen at 2.625x and side-by-side sheets for screens that
   have a reference render. Read the sheets (the Read tool shows images). Fix spacing, colour, type
   size and weight, radius and alignment until the design matches the render. Record the
   difference scores before and after in your report.
2. For screens without a reference render, compare with the HTML mock's layout and the code.
3. Export your component pages too (`d.tool("export", ids=[page_or_frame], format="png", ...)`)
   and look at them.
4. Inspect for hygiene: `d.cmd("node.find", root=page, name="Frame")` should find no
   default-named layers; `inspect_document` with `fields` to check bindings.

## Rules you must not break

- No git commits, no `git stash`, no git config changes. The lead commits.
- Don't run Gradle (one agent owns the reference renders; ask the lead for a missing render).
- Don't touch the Android phone or adb.
- Don't edit app code. Read it as the source of truth.
- Write only inside `design/build/<your modules>`, `/tmp/attic-design/`, and paths the lead named.
- Annotation and note text follows `~/.claude/skills/unslop/references/core-contract.md`: plain,
  specific, no filler, no count-then-list openers.

## FigCraft behaviours to know

- An instance keeps its text, boolean and instance-swap property values when it switches variant,
  even values it never overrode. After `instance.setProperties` switches a variant, set every
  property the screen needs explicitly.
- Text styles store weight as a named instance (`fontStyle`) and other axes under `variations`;
  `d.text_style(weight=..., variations=...)` does both. A partial `style.update` value resets the
  fields it leaves out, so pass the whole style.
- `variable.create` with `{alias}` needs a variable id; `d.var_id("Collection/name")` finds it.
  `d.bind(...)` resolves names itself. `{"$var": name}` in fill and stroke takes names directly.
- Set `absolute` before placing a child: `d.set()` splits the call for you.
- Instances default to x 0, y 0 inside frames without auto layout; pass x and y to place them.
- A component set's wrapping grid is capped at `max_width` (1600) by `d.combine()`.
- Each style's `setup()` sets `d.screen_fill` and `d.icon_color` (Comfort uses `Color/ground` and
  `Color/on`; Expressive's are named in its foundations module).
- Robolectric draws no elevation shadows, so the renders show none; take shadows from the code.
- Bind a main component's fields when you make it. A binding added after instances exist resets
  their size and image overrides.
- A bound field doesn't follow `transform.scale`, so leave fields unbound on anything drawn scaled.
- `variable.bind` accepts an unknown field name without an error: check that a binding took.
- Put a cover image in with `image.replace` on the cover's own image paint. `image.place` with a
  target stacks a second paint, which the `.fig` export can't carry as an override.
- A long text in an auto-layout column can be measured before its style applies; re-set
  `textAutoResize` after `style.apply` if it overlaps the next layer.
- FigCraft keeps a variant's text values when an instance switches to another variant. The First
  run Welcome screen and the gone states both showed the wrong copy because of this.
