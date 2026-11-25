# Mood mixes

A tester asked whether Attic could suggest songs "based on current interests or mood, using some
deterministic algorithms", by asking for interests and favourite moods and then sorting songs into
playlists. Until now Attic's suggestions were only about sound: radio, Instant mixes and the daily
taste mixes all start from audio embeddings and look for what sounds close.

This feature adds a second axis. Every analysed song gets mood scores from small classifiers that
read the embedding analysis already stored. You can pick the moods, genres and languages you like.
Each day Attic builds one mix per mood from those scores, your picks and your listening, by fixed
arithmetic with a seeded daily rotation. Everything runs on the phone.

Status: built, 2026-11-25. Branch `feature/mood-mixes`, on `ui-redesign` at c0efc6f (1.2.0, Room 7).
Room version 8 and backup schema 3 are this feature's: Room 6 is smart transitions, 7 and backup
schema 2 the equaliser.

## What exists today, and what this builds on

| Piece | Where | What this feature takes from it |
|---|---|---|
| Discogs-EffNet, 1,280-d embedding per song, unit length | `analysis/SongAnalyzer.kt`, `EffnetModel.kt`, table `song_analysis` | The input to the mood classifiers. No new decode. |
| Top-5 Discogs styles per song | `StyleScores.kt`, table `song_styles` | Nothing directly; genres come through `GenreIndex`. |
| Genres from tags, else from styles | `data/Genres.kt`, `GenreRepository.kt` | The genre choices in the picker and the genre match in scoring. |
| Languages | `data/LanguageLabels.kt` (`resolve`), table `song_language` | The language choices and the language filter. |
| Listening log, likes, history | `listening_events`, `liked`, `history` | Likes up, skips down, recency. |
| Taste mixes, once a day, seeded by the day | `home/TasteMixes.kt`, `MixCache.kt`, `HomeFeed.kt` | The daily build, the cache file and the seeded rotation pattern. |
| Radio with collection seeding | `radio/RadioEngine.kt` (`RadioState.forCollection`), `RadioController.kt` | Mood radio. |

## The mood classifiers

Essentia publishes classifier heads trained on Discogs-EffNet embeddings, the same model Attic
bundles. Each one is a two-layer network: `embeddings [batch, 1280]` → dense 100 (ReLU) → dense →
softmax or sigmoid → `activations`. All of them ship as ONNX next to the TensorFlow files, so ONNX
Runtime, which Attic already bundles, runs them as they are.

Checked on 2026-11-25 against `https://essentia.upf.edu/models/classification-heads/<name>/<name>-discogs-effnet-1.onnx`:

| Head | Output Attic keeps | ONNX size | Published accuracy | Used for |
|---|---|---|---|---|
| `mood_happy` | P(happy) | 514 KB | 0.87, 5-fold, 302 tracks | Happy, Melancholy (against) |
| `mood_sad` | P(sad) | 514 KB | 0.90, 230 tracks | Melancholy, Happy (against) |
| `mood_relaxed` | P(relaxed) | 514 KB | 0.91, 446 tracks | Chill, Focus, Energetic (against), Romantic |
| `mood_aggressive` | P(aggressive) | 514 KB | 0.98, 280 tracks | Chill, Romantic (against) |
| `mood_party` | P(party) | 514 KB | 0.93 | Party, Melancholy and Romantic (against) |
| `danceability` | P(danceable) | 514 KB | 0.97 | Party, Energetic |
| `voice_instrumental` | P(instrumental) | 514 KB | 0.96 | Focus, Romantic (needs a voice) |
| `engagement_regression` | engagement, about 0 to 1 | 514 KB | Pearson 0.73 | Energetic, Focus and Chill (against) |
| `mtg_jamendo_moodtheme` | 56 mood and theme tags, sigmoid each | 2.74 MB | ROC-AUC 0.76, PR-AUC 0.14 | Romantic, Energetic, Focus, Chill, Happy, Melancholy, Party (supporting) |

Total 6.9 MB of assets. Not used: `mood_acoustic` and `mood_electronic` (sound, not mood),
`approachability` (no mood here needs it). The arousal and valence regressors (DEAM, emoMusic,
MuSe) exist only for the MusiCNN and VGGish embeddings, so they would need a second model run over
the audio; they are out.

**Licence.** The same as the main model: Essentia's model directory licence file names CC BY-NC-ND
4.0 (and links the BY-NC-SA text), from the Music Technology Group, Universitat Pompeu Fabra. Both
allow non-commercial redistribution of the unmodified files with attribution. The heads are
bundled unmodified, each with its unmodified metadata JSON, under `app/src/main/assets/moods/`, and
`third_party/discogs-effnet-NOTICE.txt` lists them. Merging them into one graph would be an
adaptation, which ND doesn't allow, so they stay nine files and nine ONNX sessions.

**How good are they?** On the owner's 742 analysed songs the binary heads rank as one would
expect: the top "sad" songs are slow ballads, the top "party" songs are club tracks, the top
"aggressive" are hero themes, the top "instrumental" are scores and lo-fi. The moodtheme tags are
weaker (a phonk track ranks first for "romantic"), as their published PR-AUC says, so they carry
less weight in every mood below, and no mood rests on them alone except Romantic.

### Input scale: the unit-length embedding

Attic stores each song's embedding as the mean over its patches, normalised to unit length. The
heads were trained on single patches' raw embeddings, and the Essentia pipeline averages the head's
outputs over patches. Two approximations stand between the stored vector and that:

1. the head runs once on the mean embedding, not on every patch with the outputs averaged
2. the mean embedding's length was thrown away when it was normalised

To see what they cost, 169 songs from the owner's library (every tenth file, first three minutes,
decoded on the Mac, never committed) went through the real model on the JVM. For each, the heads
ran three ways: on every patch with the outputs averaged (Essentia's way, the reference), on the
raw mean embedding, and on the unit-length mean times a constant *K*. Rank correlation (Spearman)
with the reference:

| Input | happy | sad | relaxed | aggressive | party | danceable | instrumental | engagement | moodtheme groups |
|---|---|---|---|---|---|---|---|---|---|
| unit × 3.0 | 0.97 | 0.78 | 0.98 | 0.54 | 0.98 | 0.91 | 0.95 | 0.24 | −0.43 to 0.21 |
| unit × the song's own length | 1.00 | 0.98 | 1.00 | 0.94 | 0.99 | 0.98 | 0.98 | 0.97 | 0.97 to 0.99 |

"moodtheme groups" are the tag averages the formulas below use (Chill's calm, relaxing and soft,
and so on). The first approximation costs almost nothing. The second is what matters: the length
of the mean embedding runs from 2.4 to 6.4 (10th to 90th percentile, median 3.8), and for the
moodtheme head the length carries most of the signal (its Spearman with the mean tag activation is
−0.94). With one constant in its place, the tags come out unrelated to the reference, sometimes
inverted. The binary heads hold up: with *K* = 3.0 they rank songs nearly as the reference does
and put 94 to 100 % of songs on the same side of 0.5.

So:

- **Analysis keeps the length.** `EffnetModel` already computes it to normalise; it is now
  returned and stored as `song_analysis.embeddingNorm`. The heads then run on unit × length,
  which matches the reference in every column above.
- **Songs analysed before this get scores straight away** from the stored vector, with no decode:
  the seven binary heads on unit × 3.0. Their moodtheme and engagement values are stored as
  missing, and the formulas leave those terms out (below). These rows are marked `exact = 0`.
- **They become exact the next time analysis decodes them.** A song with an embedding but no
  stored length counts as not finished, the way a song with no styles does, so the analysis
  worker gives it one more decode while the phone charges, in the same last tier as the style
  backfill (`AnalysisPlan`). On a phone whose songs were analysed before styles existed (the
  owner's phone today: 742 embeddings, no styles) that decode was going to happen anyway. Only
  songs analysed by an unreleased build that stored styles but no length get a decode they
  wouldn't have had. If that decode fails, the length is stored as 0 (unknown), so the song isn't
  tried again and keeps its approximate scores.

**On the owner's phone as it is today** (742 embeddings, no lengths, no styles, genre tags empty
or YouTube categories), the approximate backfill alone gives six moods a full mix: 299 songs fit
Chill, 304 Party, 242 Energetic, 233 Happy, 161 Melancholy and 60 Focus. Romantic has none until
analysis has decoded songs again. Scoring the 742 took 0.6 s on the JVM, building the mixes 36 ms.
Focus is the weakest without the tags: a few soft vocal songs get in.

### When scores are computed

`MoodScorer` (new, `analysis/MoodScorer.kt`) scores every embedding that has no mood row at the
current `MoodScorer.VERSION`, or has an approximate row while its length is now known. It runs 128
songs per batch through the heads (all nine with a known length, the seven binary ones without)
and writes the rows in one transaction per batch. It is idempotent, guarded by a mutex, and runs on `Dispatchers.Default` at
background priority:

- at the end of each analysis worker run, and after every 50 songs it analyses
- from `HomeFeed.refresh()` before mixes are built, which is how the first backfill happens: the
  first time Home opens after the update, every stored embedding gets its scores
- never on the main thread, and never by decoding audio

The heads load lazily (a few MB) and close when the pass ends. A song whose file couldn't be read
has no embedding and gets no mood row; it is simply never in a mood mix.

Raising `VERSION` (new heads, another input scale, another order) makes every song be scored again
from its stored embedding, in seconds, with no decode.

### Storage: Room version 8

```
song_analysis  + embeddingNorm REAL (null: analysed before lengths were stored; 0: unknown, not retried)
song_moods(trackKey TEXT PRIMARY KEY, version INTEGER NOT NULL, exact INTEGER NOT NULL, scores BLOB NOT NULL)
```

`scores` is 64 numbers in a fixed order (`MoodSignals`): the eight single outputs in the table's
order, then the 56 moodtheme tags in Essentia's order. Each is a little-endian unsigned 16-bit
fraction of 1, the way `StyleCodec` stores styles, with 0xFFFF meaning missing: 128 bytes a song.
Every head output is kept, not only the ones today's formulas use, so the formulas can change
without scoring again. When analysis saves a new embedding for a song, its mood row is deleted in
the same transaction, and the next scoring pass writes it again.

The migration to 8 adds the column and creates the table, and changes nothing else
(`DatabaseMigrations.SQL_7_TO_8`), with a test in `DatabaseMigrationTest` against the exported
schema `app/schemas/.../8.json`.

**Version numbers.** Smart transitions took 6 (already on the owner's phone) and the equaliser 7, so
mood mixes takes 8: `Migration(7, 8)`, tested from the real version 7 schema. Its SQL touches none
of their tables.

## Your picks: moods, genres and languages

A short page, "Moods and interests", in three groups of chips:

- **Moods you like:** Chill, Happy, Energetic, Focus, Party, Melancholy, Romantic. Any number.
- **Genres:** the library's own genres from `GenreRepository`, biggest first, up to 24. Empty until
  genres exist (tags, or styles from analysis).
- **Languages:** the languages `LanguageLabels.resolve` finds in the library, in the order of
  `LanguageLabels.CHOICES`, plus "No vocals" when the library has instrumentals.

Everything is optional. Save keeps what is chosen; Skip (or back) keeps what was there. It is
offered:

- from Home: a card at the start of the Moods shelf, until you save or dismiss it once
- from Settings: a "Moods and interests" row with a summary of the picks
- not in the first run. The first run has four steps and ends before analysis has started, so
  mood mixes can't exist yet; a fifth step would ask a question whose answer shows nothing for
  days. The Home card asks once there are mixes to show.

**Stored** in `settings.xml` through `AppSettings` (new `Interests` value: three ordered string
lists and `updatedAt`), not Room: it is a handful of settings, and they need no migration.
Genres are stored by `Genre.key` (the normalised name), languages by code, moods by their id.

**In the backup** as a new section, and the schema goes from 2 to 3:

```json
"interests": { "moods": ["chill", "focus"], "genres": ["hip hop", "pop"], "languages": ["ta", "en"], "updatedAt": 1795392000000 }
```

A version 2 reader would skip the section without a word and restore the rest, losing the picks,
so version 3 makes older Attics refuse the file ("made by a newer version"). Version 1 and 2 files
still read: they have no picks. A restore fills in the picks when the phone has none, and otherwise
keeps the phone's (the merge rule for settings that aren't theme, style or autoplay: nothing on the
phone is replaced).

## The algorithm

Everything below is a pure function of its inputs (`home/MoodMixes.kt`), in the style of
`TasteMixes` and `HomeShelves`: rows already read from the database, the picks, the day and the
time passed in. No randomness except one `Random` seeded from the day and the mood, no network,
no clock read inside.

### 1. Signals per song

From a song's 64 stored outputs Attic takes:

| Signal | From |
|---|---|
| H, S, R, A, P, D | P(happy), P(sad), P(relaxed), P(aggressive), P(party), P(danceable) |
| I | P(instrumental) |
| E | engagement, clamped to 0..1 |
| T{a, b, …} | the mean of those moodtheme tags |

Raw probabilities don't compare across libraries: on the owner's songs P(danceable) has a median
of 0.98 and P(instrumental) a median of 0.05. So each signal is turned into its **percentile rank**
among the library's scored songs, `p(x)`, from 0 (lowest) to 1 (highest), ties sharing the mean
rank. A mood's fit is a weighted mean of percentiles; a **gate** on a raw probability keeps a
mood honest in a library that has few such songs (the saddest quarter of a party library isn't
sad).

### 2. Fit per mood

| Mood | Fit (weights) | Gate |
|---|---|---|
| Chill | 3 p(R) + 2 p(T{calm, relaxing, soft}) + 1 (1 − p(A)) + 1 (1 − p(E)) | R ≥ 0.5 |
| Happy | 3 p(H) + 2 p(T{happy, positive, uplifting, fun}) + 1 (1 − p(S)) | H ≥ 0.5 |
| Energetic | 2 p(T{energetic, upbeat, powerful, fast}) + 2 p(E) + 1 p(D) + 2 (1 − p(R)) | R ≤ 0.5 |
| Focus | 3 p(I) + 2 p(T{background, meditative, calm, soundscape}) + 1 p(R) + 2 (1 − p(E)) | I ≥ 0.3, A ≤ 0.5 |
| Party | 3 p(P) + 2 p(D) + 1 p(T{party, fun, groovy}) + 1 p(E) | P ≥ 0.5 |
| Melancholy | 3 p(S) + 2 p(T{melancholic, sad, emotional}) + 1 (1 − p(H)) + 1 (1 − p(P)) | S ≥ 0.5 |
| Romantic | 3 p(T{romantic, love}) + 1 p(R) + 1 (1 − p(A)) + 1 (1 − p(P)) + 1 (1 − p(I)) | A ≤ 0.3, I ≤ 0.5 |

`fit` is the weighted sum divided by the sum of the weights, so it runs 0 to 1. A song **fits** a
mood when it passes the gate and `fit ≥ 0.6`.

**Missing signals.** A song with approximate scores has no moodtheme tags and no engagement. A term
whose signal is missing drops out and the other weights are rescaled; its percentiles are taken
among the songs that have the signal. Romantic rests on the moodtheme tags, so a song without them
can't be Romantic until analysis has decoded it again.

Tried on the 169 sample songs with exact scores, between 23 (Focus, Melancholy) and 58 (Chill)
songs fit each mood, and the top of each list reads as the mood says: slow film ballads for Chill
and Romantic, dance numbers for Party and Energetic, scores and slowed tracks for Focus. Moods
overlap; a song can be in several mixes.

### 3. Score per song and mood

For every song that fits mood *m*:

```
score = fit
      + 0.10 · genre       1 if one of the song's genres is picked, else 0 (0 for all with no genre picked)
      + 0.10 · language    1 if its language is picked, 0.5 if unknown, else 0 (0 for all with no language picked)
      + 0.15 · taste       −1 to 1, below
      − 0.20 · fresh       0 to 1, below
```

- **taste** = 0.5 · liked + 0.5 · f / (f + 2) − 0.8 · s / (s + 1), clamped to −1..1. *f* and *s*
  are the song's finished-or-partial listens and its skipped-or-removed listens, each weighted
  by 2^(−age / 30 days), so what you play now counts more than what you played in spring.
- **fresh** = 2^(−hours since you last heard it / 12): 1 just after, 0.5 at twelve hours, 0.06 at
  two days. A mix leans away from what you just heard.

The weights keep fit in charge: the most a song can gain from picks and taste is 0.35, enough to
reorder songs of similar fit but not to put a song that barely fits ahead of one that clearly
does.

**Left out** before scoring:

- songs whose known language isn't picked, when any language is picked. "No vocals" songs and
  songs of unknown language stay. If that leaves fewer than 8 songs for the mood, the filter is
  dropped for that mood and the language term alone does the work.
- songs you skip: three or more skips, and more skips than finished listens.
- songs no longer in the library.

### 4. The day's mix

1. Sort the fitting songs by score, highest first, ties by key. The **pool** is the first 50.
2. **Daily rotation.** A `Random(day · 31 + mood ordinal)` draws one number *u* per pool song, in
   key order. Each song's draw key is *u*^(1 / w) with *w* = score − (lowest score in the pool) +
   0.05: a weighted sample without replacement (Efraimidis and Spirakis), so higher scores are
   likelier to make the day's cut but the cut changes each day. Songs go in order of draw key.
3. **One version of a recording.** A song whose embedding is 0.985 or closer to one already taken
   is dropped (the copy threshold radio and taste mixes use).
4. **Caps.** At most 3 songs per artist and 2 per album.
5. Take the first **25**. If fewer than **8** are left, the mood has no mix today.
6. **Order.** The draw order, with a song moved to the next place where it doesn't follow its own
   artist.

The same library, scores, picks, listening log, time and day give the same 25 songs in the same
order. A new day changes the draw in step 2 and nothing else.

### 5. Which mixes, and when they are built

- With moods picked, Home shows those moods' mixes in the order of the chips. With none picked,
  Home shows every mood that has a mix, in the fixed order above.
- Search's Browse shows every mood that has a mix, picked or not, so any mood is a tap away.
- Built once a day in `HomeFeed`, after the taste mixes, and kept in memory and in
  `noBackupFilesDir/mood-mixes.txt` (`MoodMixCache`: day, an input fingerprint, then one line per
  mood). The fingerprint covers the picks, `MoodScorer.VERSION` and the number of scored songs, so
  saving new picks rebuilds at once, and so does a backfill that scores more songs. Listening
  during the day doesn't change the day's mixes.

### Cost

For 3,000 songs: reading `song_moods` (384 KB), nine percentile sorts, seven fits, then embeddings
for at most 7 × 50 pool songs, not the whole table. Tens of milliseconds on `Dispatchers.Default`,
once a day. Scoring a backfill is nine small matrix products per batch of 128: about 2 × 10⁸
multiply-adds for 3,000 songs, a second or two of CPU once. Nothing runs on the main thread; Home
only maps keys to songs, as it does for taste mixes. The assets add about 6.9 MB to the APK.

## Mood radio

A mood's page has "Mood radio" next to Play. It opens a radio session (`RadioController.startFromMood`)
seeded the way `RadioState.forCollection` seeds smart shuffle:

- the seed is the centroid of the day's mix (the normalised mean of its embeddings), and the
  session keeps steering toward it
- picks come only from songs that fit the mood (step 2), so radio stays in the mood instead of
  drifting to whatever sounds close
- the opening song is the fitting song nearest the centroid
- picks keep to the picked languages when any are picked; with none, radio follows the
  session's language as song radio does
- the queue plays as "Radio · Chill"

When a mood mix runs out, autoplay continues the same way, leaving out the mix's songs.

## Screens

The design is in both `.figcraft` files, section "Moods" on the Screens page, Light and Dark, phone
and the tablet sizes from `docs/design/2026-11-25-tablet/SPEC.md` (on `feature/tablet-layouts`).

- **Home, Moods shelf.** After the taste mixes. One tile per mix: the mood's name, its one-line
  description, a mosaic of four covers from different albums, and the count. Tapping a tile opens
  the mood's page. Before picks are saved or dismissed, the shelf starts with a card asking for
  them.
- **Mood page.** A collection page: the mood, "Mood mix · N songs, M min", Play, Mood radio, and
  the songs. A line under the title says it was built from how the songs sound and what you
  play, and changes daily.
- **Search, Browse.** A "Moods" group above "Genres", the same tiles. A query matching a mood's
  name ("chill") lists the mood with the genres.
- **Moods and interests.** The picker page, from the Home card and Settings.
- **Settings.** A "Moods and interests" row with the picks' summary.

**Before any mood data.** Until at least 8 songs have mood scores (a new install before analysis,
or the moment after this update before the first backfill finishes), the Moods shelf, its card and
Search's Moods group are hidden rather than empty. A mood page opened then (a back stack restored
after a restart) builds the mixes itself and says "This mood has no mix today" if there is still
none. The picker works with no data: its moods are fixed, and its Genres group shows its empty line.

No new icons: the icon font is a subset (`ui/expressive/Symbols.kt`), and adding glyphs means
cutting it again.

## Privacy

Everything stays on the phone: the classifiers run locally on stored embeddings, the picks live in
the app's settings, and nothing is sent anywhere. `PRIVACY.md` gains a line in "What Attic stores"
(mood scores from analysis, and the moods, genres and languages you pick) and one in "Backups"
(the picks are in the backup file). `site/privacy.html` is regenerated with
`scripts/render_privacy_page.py`.

## Tests

- `MoodMixesTest`: fixtures of songs with hand-set signals. Fits and gates per mood, percentiles
  with ties, the language filter and its fallback, skip exclusion, taste and freshness terms,
  copies, artist and album caps, the minimum size, artist spreading, and determinism: the same
  inputs give the same list, another day a different one, and a change to picks a different one.
- `MoodSignalsTest`: encode and decode, a short blob, the order against the bundled JSON metadata.
- `MoodScorerTest`: the real heads on the JVM against reference outputs for fixed vectors, and the
  batch giving the same as single runs.
- `MoodMixCacheTest`: round trip, a stale day, a changed fingerprint, an old version.
- `DatabaseMigrationTest`: the migration to 8 keeps every row and adds `song_moods`.
- `BackupCodecTest`, `RestorePlannerTest`: the interests section round trip, a version 1 file, the
  merge rule.

## Later

- Mood mixes in Android Auto's browse tree.
- A "Moods" filter in the Library's song list.
- Moods inside radio's own scoring, so song radio can keep a mood too.
