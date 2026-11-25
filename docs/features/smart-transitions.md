# Smart transitions

Status: built, 2026-11-25. Where the build differs from the first plan, this note says what was built. Branch `feature/smart-transitions`, from `tier2-integration` at dffdd29.

A tester asked whether Attic crossfades the way the big streaming apps do. It already has a fixed
crossfade (Off or 2–12 s). This adds a third choice, **Smart**: the next song comes in where the
outgoing one winds down rather than a fixed number of seconds before its end. Silence at either end
is skipped, the overlap's length comes from how the two songs' levels move, and the beats are lined
up when the tempos are close enough. Radio and smart shuffle also lean slightly toward songs whose
tempo, energy and key follow on from the song before.

Audio is never time-stretched or pitch-shifted. Songs from one album played in order stay gapless.

## What exists today

Read before this was written:

- `playback/CrossfadePlan.kt`: the pure part. `fadeMs()` picks the fade length (0 for none: setting
  off, no next song, repeat-one, sleep timer "End of this song", consecutive album tracks via
  `AlbumRun.follows`, and never more than a third of either song). `step()` says when to arm the tail.
  Equal-power `cos`/`sin` curves. `TailAlignment` lines the tail up with the main player to 20 ms by
  taking the median of five readings and learning a seek lead.
- `playback/CrossfadeController.kt`: the players. A bare second ExoPlayer (the tail) shares the main
  player's audio session and levelling gain and never takes focus. Five seconds before the fade it
  opens the current song silently and aligns. At the fade it goes to full volume and the main player
  `seekTo(nextIndex, 0)` at volume 0, guarded by `isOwnTransition`, then the two ramp on a 30 ms
  tick. Pause, seek, skip, error, a timeline change that removes the incoming song, or the setting
  going off cuts the fade. The sleep timer's fade is a master volume applied to both.
- `analysis/SongAnalyzer.kt`, `PcmDecoder.kt`, `AnalysisWorker.kt`, `AnalysisStore.kt`: one decode
  per song feeds the model (16 kHz mono, first 12 minutes), chroma (4 frames/s, first 10 minutes) and
  `LoudnessMeter` (native rate, whole file up to the decoder's 45-minute cap). `AnalysisPlan` orders
  the work: loudness-only decodes first, then unanalysed songs, then songs missing styles.
- `data/Database.kt`: Room version 5. `song_analysis` (embedding, chroma), `song_styles`,
  `song_loudness`. `completeKeys` is the count Settings shows as analysed.
- `radio/RadioEngine.kt`: ranks candidates by `similarity + affinity + composer boost − unlabelled
  penalty`, then filters for language, caps and versions. Used by song radio, genre radio, autoplay
  and smart shuffle (`RadioController`). Mood mixes on `feature/mood-mixes` build on the same engine.
- `playback/LevelStore.kt`: the pattern for getting per-song data to the playback thread: a Room
  `Flow` folded into an immutable snapshot off the main thread, read without waiting.

## 1. Transition analysis

### Where it runs

A new `TransitionMeter` (`analysis/TransitionMeter.kt`) rides on the existing decode, like
`LoudnessMeter`. It is fed the 16 kHz mono stream `SongAnalyzer` already makes, but for the whole
file up to the decoder's 45-minute cap, not just the model's first 12 minutes. The model's buffer is
unchanged.

Per 160-sample hop (10 ms) it keeps two numbers, using the existing `PowerSpectrum` on a 512-point
Hann frame:

- **level**: the hop's mean square, in dB;
- **onset strength**: positive spectral flux of `ln(1 + 1000·power)` over 24 log-spaced bands from
  40 Hz to 8 kHz (bands rather than bins keep the logarithms to 24 a frame).

The middle of a song isn't needed, so the meter keeps the first 90 s and a ring buffer of the last
90 s of frames, a histogram of half-second levels for the reference, and running sums for the
whole-song figures. Memory stays at about 150 KB whatever the length.

### What it works out

All times are milliseconds from the start of the decoded audio.

| Field | How |
|---|---|
| `audioStartMs`, `audioEndMs` | First and last frame above −60 dBFS and within 45 dB of the song's reference level. Everything outside is lead-in or trailing silence. |
| reference level | The 90th percentile of 0.5 s block levels. A song's "full" level, robust to a quiet intro or a loud peak. |
| `outroStartMs` | Scanning back from `audioEndMs` over 0.5 s blocks smoothed over 2 s: the last point where the level is within 8 dB of the reference, then further back while the level keeps rising going backwards, to where the decline began (a fade-out crosses −8 dB part way down). After it the song only gets quieter. Equal to `audioEndMs` for a cold ending. |
| `introEnergy`, `outroEnergy` | Block levels relative to the reference, two per second, quantised to 0.5 dB in a byte (0 = reference, 255 = −127.5 dB). 30 s from `audioStartMs` forward, and 30 s back from `audioEndMs`. 60 bytes each. |
| `introBpm`, `introBeatMs`, `introBeatConfidence` | Tempo and beat phase over the 20 s after `audioStartMs` (below). |
| `outroBpm`, `outroBeatMs`, `outroBeatConfidence` | The same over the window `[outroStartMs − 15 s, outroStartMs + 5 s]`, clamped to the audio, since the beat-aligned overlap sits around the outro's start. |
| `energy` | Mean onset strength above its running half-second mean over the whole song, scaled 0–1: spikes count, steady sound doesn't. A loudness-independent "how busy" figure for radio ordering. |
| `key`, `keyConfidence` | From the chroma analysis already stores, not from this decode (below). |
| `decodedMs` | Length of the decoded audio. If it differs from the player's duration by more than 1.5 s the stored positions are not trusted for that song. |

**Tempo and beats.** On the onset-strength envelope of a 20 s window (1,250 frames):

1. Autocorrelation over lags for 60–200 BPM, weighted by a log-normal prior centred on 120 BPM
   (one octave wide), so the half- and double-time peaks lose to the one a listener would tap.
2. The best lag refined by parabolic interpolation.
3. Beat phase: the offset that maximises the summed onset strength on a comb of that period.
4. Beats picked near each comb tooth (the local onset maximum within ±10 % of a period), then a
   least-squares line through them gives the final period and phase. Over 20 s this pins the period
   to well under 0.5 %, which matters: without time-stretching, a period error turns straight into
   drift across the overlap.
5. Confidence: half how periodic the envelope is at that lag, half how much stronger it is on the
   beats than on average, all scaled by how far the envelope's peaks stand above its median (steady
   sound only flickers). Below 0.35 the window counts as having no beat (an ambient intro, a spoken
   outro, a fade-out).

Very fast tempos can read at half time (174 BPM clicks read as 87). The planner folds half and
double time together, so that grid lines up just as well.

`introBeatMs` is the fitted beat nearest the window's start; `outroBeatMs` the one nearest
`outroStartMs`. Any other beat in the window is `beatMs + k × 60000 / bpm`.

**Key.** The stored chroma frames are averaged and correlated with the 24 rotations of the
Krumhansl–Kessler major and minor profiles. `key` is 0–11 major (C = 0), 12–23 minor.
`keyConfidence` is the gap between the best and second-best correlation. Radio uses the key only
above 0.1 confidence. Computing it from stored chroma means a backfill needs no model run and no
extra pass over the audio.

### Storage: Room 5 → 6

A new table, one row per song, about 200 bytes:

```sql
CREATE TABLE IF NOT EXISTS `song_transitions` (
  `trackKey` TEXT NOT NULL, `version` INTEGER NOT NULL, `decodedMs` INTEGER,
  `audioStartMs` INTEGER, `audioEndMs` INTEGER, `outroStartMs` INTEGER,
  `introBpm` REAL, `introBeatMs` INTEGER, `introBeatConfidence` REAL,
  `outroBpm` REAL, `outroBeatMs` INTEGER, `outroBeatConfidence` REAL,
  `energy` REAL, `key` INTEGER, `keyConfidence` REAL,
  `introEnergy` BLOB, `outroEnergy` BLOB, `measuredAt` INTEGER NOT NULL,
  PRIMARY KEY(`trackKey`))
```

A row with null `decodedMs` records a file that couldn't be decoded, so it isn't tried again until
`TRANSITIONS_VERSION` changes. Nothing existing changes, so every saved row carries over.
`DatabaseMigrationTest` gets a 5 → 6 case against the exported schema, and the existing chain test
runs through it.

**Clash with the equaliser branch.** `feature/per-song-equaliser` also takes Room 5 → 6, for
`eq_settings` and `eq_presets`. Both migrations only add tables with `CREATE TABLE IF NOT EXISTS`,
so whichever branch reaches `tier2-integration` second renumbers its migration to 6 → 7 and regenerates
the schema JSON. No SQL changes. On the phone, a database already at 6 from an equaliser build would
fail Room's identity check with this branch's 6, so the phone's database version is checked before
installing (see Testing).

### Backfill

`AnalysisNeed.Loudness` becomes `AnalysisNeed.Measure`: a decode with no model run that measures
whatever the song lacks, loudness, transitions or both, in one pass. A full analysis now saves the
transitions in the same transaction as the sound, styles and loudness.

`AnalysisPlan.todo` order:

1. songs missing loudness (it changes how they sound),
2. unanalysed songs (radio can't use them),
3. songs missing only transitions (the backfill, a decode each),
4. songs missing styles (a model run each, which also measures transitions).

Recently played songs first within each, as now.

A library that was fully analysed before this update gets its transitions on the next charging
run. Until a song has them, Smart falls back to the fixed crossfade for it, and radio leaves the
flow term out for it. Radio, genres and levelling keep using what is already stored.

`completeKeys`, the Settings count, also requires a transitions row. This follows what the owner
accepted for styles and loudness on 2026-11-23: a library analysed before the update shows as part
done until the next charging run.

### Cost

The meter adds one 512-point FFT per 256 samples of 16 kHz audio: 62.5 FFTs per second of audio,
about a quarter of what `MelSpectrogram` would do if it took every frame. The model run dominates a
full analysis, so the expected cost is a few percent of it. For a backfill decode the meter's cost
is set against the decode itself. Both are measured and reported: on the JVM per minute of audio,
and on the phone from `AnalysisWorker`'s per-song log line, which gains the meter's own time.

## 2. Transition planning

### The setting

`AppSettings.transitions`: `Off`, `Crossfade` or `Smart`, saved as `transitions`. The existing
`crossfade_seconds` stays as the crossfade length: Crossfade uses it, and Smart uses it for songs
without analysis. Its choices become 2–12 s, default 6. On first read, an install with
`crossfade_seconds > 0` becomes `Crossfade` with that length, and `0` becomes `Off` with length 6.
Default for a new install: Off, as now.

### The planner

`playback/TransitionPlanner.kt`, pure Kotlin, JVM-tested. `CrossfadePlan.fadeMs` becomes one branch
of it.

```kotlin
data class Transition(
    val fadeStartMs: Long,      // in the outgoing song: when the tail takes over
    val fadeMs: Long,           // overlap length
    val incomingStartMs: Long,  // where the main player enters the next song
    val beatAligned: Boolean,
    val outBpm: Double?, val inBpm: Double?,
    val expectedDriftMs: Long,  // worst beat drift across the overlap, from the tempo gap
    val kind: Kind,             // Fixed, Smart, SmartFallback
)
```

It returns null where today's `fadeMs` returns 0: setting off, no next song, repeat-one, "End of
this song", consecutive album tracks.

**Crossfade** (and Smart without analysis): `fadeStartMs = duration − length`, `incomingStartMs = 0`,
exactly as today, including the one-third caps.

**Smart**, with transitions stored for the outgoing song (and the positions trusted):

1. The overlap must end by the outgoing `audioEndMs`, so trailing silence is never played.
2. The main player enters the incoming song at its `audioStartMs` (minus 20 ms of margin), so its
   leading silence is skipped. Without incoming analysis this is 0.
3. **Length and start from the energy curves.** For each candidate length
   L ∈ {1, 1.5, 2, 3, 4, 6, 8, 10, 12} s and each start t from `outroStartMs − 4 s` to
   `audioEndMs − L` in 0.5 s steps, the two curves are mixed through the equal-power ramps and
   scored on a 250 ms grid over the same stretch for every candidate: from the earliest start to 6 s
   after the outgoing song's end. The plan with the lowest cost wins; ties go to the shorter overlap,
   then the later start. The weights are clash 2, lost music 3, dead air 1.5.
   - *Clash*: both songs loud at once. The sum over the overlap of
     `min(P_out(t)·cos², P_in(u)·sin²)`, the power of whichever is quieter at each moment. Two
     full-level songs overlapping for 8 s score badly; a fade-out under a build-up scores near zero.
   - *Lost music*: the outgoing song's power that the fade takes away,
     `P_out(t)·(1 − cos²)`, plus the same for the incoming song's opening under `1 − sin²`. Cutting
     into a loud ending, or fading in over a big first hit, costs.
   - *Dead air*: what the listener hears (the outgoing tail before the overlap, the mix during it,
     the incoming song after it) wherever it sits more than 12 dB under the reference.

   Without incoming analysis the incoming curve is taken as full level from its start, which keeps
   overlaps short.

   What this gives, and what the fixtures check: a long fade-out into a slow build gets a long
   overlap starting near the fade-out; a cold ending into a loud first beat gets about a second
   right at the end; a cold ending into a quiet intro gets a short overlap and lets the intro play.
4. **Beat alignment**, only when both the outgoing outro and the incoming intro have a beat
   (confidence ≥ 0.35) and the tempos are within 6 % after allowing for half and double time.
   - Without time-stretching the two grids drift apart by `L × |1 − ratio|`. Aligning at the
     midpoint halves the worst case, and the overlap may be at most as long as keeps that worst case
     within 40 ms (about a twelfth of a beat at 120 BPM): about 2.7 s at 3 %, 1.6 s at 5 %. Under
     1.5 s it isn't aligned at all.
   - If the energy-chosen overlap is longer than that, the search runs again with lengths capped at
     it, and the shorter overlap is used only if it costs at most 1.3 times the best one plus 0.2.
     A long fade-out into a slow build keeps its 8 s blend unaligned rather than becoming a 2 s
     aligned one; a short, punchy handover gets aligned. (The first plan shortened the overlap
     around its old middle; that entered the next song seconds in and skipped its build-up.)
   - The overlap's middle is then moved onto a beat of each song: the outgoing beat nearest it, and
     the incoming beat nearest `incomingStartMs + L/2`. Each end moves by at most half a beat;
     neither crosses the audio's start or end.
   - So the 6 % window is kept as asked, but it is the drift budget that decides.

   Downbeats (bar lines) are not detected; beats line up, bars may not.
5. The plan obeys today's caps: the overlap is at most a third of either song, and the incoming
   song must have `incomingStartMs + L` of audio.

Planning is arithmetic on a few hundred bytes, with the level curves turned into lookup tables
once per plan: 120–320 µs on the JVM. It runs on the main thread when the current or next song
changes, like the old `fadeMs`. It is timed in a unit test and logged on the phone, and it never
touches the database.

### Getting the analysis to the player

`playback/TransitionStore.kt`, the `LevelStore` pattern: `dao.transitions(TRANSITIONS_VERSION)`
as a `Flow`, decoded off the main thread into an immutable `Map<String, TransitionInfo>`, read by
the controller without waiting. Rows are small (about 200 bytes), so a 5,000-song library is about
1 MB in memory. A new snapshot calls `CrossfadeController.settingsChanged()`, so a song analysed
while it plays gets a smart plan for its ending.

### The controller

`CrossfadeController` keeps its states, its tail player and every rule for cutting a fade. What
changes:

- `Plan` carries a `Transition` instead of a bare `fadeMs`. `step()` measures to `fadeStartMs`
  instead of `duration − fadeMs`.
- At the fade, the main player seeks to `(nextIndex, incomingStartMs + entryLeadMs)` instead of
  `(nextIndex, 0)`.
- Fade progress is `(main.currentPosition − incomingStartMs) / fadeMs` instead of
  `main.currentPosition / fadeMs`.
- The fade finishes at the end of the overlap, which may be before the outgoing song's end; the
  tail is at volume 0 by then and is stopped.

**Alignment at the join.** The tail is aligned to the main player to 20 ms before the fade (as
today). At the fade the main player jumps into another file, and its first audio may land a little
late. For the first 600 ms of the fade the controller reads both positions on each tick and works
out the beat-phase error, `(tail − fadeStartMs) − (main − incomingStartMs)`, as the median of five
readings. It is logged with the plan. The controller keeps an `entryLeadMs`, learned like
`TailAlignment.leadMs`, and seeks the next fade's incoming song that much ahead, so the error
settles after the first fade or two. No correction seek happens mid-fade: it would be a second
audible jump.

**Logging.** Tag `AtticTransition`, info level: the plan (kind, fade start, length, entry point,
the two BPMs and confidences, aligned or not, expected drift, planning time in µs) when it is made,
and the measured join error when the fade starts. This is what the phone test reads, since nobody
can listen there.

**Album runs stay gapless.** `AlbumRun.follows` still returns no transition, so ExoPlayer's gapless
playback is untouched. Smart doesn't skip silence between album tracks either: a live album's
applause or a hidden gap is the album's.

### What stays as it is

- The media session, notification, widget and Android Auto only see the main player. A transition
  is an early move to the next item, logged by `ListenTracker` as a song that finished. With Smart
  that move can come earlier than a fixed fade's (at the outro, not a few seconds from the end).
  The notification card moves when the overlap starts.
- Pause, seek or skip mid-fade cuts cleanly: the tail stops, the main player goes to full volume in
  the new song, wherever it is.
- The sleep timer's countdown fade multiplies both players. "End of this song" turns transitions off
  for that song, as now.
- The tail takes the main player's levelling gain. A per-song equaliser (`feature/per-song-equaliser`)
  that sits beside levelling as an `AudioProcessor` would need the same treatment on the tail's
  renderer: that is noted for whoever merges the two, since the tail is built here with its own
  `LevellingRenderersFactory`.

## 3. Smoother radio order

`RadioSong` gains an optional `flow: FlowTraits(bpm, bpmConfidence, energy, key, keyConfidence)`.
`RadioController.loadSongs` fills it from `song_transitions` (one light query, no BLOBs). In
`RadioEngine.next`, the candidate score gains `FLOW_WEIGHT × flow(previous, candidate)`, where
`previous` is the last song in `playedKeys` (the one the pick will follow):

- **Tempo:** the folded ratio (half and double time allowed). Within 3 % scores +1, falling to 0 at
  8 % and −0.5 beyond. Needs confidence on both.
- **Energy:** `1 − 2·|Δenergy|`, clamped to −0.5..1.
- **Key:** on the Camelot wheel, same key or one step (a fifth, or relative major/minor) +1, two
  steps 0, further −0.5. Needs key confidence on both.

`flow` is the mean of whichever parts both songs have, and 0 when they share none. `FLOW_WEIGHT`
is 0.02, under the 0.03 composer boost and well under the spread of sound similarity (a typical
unrelated pair scores 0.54, the cut-off is 0.62). So the flow term reorders close candidates and
never brings in a song radio wouldn't otherwise play. It is arithmetic on stored numbers, so it is
deterministic: the same library and session give the same picks. Smart shuffle and anything else
built on `RadioEngine` gets it too.

## 4. UI

Designed first (FigCraft, both styles, Light and Dark), then built. The brief to the designer:

- **Settings › Playback.** The "Crossfade" row becomes **"Transitions"** with three choices, Off,
  Crossfade and Smart, in each kit's existing picker (Expressive menu, Comfort pop-over). Under it,
  when the choice is Crossfade or Smart, a **"Crossfade length"** row with today's 2–12 s menu. The
  supporting text says what the chosen mode does, for example for Smart: "Starts the next song as
  this one winds down, skips silence, and lines up the beats when the tempos match. Songs not
  analysed yet use the crossfade length." Copy follows `BRIEF.md` and the unslop contract; the
  designer refines it.
- **Now Playing hint**: Expressive's seek bar draws a 4dp dot where a Smart transition will start,
  until the handle reaches it. The service publishes the planned point in the session extras
  (`TransitionSession`), the way the sleep timer's status goes out, so the UI never reads the
  analysis itself. The design files also drew a notch for Comfort; under its total time it said
  little, so it wasn't built.

`CURRENT-UI.md` and both `KIT.md` files are updated with the code.

## 5. Tests

- `TransitionMeterTest`, on generated fixtures at 16 kHz:
  - click tracks at 80, 100, 120, 128, 150 and 174 BPM, some with a level change and noise: tempo
    within 1 %, beat phase within 20 ms;
  - silence before and after: `audioStartMs`/`audioEndMs` within 50 ms;
  - a linear fade-in and a linear fade-out, and a cold ending: `outroStartMs` within 1 s of where the
    fade begins, and at the end for the cold ending;
  - a pad with no beat: confidence under the threshold.
  The test prints a table of the results, which goes in the final report.
- `KeyEstimateTest`: synthetic chroma for C major, A minor and F# major triads.
- `TransitionPlannerTest`: fixed fallback identical to today's numbers (today's `CrossfadePlanTest`
  cases kept), silence skipping at both ends, the energy cases above, beat alignment moves the
  midpoint onto beats and respects the drift budget, half/double time, no alignment over 6 % or
  under the confidence threshold, album runs and repeat-one return null.
- `RadioEngineTest`: two candidates of equal similarity, the one that flows wins; a candidate 0.05
  more similar still beats a better-flowing one; songs without traits rank as before; same input,
  same order.
- `AnalysisPlanTest`: the new order and the `Measure` need.
- `DatabaseMigrationTest`: 5 → 6 keeps every row and creates `song_transitions`.

## 6. Testing on the phone

Following `/tmp/attic-dev/tools/README.md`: the lock for every session, `install -r` only, a timeout
on every adb call, play only from the mini player or the existing queue, never "Download lyrics",
no full analysis run.

1. Before installing, read the database's `user_version` and the identity hash in
   `room_master_table` from a copy pulled with `run-as … cat` (read only). If it is already 6 from
   an equaliser build, stop and decide before installing. Confirm the APK has `AtticDatabase_Impl`.
2. Record the transitions setting, set Smart.
3. Re-analyse at most two songs (clear their `song_transitions` rows only, through a debug-only
   intent, then let the debug hook measure just those two) and log the meter's time. Restore
   afterwards by keeping the measured rows if they are the same version, which is what a normal run
   would write.
4. Play across transitions from the existing queue; record the screen; read `AtticTransition` for the
   plan and the join error. Check a pause, a seek and a skip mid-fade, and an album run staying
   gapless.
5. Restore: transitions setting back, playback paused, animation scales 1.0,
   `stay_on_while_plugged_in` 0, screen timeout 600000, demo mode off, final debug build installed.

What the phone can't tell: whether a transition sounds good. That is for the owner's ears, and the
report lists the transitions to listen to.

## Not doing

- Time-stretching or pitch-shifting, which would line up any tempos at the cost of the audio.
- Downbeat or phrase detection: lining up bars and 8-bar phrases is the next step if beats alone
  aren't enough.
- EQ or filter sweeps during the overlap (a DJ's bass swap). The overlap is volume only.
- Changing where an album track starts or ends.
