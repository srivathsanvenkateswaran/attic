# Equaliser

Attic has its own ten-band equaliser, and a song or an album can keep a setting of its own that
comes back whenever it plays. Code: `app/src/main/java/app/attic/player/playback/eq/` (engine and
store), `ui/model/EqualiserModel.kt` (state), `ui/expressive/ExpressiveEqualiser.kt` and
`BandGraph.kt`, `ui/comfort/ComfortEqualiser.kt`. Design: sheet 11 in
`docs/design/2026-11-21-ui-directions/pixel/` and `oneui/`, and the Equaliser section of both
`.figcraft` files.

## How other players do it (November 2026)

| Player | Engine | Per song |
|---|---|---|
| Auxio | None of its own: opens the phone's panel (`ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL`) and announces its session | No |
| Gramophone | None for the user: opens the phone's panel; its own DynamicsProcessing only limits ReplayGain | No |
| Musicolet | In-app, 3 to 31 bands, bass and preamp | No: settings per output (speaker, headphones, Bluetooth) |
| Poweramp | Its own DSP, up to 32 bands, parametric mode, preamp and limiter | Yes: song, then album, folder or genre, then device, output, default |

## The engine

A Media3 `AudioProcessor` after volume levelling, not `android.media.audiofx.Equalizer` on the
audio session:

- **The same on every phone.** The session effect has whatever bands the phone's ROM gives (the
  stock one has five, at 60 Hz to 14 kHz), and some ROMs break session effects outright.
- **Exact song boundaries.** Media3 1.11 flushes processors at each new playlist item with the
  item's period, the same signal levelling uses, so a song's curve starts on its first sample,
  gapless transitions included. A session effect is changed from outside the audio path, so a
  per-song switch would land a little early or late.
- **Crossfades.** The crossfade's second player gets its own processor reading the same settings,
  so the outgoing song keeps its curve while the incoming one has its own.
- **No headroom surprises.** It runs in Attic's own pipeline, so it can see how much it boosts and
  take that back.

The "Phone equaliser" row on the Equaliser page still opens the phone's own panel, for anyone who
prefers it.

Inside (`EqDesign.kt`, `EqualiserAudioProcessor.kt`):
- Ten bands at 31.25 Hz to 16 kHz, octaves of 1 kHz, each −12 to +12 dB. Nine peaking filters at
  Q 0.8 and a high shelf at the top (a peaking filter at 16 kHz is squeezed against Nyquist).
- Neighbouring bands overlap, so a filter's gain is not its slider's: the gains are solved so the
  response passes through every slider (Välimäki and Liski, "Accurate cascade graphic equalizer",
  2017). The worst error at a band centre is 0.07 dB over every test curve. Without it, all
  sliders at +6 would give 12.5 dB at 1 kHz.
- Double-precision filter state: at 31 Hz and 48 kHz, single precision leaves noise at −58 dBFS.
- The pre-gain takes back the curve's whole peak boost, and a limiter (−1 dBFS ceiling, instant
  attack, 80 ms release) catches what is left. On a loud master, taking back only half the boost
  had the limiter working on 85% of samples. The cost of full headroom: a boost preset plays 3 to
  5 dB quieter than without it.
- A curve changed mid-song glides over 80 ms, so dragging a band is smooth. A flat curve copies
  the audio through untouched.
- Presets are the eleven that have shipped with iTunes and the Mac's Music app for years, from
  that app's own preset file, on the 0.5 dB slider grid.

## Settings

- `eq_settings` (Room 7, on top of smart transitions' version 6): one row per song (by library key) or album (by album key), the curve as
  ten numbers. `eq_presets`: the user's named curves. The switch and the default curve are in
  settings. Backups carry all of it (schema 2; version 1 files still read). See `backup.md`.
- `EqualiserStore` holds everything as one immutable value the playback thread reads without
  waiting. A dragged band is heard on the next buffer; the save waits for the band to be still
  for 400 ms.
- A song plays with its own setting, else its album's, else the default. The sheet always edits
  the one the song is playing with, and says which.

## Cost

Measured on 2026-11-25 by `EqBenchmarkTest`: 60 s of 48 kHz stereo 16-bit noise per row, after
warm-up, on a desktop JVM (Apple M4). A phone's ART is several times slower, so read the last
column as a lower bound; even ten times it is about 2% of one core.

| Case | Buffer | ns per buffer | ns per frame | Share of one core in real time |
|---|---|--:|--:|--:|
| Equaliser, flat (copies through) | 4096 B | 466 | 0.46 | 0.002% |
| Equaliser, ten-band curve | 4096 B | 38,528 | 37.6 | 0.18% |
| Equaliser, gliding every buffer (a band being dragged) | 4096 B | 51,161 | 50.0 | 0.24% |
| Volume levelling, for comparison | 4096 B | 2,524 | 2.5 | 0.012% |
| Equaliser, ten-band curve | 16384 B | 155,175 | 37.9 | 0.18% |

- Drawing the response: 128 points in about 22 µs, so a drag frame costs nothing visible.
- Nothing runs on the main thread per buffer. A dragged band updates one immutable value and
  bumps a counter; the save waits 400 ms and goes to Room's executor or `SharedPreferences.apply`.
- Start-up does no equaliser work: the store is created when the playback service starts or a
  screen first asks for it, and it reads only settings already loaded and Room flows.
- Lists don't change. Now Playing reads one more `StateFlow` for its indicator.

On the phone (Xiaomi "miel", Android 16, the owner's 740-song library), 2026-11-25. The release
builds ran as a separate test install (`app.attic.player.eqtest`), so the owner's Attic data was
never touched: first the base (`ui-redesign` at f2dfff7), then this branch installed over it. The
measurements use `/tmp/attic-dev/perf/session.sh`, Dark theme, speed-profile compiled, with medians
over 10 cold starts and 3 full scrolls of the song list.

| | Cold start, base → equaliser | Janky frames scrolling Songs, base → equaliser |
|---|--:|--:|
| Expressive | 256.5 → 244.5 ms | 2.33% → 2.31% |
| Comfort | 257.5 → 243.5 ms | 2.00% → 2.01% |

- **Playback thread** (`ExoPlayer:Playb`), from its `/proc` CPU ticks over 20 s:
  - with Bass boost on a song: 4.55% of one core
  - on the next song, flat: 3.15%

  So the equaliser costs about 1.4% of one core. That is an upper bound: the `AtticEq` debug meters were on for the test, and they filter the signal again.
- **Migration:** opening the test install's data with this branch ran the equaliser's migration without a crash. That was before the equaliser moved to version 7; the 6→7 step is covered by `DatabaseMigrationTest`, starting from smart transitions' version-6 schema.

## Checking it on a phone

`adb shell setprop log.tag.AtticEq DEBUG` makes the processor log each song's curve as it starts,
each change mid-song, and once a second the input and output level in dBFS, overall and below
177 Hz, so a bass boost shows up as a number.
