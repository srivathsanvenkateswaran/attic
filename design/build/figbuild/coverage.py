"""Write design/COVERAGE.md: every screen and state in CURRENT-UI.md's map, checked against both files.

    python3 -m figbuild.coverage /tmp/attic-design/expressive-coverage.json /tmp/attic-design/comfort-coverage.json

The coverage JSON files are written by each style's handoff module on every build. The checklist
below follows CURRENT-UI.md sections 1 and 2. A row is ticked for a file when any of its keys is a
screen in that file.
"""
from __future__ import annotations

import json
import sys
from datetime import date
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]

CHECKLIST = [
    ("Launch and first run", [
        ("First run: Welcome", ["onboarding_welcome"]),
        ("First run: Pick your look", ["onboarding_look"]),
        ("First run: Access, not asked", ["onboarding_access"]),
        ("First run: Access, allowed", ["onboarding_access_granted"]),
        ("First run: Access, refused", ["onboarding_access_refused"]),
        ("First run: Your library, scanning", ["onboarding_library_scanning"]),
        ("First run: Your library, songs found", ["onboarding_library_found"]),
        ("First run: Your library, no songs", ["onboarding_library_empty"]),
        ("Permission screen, not asked", ["permission"]),
        ("Permission screen, refused", ["permission_refused"]),
        ("Loading", ["loading"]),
        ("Empty library", ["empty_library"]),
        ("Splash", ["splash", "system_splash"]),
    ]),
    ("Home", [
        ("Home, All", ["home"]),
        ("Home, full length", ["home_full"]),
        ("Home, Albums filter", ["home_albums"]),
        ("Home, Artists filter", ["home_artists"]),
        ("Home, Folders filter", ["home_folders"]),
        ("Home, Recap card in late December", ["home_recap_year"]),
        ("Mini player paused", ["mini_player_paused"]),
        ("Mini player for a file from another app", ["mini_player_open_with"]),
    ]),
    ("Search", [
        ("Browse", ["search_browse"]),
        ("Typing", ["search_typing"]),
        ("Results", ["search_results"]),
        ("No results", ["search_no_results"]),
    ]),
    ("Library", [
        ("All, list", ["library_list"]),
        ("All, grid", ["library_grid"]),
        ("Playlists", ["library_playlists"]),
        ("Albums", ["library_albums"]),
        ("Artists", ["library_artists"]),
        ("Songs, with sort and Shuffle all", ["library_songs"]),
    ]),
    ("Collections", [
        ("Album", ["album"]),
        ("Album, scrolled with the bar collapsed", ["album_collapsed"]),
        ("Artist", ["artist"]),
        ("Playlist", ["playlist"]),
        ("Playlist, reordering", ["playlist_reorder"]),
        ("Playlist, empty", ["playlist_empty"]),
        ("Folder", ["folder"]),
        ("Favourites", ["favourites"]),
        ("Favourites, empty", ["favourites_empty"]),
        ("Album is gone", ["item_missing"]),
        ("Artist is gone", ["item_missing_artist"]),
        ("Playlist is gone", ["item_missing_playlist"]),
    ]),
    ("Now Playing, queue and lyrics", [
        ("Now Playing", ["now_playing"]),
        ("Now Playing, full length (lyrics card, artist, Credits with composer and genre)", ["now_playing_full"]),
        ("Now Playing, paused, radio, shuffle, repeat one", ["now_playing_paused_radio"]),
        ("Now Playing for a file opened from another app (Open with)", ["open_with"]),
        ("Queue", ["queue"]),
        ("Queue during radio", ["queue_radio"]),
        ("Queue with nothing up next", ["queue_empty"]),
        ("Lyrics, synced", ["lyrics_synced"]),
        ("Lyrics, plain", ["lyrics_plain"]),
        ("Lyrics, none found", ["lyrics_none"]),
        ("Lyrics, not downloaded", ["lyrics_not_downloaded"]),
        ("Lyrics, instrumental", ["lyrics_instrumental"]),
        ("Lyrics, downloading", ["lyrics_downloading"]),
    ]),
    ("Sheets, dialogs, menus and messages", [
        ("Track menu", ["sheet_track_menu"]),
        ("Track menu in a playlist", ["sheet_track_menu_playlist"]),
        ("Track menu from Now Playing (with Sleep timer)", ["sheet_track_menu_now_playing"]),
        ("Track menu for a file from another app", ["sheet_track_menu_open_with"]),
        ("Sleep timer choices", ["sheet_sleep_timer"]),
        ("Sleep timer running", ["sleep_timer_active", "sheet_sleep_timer_running"]),
        ("Share (track menu row; Android's share sheet)", ["sheet_track_menu", "handoff_share"]),
        ("Playlist picker", ["sheet_playlist_picker"]),
        ("Song language picker", ["sheet_language_picker"]),
        ("Add songs", ["sheet_add_songs"]),
        ("Playlist name: create, new, rename", ["dialog_new_playlist", "dialog_name_playlist", "dialog_rename_playlist"]),
        ("Delete playlist", ["dialog_delete_playlist"]),
        ("Playlist overflow menu", ["menu_playlist_overflow"]),
        ("Message (snackbar)", ["snackbar"]),
        ("Message over the player", ["snackbar_over_player"]),
        ("Message: sleep timer set", ["snackbar_sleep_timer", "snackbar_sleep_timer_set"]),
        ("Message: sleep timer off", ["snackbar_sleep_timer_off"]),
        ("Message: couldn't share", ["snackbar_share_failed"]),
        ("Message: couldn't open a file", ["snackbar_open_failed"]),
    ]),
    ("Settings", [
        ("Settings, with Appearance (style switch and theme)", ["settings"]),
        ("Settings, full length", ["settings_full"]),
        ("Settings, song analysis running", ["settings_analysis"]),
        ("Settings, lyrics downloading", ["settings_lyrics_download"]),
        ("Backup section", ["settings_backup"]),
        ("Restore: confirm", ["dialog_restore_confirm"]),
        ("Restore: result", ["dialog_restore_result"]),
        ("Restore: not a backup", ["dialog_restore_problem"]),
        ("Message: backup saved", ["snackbar_backup_saved"]),
        ("Open-source licences", ["licences"]),
        ("Licence text", ["licence_text"]),
    ]),
    ("Recap", [
        ("Recap page", ["recap"]),
        ("Recap page, full length", ["recap_full"]),
        ("Recap, nothing played", ["recap_empty"]),
        ("Recap, loading", ["recap_loading"]),
        *[(f"Story card: {t.replace('_', ' ')}", [f"recap_story_{t}"]) for t in
          ("intro", "minutes", "top_song", "top_songs", "top_artist", "top_artists", "languages",
           "listening_clock", "highlights", "summary")],
        *[(f"Share image: {t.replace('_', ' ')}", [f"recap_share_{t}"]) for t in
          ("intro", "minutes", "top_song", "top_songs", "top_artist", "top_artists", "languages",
           "listening_clock", "highlights", "summary")],
    ]),
    ("Outside the app (System surfaces page)", [
        ("Widget, small", ["widget_small"]),
        ("Widget, wide", ["widget_wide"]),
        ("Widget, large", ["widget_large"]),
        ("Widget, large with nothing playing", ["widget_large_empty"]),
        ("Widgets on a home screen", ["system_home", "system_home_screen"]),
        ("Media notification in the shade", ["system_shade", "system_notification_shade"]),
        ("Lock-screen player", ["system_lock", "system_lock_screen"]),
        ("\"Analysing your songs\" notification", ["system_shade_analysis"]),
        ("Launcher icon, colour and monochrome", ["launcher_icon", "system_launcher_icon"]),
    ]),
]


def _keys(cov: dict) -> set[str]:
    s = cov["screens"]
    return set(s) if isinstance(s, dict) else {x["key"] for x in s}


def _components(cov: dict) -> list[dict]:
    return cov.get("components") or cov.get("component_sets") or []


def write(expressive: dict, comfort: dict, out: Path):
    ek, ck = _keys(expressive), _keys(comfort)
    lines = [
        "# Design coverage",
        "",
        f"Generated by `design/build/figbuild/coverage.py` on {date.today().isoformat()} from the two files' handoff",
        "reports. The checklist follows the screen map and states in",
        "[CURRENT-UI.md](../docs/design/2026-11-21-ui-directions/CURRENT-UI.md) sections 1 and 2, at",
        "ui-redesign 4fe4dab. Every ticked screen exists in Light and Dark in that file unless the Notes say otherwise.",
        "",
        "| Area | Screen or state | Expressive | Comfort |",
        "|---|---|:-:|:-:|",
    ]
    gaps = {"Expressive": [], "Comfort": []}
    for area, rows in CHECKLIST:
        for label, keys in rows:
            e, c = bool(ek & set(keys)), bool(ck & set(keys))
            lines.append(f"| {area} | {label} | {'✓' if e else '—'} | {'✓' if c else '—'} |")
            if not e: gaps["Expressive"].append(label)
            if not c: gaps["Comfort"].append(label)
    lines += ["", "## Gaps", ""]
    for name, g in gaps.items():
        lines.append(f"- **{name}:** " + ("; ".join(g) if g else "none") + ".")
    listed = {k for _, rows in CHECKLIST for _, keys in rows for k in keys}
    lines += ["", "## Screens beyond the checklist", ""]
    for name, keys in (("Expressive", ek), ("Comfort", ck)):
        extra = sorted(keys - listed)
        lines.append(f"- **{name}:** " + (", ".join(f"`{k}`" for k in extra) if extra else "none") + ".")
    lines += ["", "## Components", ""]
    for name, cov in (("Expressive", expressive), ("Comfort", comfort)):
        comps = _components(cov)
        lines += [f"### {name}", "", "| Component | Page | Variants | Variant properties | Other properties |", "|---|---|--:|---|---|"]
        for c in sorted(comps, key=lambda c: (c.get("page", ""), c["name"])):
            if c["name"].startswith("Icon/"):
                continue
            vp = ", ".join(c.get("variant_properties") or [])
            pp = ", ".join(c.get("properties") or [])
            lines.append(f"| {c['name']} | {c.get('page', '').replace('Components / ', '')} | {c.get('variants', 1)} | {vp} | {pp} |")
        icons = sum(1 for c in comps if c["name"].startswith("Icon/"))
        if icons:
            lines.append(f"\nPlus {icons} icon components on the Icons page.")
        lines.append("")
    out.write_text("\n".join(lines) + "\n")
    return gaps


def main(argv=None):
    argv = argv or sys.argv[1:]
    e = json.loads(Path(argv[0]).read_text())
    c = json.loads(Path(argv[1]).read_text())
    gaps = write(e, c, REPO / "design" / "COVERAGE.md")
    print("gaps:", json.dumps(gaps, indent=1))


if __name__ == "__main__":
    main()
