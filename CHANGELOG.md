# Changelog

All notable changes to Attic are listed here. Versions follow [Semantic Versioning](https://semver.org/).

## [1.3.0] - unreleased

### Mood mixes
- A Moods shelf on Home, after Mixes: one mix a day for each of Chill, Happy, Energetic, Focus, Party, Melancholy and Romantic that has enough songs, up to 25 songs each
- Each song's moods are worked out on the phone from the song analysis already stored, by nine small classifiers that ship with the app. No audio is decoded for this and nothing goes over the network
- A mix is built from those scores, the genres and languages you picked, your likes and skips, and how recently you played each song. It is the same all day and changes the next, or when you change your picks
- "Moods and interests" lets you pick the moods, genres and languages you like. Open it from the card at the start of the Moods shelf, or from Settings > Moods. With moods picked, Home shows those moods in your order; with none, every mood that has a mix
- A mood's page has Play, shuffle and Mood radio, which steers towards the middle of the day's mix and only picks songs that fit the mood. When a mood mix ends, autoplay carries on in the same mood without repeating the mix
- Search's Browse has a Moods group above Genres, and typing a mood's name ("chill") finds it

### Playback
- Settings > Playback > Transitions: Off, Crossfade or Smart. Smart starts the next song as this one winds down, skips silence at both ends, picks an overlap of 1 to 12 seconds from how the two songs' levels move, and lines up a beat of each when their tempos are within 6% (half and double time count). It never stretches or pitch-shifts the audio. Songs not analysed yet use the crossfade length, now its own row, 6 seconds by default. Songs from one album played in order still run straight into each other. Transitions are off for a new install, and an existing crossfade setting carries over as Crossfade
- The crossfade lines up the two songs reliably: it used to read a position that stood still for about a quarter of a second and then jumped, so the join was down to luck and the volume moved in steps
- In the Expressive style, a dot on Now Playing's seek bar marks where Smart will start the next song
- Radio and smart shuffle lean slightly towards songs whose tempo, energy and key follow on from the last one
- Analysis also measures each song's start, end, outro, tempo and key

### Backup
- Backups carry your moods, genres and languages. A backup file made by 1.3.0 can't be restored by an older Attic. Files from 1.0, 1.1 and 1.2 still restore

### Upgrading from 1.2.0
- Songs already analysed get mood scores the first time Home opens, from what analysis stored. Romantic and Smart transitions need one more decode of each song, so analysis runs once more while the phone charges, and until it has, Settings shows analysis as not finished, Romantic has no mix and Smart uses the crossfade length for those songs
- The APK grows by about 6.5 MB, for the mood classifiers

<!-- TODO: tablets -->

### Tablets
- Laid out for tablets and other large windows, in both styles, light and dark. A navigation rail (Expressive) or a side dock (Comfort) replaces the bottom bar, and a player bar runs along the bottom of the screen
- The Library shows the album, artist or playlist you open beside its list when the tablet is sideways; albums, artists and playlists put their cover and buttons beside the songs
- Now Playing sits beside the queue or the lyrics when the tablet is sideways, and full-screen lyrics keep the cover and controls beside them
- Home, Search and the Library spread into columns; Settings, the queue and the Recap page stay a readable width; sheets and dialogs no longer stretch across the screen
- Tablets turn freely and keep the screen, the open player and your place in a list when rotated or resized, including in split screen and floating windows. Phones stay in portrait
- With a keyboard, Space plays and pauses and Esc closes what is open; with a mouse, a right click on a song opens its menu

## [1.2.0] - 2026-11-25

### Equaliser
- A ten-band equaliser of Attic's own, from 31 Hz to 16 kHz, each band −12 to +12 dB. It is off until you turn it on
- "Equaliser" in the menu on Now Playing opens a sheet with the switch, presets, the ten bands over the curve they make, "Save for this song", "Save for this album" and "Use default"
- A song plays with its own setting, else its album's, else the default. A pill on Now Playing says "This song" or "This album" when one of them is in use
- Eleven built-in presets, from Flat and Bass boost to Spoken word, and presets of your own that you can rename and delete
- Settings > Playback > Equaliser holds the default curve and lists the songs and albums with their own setting. Its "Phone equaliser" row still opens the phone's own equaliser, when the phone has one
- It runs inside Attic's own audio path, so it sounds the same on every phone, changes on a song's first sample (gapless songs too), and during a crossfade each song keeps its own curve
- Boost presets play 3 to 5 dB quieter instead of distorting: Attic takes back the curve's boost and a limiter keeps peaks under −1 dBFS
- About 1.4% of one core while a curve is on, measured on the test phone, with no change to cold start or scrolling

### Backup
- Backups carry the equaliser: its switch, the default curve, every song's and album's setting, and your presets
- A backup file made by 1.2.0 can't be restored by an older Attic. Files from 1.0 and 1.1 still restore

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

### Tablets
- Portrait is optional, so tablets can install Attic; its screens are laid out for phones

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

[1.2.0]: https://github.com/srivathsanvenkateswaran/attic/releases/tag/v1.2.0
[1.1.0]: https://github.com/srivathsanvenkateswaran/attic/releases/tag/v1.1.0
[1.0.0]: https://github.com/srivathsanvenkateswaran/attic/releases/tag/v1.0.0
