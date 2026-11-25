# Attic on large screens

Where Attic stands against Google Play's adaptive app quality guidelines, as of 2026-11-25 on branch
`feature/tablet-layouts`. The layouts themselves are specified in
`docs/design/2026-11-25-tablet/SPEC.md` and described in
`docs/design/2026-11-21-ui-directions/CURRENT-UI.md`, section 2.23.

## The guidelines

Google renamed the large screen app quality guidelines to the **adaptive app quality guidelines**
(<https://developer.android.com/docs/quality-guidelines/adaptive-app-quality>, updated 2026-11-19). The
tiers are now:

| Tier | Name today | Old name | What it asks |
|---|---|---|---|
| 3 | Adaptive ready | Large screen ready | Full screen or full window, never letterboxed; survives configuration changes with its state; works in multi-window; basic keyboard, mouse and stylus |
| 2 | Adaptive optimized | Large screen optimized | Layouts for every window size by window size class, multi-pane where it fits; secondary elements sized for large screens; keyboard, mouse and trackpad support beyond the basics |
| 1 | Adaptive differentiated | Large screen differentiated | Experiences per device: desktop, foldables, stylus, camera and audio, cars, TV, XR, widgets |

Tier 2 includes Tier 3. Google's old item IDs (LS-C1, LS-I5 and so on) map one to one to the new
names (<https://developer.android.com/docs/quality-guidelines/archive/adaptive/id-map-ls-adaptive>).

What Play does with them, from Play's announcement of 18 November 2026: apps that meet Tier 2 or Tier 1
can apply for a large-screen badge through a form linked from the guidelines page; from April 2027
Tier 3 is required to publish or update an app, and every listing needs foldable screenshots
(1:1, 4:3 and 10:16) besides the phone ones; from August 2027 apps must target API 37, which removes the
opt-out from Android 16's rule that large screens ignore orientation and resizability limits.

**Where Attic lands: Tier 2, Adaptive optimized,** with the gaps listed below. It meets Tier 3 in
full.

## The target device

Xiaomi Pad 6: 2880 × 1800 px at 400 dpi, so 1152 × 720 dp sideways (an expanded width) and 720 × 1152
dp upright (a medium width). Its last update is HyperOS 2 on Android 14, so Android 16's rule that
large screens ignore orientation locks never applies to it: before this work it would have shown the
portrait phone layout letterboxed in landscape. Each half of a landscape split screen is about 570 dp
wide, a compact width, and gets the phone layouts.

## Tier 3, Adaptive ready

| Guideline | Status | How |
|---|---|---|
| Config_Changes (LS-C1) | Met | No orientation lock on large screens (`MainActivity.lockPhonesToPortrait` locks only displays whose smaller side is under 600 dp). `android:configChanges` covers orientation, screen size and layout, smallest screen size and keyboard, so rotation and resizing re-lay out in place: the screen, the open overlay, typed text, scroll positions and playback carry over. Saved state (`rememberSaveable`) covers process death. |
| Config_Combinations (LS-C2) | Met | The same path handles any sequence of size changes. |
| Multi-Window_Functionality (LS-M1) | Met | `resizeableActivity="true"`; every width from compact to expanded has a layout. |
| Multi-Resume (LS-M2) | Met | Playback runs in a foreground media service; the player keeps updating while another app has focus. |
| Camera_Preview, Media_Projection (LS-CM1, LS-CM2) | Not applicable | Attic uses neither. |
| Keyboard_Input (LS-I1) | Met | Compose text fields (search, playlist names) take a hardware keyboard; nothing recreates the activity when one connects (`keyboard`, `keyboardHidden` in `configChanges`). |
| Mouse_Trackpad_Basic (LS-I2) | Met | Every clickable is a Compose clickable; lists scroll with the wheel and trackpad. |
| Stylus_Basic, Stylus_Text_Input (LS-S1, LS-S1.1) | Met | Compose's defaults: a stylus taps and scrolls, and handwriting goes into text fields on Android 14. |

## Tier 2, Adaptive optimized

| Guideline | Status | How |
|---|---|---|
| Responsive_adaptive_layouts (LS-U1) | Met | `ui/adaptive/AdaptiveLayout.kt` takes the window size class from `currentWindowAdaptiveInfoV2()`. Medium and expanded windows get a navigation rail, a player bar or floating mini player, grids whose column count follows the width, readable columns of at most 720 dp, list and detail in the Library, two-pane albums, artists and playlists, and Now Playing beside its queue or lyrics (SPEC.md). |
| UI_Secondary_Elements (LS-U2) | Met, one gap | Bottom sheets at most 640 dp wide and dialogs at most 560 dp (one-field dialogs 400 dp), both centred; buttons and text fields keep a fixed width; the rail replaces the bottom bar. Gap: a song's menu is a bottom sheet, not a menu anchored next to the row. |
| Touch_Targets (LS-U3) | Met | Both kits keep targets at 48 dp or more. |
| Drawable_Focus (LS-U4) | Met, one gap | Rows, tiles, buttons and the new rail, dock, player bar and panel switch use ripple indications, which draw focus. Gap: Expressive's Home carousel items and Favourites pill draw no focus state (as on phones). |
| Keyboard_Navigation (LS-I3) | Met | Tab and the arrow keys move through Compose's focus order: the rail or dock first, then the content, then the player bar. |
| Keyboard_Shortcuts (LS-I4) | Met | Select, cut, copy, paste, undo and redo in text fields come from Compose's text fields. |
| Keyboard_Media_Playback (LS-I5) | Met | Space plays and pauses wherever focus is, unless a text field is taking the typing (`MainActivity.dispatchKeyEvent`); a keyboard's media keys reach the media session. Checked on the phone with key events: Space toggled playback, and the media pause key paused it. |
| Keyboard_Send (LS-I6) | Not applicable | Attic is not a communication app. |
| Keyboard_Exit (new) | Met | Esc does what Back does when there is something to close, whichever control has focus: Now Playing, the queue or lyrics, or a screen pushed on a tab; sheets and dialogs close on Esc in their own windows. On a tab's root it passes on, so it clears keyboard focus and never leaves the app (`MainActivity.dispatchKeyEvent`). |
| Context_Menus (LS-I7) | Met, one gap | A right click or secondary tap on a song row opens its menu (`ui/adaptive/Input.kt`). Gap: album, artist and playlist tiles have no right-click menu, as they have no long-press menu on phones. |
| Content_Zoom (LS-I8) | Not applicable | Attic has no zoomable content (no images, maps or documents to zoom); text follows the system font size. |
| Hover_States (LS-I9) | Met | Ripple indications draw the hover state on every clickable that has one. |

## Tier 1, not attempted

Tier 1 is now a set of device experiences. The ones that touch Attic:

- **Camera and audio:** Audio_Background_Playback is already met (foreground media service, a
  persistent notification with controls). Audio_Switcher (picking the output device inside the app)
  is not built.
- **Widgets:** the home-screen widget resizes across three layouts and holds up at tablet cell sizes;
  it has not been checked against the widget quality guidelines.
- **Desktop and foldables:** drag and drop, multiple instances and fold postures are not built.

## Checked how

- JVM renders of every main screen in both styles, light and dark, at 1152 × 720, 720 × 1152 and
  1440 × 900 dp (`scripts/store_shots/tablet_refs.sh`), each compared with its design frame
  (`scripts/store_shots/tablet_compare.py`).
- The owner's phone (Android 16) set to the Pad 6's pixels and density with `wm size 1800x2880` and
  `wm density 400`, upright and sideways, in both styles, with the owner's own library: the rail or
  side dock and the floating mini player upright; the player bar, the Library as list and detail, and
  Now Playing beside Up next and beside Lyrics sideways. Rotating kept the open screen and, after a
  fix found this way, the Library's scroll position; Home kept its scroll position too. Resizing to
  1425 × 1800 px (570 dp, half the Pad in split screen) switched to the phone layouts and back
  without losing the screen. Esc (key event 111) closed Now Playing. Space (key event 62) played
  and paused, after a fix found this way, and the media pause key paused. No crashes.
- Not yet on a real Pad 6: see "What needs the real device".

## What needs the real device

- The density: `adb shell wm density` on the Pad should print 400. If HyperOS ships another default,
  or the owner's mother uses a larger display size, the window classes shift (at 360 dpi the Pad is
  1280 × 800 dp, still expanded sideways and medium upright).
- HyperOS's floating windows and its own split-screen handle: resizing by dragging, and whether
  HyperOS's "full screen display" setting for apps leaves Attic alone now that it doesn't lock.
- The 144 Hz panel: scrolling and the player's animations at that rate.
- The lock screen and notification on HyperOS 2 in landscape, and the widget on HyperOS's launcher.
- A Bluetooth keyboard and mouse: Space, Esc, Tab order, right click and hover on the real device.
