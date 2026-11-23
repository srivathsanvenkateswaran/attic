# Changelog

All notable changes to Attic are listed here. Versions follow [Semantic Versioning](https://semver.org/).

## [1.0.0] - unreleased

The first public release.

### Playback
- Home, Search and Library tabs, with album, artist and playlist screens
- A mini player and a full Now Playing screen, tinted from the cover art
- Shuffle, repeat, and a queue with drag to reorder and swipe to remove
- Favourites and your own playlists, stored on the device
- Background playback with notification and lock-screen controls, including shuffle and like
- Media button support for Bluetooth and headsets, and a pause when another app takes audio focus
- A resizable home-screen widget with the current song, controls and five recent albums

### Look
- Two styles, Expressive and Comfort, switchable in Settings > Appearance
- Dark and light themes, or match the phone
- The Headroom launcher icon

### Radio and mixes
- Start radio from any song: Attic keeps ten similar songs in the queue
- Nine instant mixes on Home, from your favourites and unplayed songs that sound like them, changing daily
- Radio stays in the language you're listening to, with about one song in ten from another language when the match is strong
- One version of a song per session when it exists in more than one language
- Learns from skips, finishes and likes in a listening log kept on the phone
- Song analysis with the Discogs-EffNet model runs on the phone, in the background while charging

### Lyrics
- Synced lyrics highlight the current line; tapping a line jumps to it
- Download lyrics for one song or the whole library from LRCLIB, then use them offline

### Library
- Finds MP3, M4A, FLAC, Ogg, Opus and WAV songs in any folder on the phone or an SD card, leaving out ringtones, notification and alarm sounds, recordings, podcasts, audiobooks and voice notes
- `Artist/Album/` folders under the library root become artists and albums
- Cleans up messy tags without writing to your files: uploader names, video IDs and "(Lyrics)" suffixes, and differently spelled artists merged into one page

### Recap
- Top songs, artists and listening clock for the week, month, year or all time, with cards to save or share

### Backup
- Back up favourites, playlists, listening history, saved lyrics and settings to a file, and restore it on any phone; a restore adds to what is there and deletes nothing
- Android's own backup keeps the same data, encrypted end to end, and a new phone picks it up on first launch

[1.0.0]: https://github.com/srivathsanvenkateswaran/attic/releases/tag/v1.0.0
