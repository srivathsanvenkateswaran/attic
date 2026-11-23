# Attic design references

Rendered on the JVM by `scripts/store_shots/design_refs.sh` (classes in
`app/src/storeShots/kotlin/app/attic/player/designrefs/`), from the store shots' demo library, with
the same 1080x2400 phone, status bar and gesture bar. {{COUNT}} images, {{SIZE}} in total.

File names are `<style>-<theme>-<key>.png`, style `expressive` or `comfort`, theme `light` or `dark`.
Sheets, dialogs and menus are drawn over the screen they open from. Every Now Playing, queue,
lyrics and mini player shows "Lights Over the Water" (Paper Lanterns, cover 0) at 1:18.

Other folders: `covers/` (demo covers and README.txt), `brand/` (the Headroom mark and icon
sources), `colors/` (colour JSON and comfort-tinted-rules.md).

## Keys rendered

{{TABLE}}

Sizes: 1080x2400 unless the table says otherwise. `*_full` are whole pages (the page down to its last
row, then the dock), `recap_share_*` are the 1080x1920 images Save and Share write, `widget_*` are
the home-screen widget at launcher size with 16dp of plain ground around it.

## What some keys mean

- `home_albums`, `home_artists`, `home_folders`: Home with that filter chip on.
- `search_typing`: the field focused with "Ka" typed and the keyboard up. The keyboard is a plain
  stand-in drawn by the harness (300dp), since Attic does not draw it; the layout around it is real.
- `search_results` is "ka" with the keyboard down; `search_no_results` is "lullaby".
- `library_songs` is the Songs chip (`LibraryFilter.Downloaded` in code). `library_list` is no chip,
  list view; `library_grid` the same as a grid.
- `album_collapsed`: the album scrolled 1400px, so the header has collapsed into the top bar.
- `item_missing`: an album that has gone from the phone ("This album is gone").
- `now_playing_full`: the whole Now Playing page, player then lyrics card, About the artist and
  Credits (with composer and genre: the demo song is tagged "Mira Okafor, Idris Bello" and "Indie
  folk" here). The page is sized to the screen, so it is stitched from frames scrolled by exact
  amounts; the compact header that appears on scrolling is not in the stitched image.
- `now_playing_paused_radio`: paused, radio context ("Radio · Lights Over the Water"), shuffle on,
  repeat one.
- `open_with` and `sheet_track_menu_open_with`: a voice note opened from a file manager that is not
  in the library: no cover, no artist, no lyrics, and the shorter menu. `mini_player_open_with` is the
  same song in the mini player.
- `lyrics_none` is "no lyrics found"; also `lyrics_not_downloaded`, `lyrics_instrumental`,
  `lyrics_downloading`.
- `sheet_track_menu` opens from a Home row; `sheet_track_menu_playlist` from a playlist row (adds
  "Remove from this playlist"); `sheet_track_menu_now_playing` from the Now Playing kebab (adds Sleep
  timer, and takes the cover's scheme in Expressive and the playing song's wash in Comfort).
- `sheet_sleep_timer`: the timer's choices. Expressive replaces the menu with a sheet; Comfort opens a
  pop-over on the menu row. `sleep_timer_active` is the Now Playing menu with a 30-minute timer
  running ("23 min left"). Attic shows the running timer nowhere else.
- `sheet_language_picker`: Expressive replaces the menu with a sheet; Comfort opens a pop-over on the
  "Language" row.
- `dialog_new_playlist` is opened from the playlist picker's "New playlist" (the picker stays under
  it); `dialog_name_playlist` is Library's "+" ("Name this playlist", with "Road trip" typed).
- `menu_playlist_overflow`: the playlist kebab's Rename / Delete playlist menu.
- `snackbar` is a message over Home ("Added to queue"); `snackbar_over_player` over Now Playing.
- `settings_analysis` (song analysis running, scrolled to it) and `settings_lyrics_download` (bulk
  lyrics download running).
- `settings_backup`: Settings scrolled to the Backup section. `dialog_restore_confirm` ("Restore this
  backup?" for a backup with 42 favourites, 3 playlists, 4,870 plays and 51 lyrics),
  `dialog_restore_result` ("Backup restored": 38 favourites, 2 playlists added and 1 merged, 4,312
  plays, 47 lyrics; 64 songs matched, 5 missing) and `dialog_restore_problem` ("Couldn't restore":
  not an Attic backup) are drawn over it.
- `onboarding_*`: the first run. `onboarding_welcome`, `onboarding_look` (style and theme), then
  Access: `onboarding_access` (nothing asked), `onboarding_access_granted` (music and notifications
  allowed), `onboarding_access_refused` (music refused, notifications not asked), then Library:
  `onboarding_library_scanning`, `onboarding_library_found` (69 songs), `onboarding_library_empty`.
  The look step's style and theme are the render's own.
- `recap_story_<card>`: the story opened on each card, over the Recap page.
- `permission` is the first ask; `permission_refused` is after the system dialog was declined.
- `empty_library`: access granted, no music: Home with nothing queued, so no mini player.
- `mini_player_paused`: Home with the mini player paused.
- `widget_small|wide|large`: the widget built by `widget/PlayerWidget.kt`'s own code (called by
  reflection) and applied as a launcher would; `widget_large_empty` is before anything has played.

## Not rendered, and why

| key | why |
|---|---|
| `share` | Attic draws nothing for song Share: it hands the file straight to the Android share sheet (ui/SongShare.kt). The Share row is in `sheet_track_menu`. For Recap, `recap_share_<card>` is the image that is shared. |
| `library_folders` | Library has no Folders chip (`LibraryFilter` is Playlists, Albums, Artists, Songs). Folders are listed under Playlists (`library_playlists`) and in the default list. Home has one: `home_folders`. |
| `appearance` | Not a separate screen: Appearance is the first group in Settings (`settings`). |
| `comfort-*-recap_story_summary`, `comfort-*-recap_share_summary` (skipped in `Ref.of`) | The Comfort Summary card throws while measuring: `SummaryCard` puts `SummaryStat` (whose `FitText` is a `BoxWithConstraints`) in a `Row(Modifier.height(IntrinsicSize.Min))`, and a SubcomposeLayout cannot answer intrinsic measurements (`IllegalStateException`, ui/comfort/ComfortRecapCards.kt around line 640). This looks like a real crash on the phone when the Comfort story reaches its last card, or on Save/Share of it, not a harness problem. Expressive's Summary renders. |
| `licence_text` other notices | Only Attic's own licence (GPL v3) is rendered; the other notices use the same screen. |

## Known differences from the phone

- Robolectric does not draw elevation shadows, so floating pieces (menus, the Comfort dock and toast,
  sheets) have no shadow. The Expressive playlist menu, whose container matches the header behind it,
  therefore shows no visible edge.
- The Expressive widget rounds its art with the views' outlines, which Robolectric's software drawing
  does not clip, so those covers have square corners here.
- Motion is frozen: each render is 3 seconds (sheets and dialogs about 6) after the screen appears.
  Spinners (`loading`, `lyrics_downloading`) show one frame.
- The icon font now includes `file_save` and `file_open` (Settings, Backup); they render as glyphs here.
- The Expressive app scheme is the phone's wallpaper colour scheme. These renders use what Robolectric
  returns for it (see colors/expressive-base.json); on a phone every untinted Expressive surface follows
  the wallpaper.
