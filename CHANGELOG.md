# Changelog

All notable changes to Attic are listed here. Versions follow [Semantic Versioning](https://semver.org/).

## [1.1.0] - 2026-11-25

### Home
- "Mixes": three to five mixes of the songs you play and like, grouped by how they sound and topped up with songs you rarely play that sound like them. They are named after their artists and change once a day
- "On repeat", "Haven't heard in a while" and "Newly added" shelves

### Browsing and radio
- Genres in Search, from your files' genre tags or, for songs without one, the styles the analysis model hears in them. A genre page lists its songs and the albums mostly in it
- Radio from a genre, steered towards that genre's average sound
- Smart shuffle on playlists and Favourites: one similar song from the rest of the library after every two of the collection's own, marked in the queue
- Swipe a song row towards the end of the line to add it to the queue
- Light haptics on like, play and pause, reordering, swipe to queue, the sleep timer and a change of style, following the phone's touch-feedback setting

### Playback
- A Playback section in Settings, in both styles
- Volume levelling, on by default: songs play at a similar loudness, from their ReplayGain tags or the loudness measured during analysis
- Crossfade of 2 to 12 seconds, off by default. Songs from one album played in order still run straight into each other
- An Equaliser row that opens the phone's equaliser on Attic's audio, shown when the phone has one

### Android Auto
- Supports Android Auto: browse Home, Playlists, Albums and Artists with covers, search, and "play X on Attic" by voice. It hasn't been tried in a car or on a head unit yet ([details](https://github.com/srivathsanvenkateswaran/attic/blob/main/docs/play/ANDROID-AUTO.md))

### Upgrading from 1.0.0
- Genres from analysis and measured loudness come from a new analysis pass, which runs while the phone charges. Until it has run, Settings shows analysis as part done, and genres and levelling fill in as it reaches each song
- Mixes appear once 12 of your played or liked songs have been analysed

## [1.0.0] - 2026-11-24

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
- Portrait is optional, so tablets can install Attic; its screens are laid out for phones

### Radio and mixes
- Start radio from any song: Attic keeps ten similar songs in the queue
- Nine instant mixes on Home, from your favourites and unplayed songs that sound like them, changing daily
- Radio stays in the language you're listening to, with about one song in ten from another language when the match is strong
- One version of a song per session when it exists in more than one language
- Learns from skips, finishes and likes in a listening log kept on the phone
- Song analysis with the Discogs-EffNet model runs on the phone, in the background while charging

### Lyrics
- Synced lyrics highlight the current line; tapping a line jumps to it
- Reads lyrics from a `.lrc` file beside the song or from the song's tags, with no network
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

[1.1.0]: https://github.com/srivathsanvenkateswaran/attic/releases/tag/v1.1.0
[1.0.0]: https://github.com/srivathsanvenkateswaran/attic/releases/tag/v1.0.0
