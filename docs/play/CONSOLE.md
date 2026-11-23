# Play Console answers

What to enter in each Play Console form for Attic, in the order the Console asks. Store listing
text is in `fastlane/metadata/android/en-US/`; paste from there so the two never drift. Data
safety answers and the foreground service declarations are in `data-safety.md` and
`COMPLIANCE.md` in this folder.

Anything in `<ANGLE BRACKETS>` is for the owner to fill in.

## Create app

| Field | Answer |
|---|---|
| App name | `Attic: Offline Music Player` (same as `title.txt`, 27 of 30 characters) |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free. This can't be changed to paid later, which is fine for Attic. |
| Declarations | Tick both: Developer Program Policies, and US export laws. Attic contains no encryption beyond Android's own HTTPS. |

## Store settings

| Field | Answer |
|---|---|
| App category | Music & Audio |
| Tags | Up to five, from the Console's list. Pick the closest to: music player, offline music, MP3 player, lyrics. |
| Email address | `<CONTACT EMAIL>`. It is shown publicly on the listing, so use an address meant for that. |
| Phone number | Leave blank; it's optional. |
| Website | `https://srivathsanvenkateswaran.github.io/attic/` once Pages is on (see `site/README.md`) |
| External marketing | Your choice; it only lets Google advertise the listing outside Play. |

## Main store listing

| Field | Source |
|---|---|
| App name | `title.txt` |
| Short description | `short_description.txt` (71 of 80 characters) |
| Full description | `full_description.txt` (about 3,000 of 4,000 characters) |
| App icon | `images/icon.png`, 512×512, 32-bit PNG |
| Feature graphic | `images/featureGraphic.png`, 1024×500, 24-bit PNG |
| Phone screenshots | `images/phoneScreenshots/`, once captured: 2 to 8, and at least four 9:16 shots for recommendation placements |
| Video | None |

## App content (Policy > App content)

### Privacy policy

`https://srivathsanvenkateswaran.github.io/attic/privacy.html`

The page only exists once Pages is enabled and the site is deployed from `main`. Open it in a
private window before pasting it in; Play rejects a URL that doesn't load.

### Ads

**No, my app does not contain ads.** Attic has no ad SDK.

### App access

**All functionality in my app is available without any special access.** There is no login.

Note for reviewers: put any music files on the phone (for example in `Music/` or `Download/`).
The library is empty until then.

Without music files the app shows "No songs on this phone". That is correct behaviour, but a
reviewer could read it as broken. The full description says up front that Attic plays music
files already on the phone.

### Content rating (IARC questionnaire)

| Question | Answer | Why |
|---|---|---|
| Email for the rating certificate | `<CONTACT EMAIL>` | IARC sends the certificate there. |
| Category | The non-game category closest to "utility, productivity, communication or other" | Attic is a media player. It isn't a game, social app, news app or store. The exact label wasn't verifiable outside the Console. |
| Violence | No | The app ships no media of its own. |
| Fear or horror | No | As above. |
| Sexual content or nudity | No | As above. |
| Profanity or crude humour | Yes | Attic shows lyrics for the user's songs, downloaded unfiltered from LRCLIB, and they may contain strong language; see the note below. |
| Controlled substances (drugs, alcohol, tobacco) | No | |
| Gambling or simulated gambling | No | |
| Users can interact or exchange content with each other | No | There are no users, accounts, chat or comments. Sharing a Recap card hands an image to another app through Android's share sheet; Attic itself doesn't send it anywhere. |
| Shares the user's location with other users | No | Attic has no location permission. |
| Allows purchases of digital goods | No | |
| Unrestricted internet access, such as a web browser or search engine | No | The only network use is fetching lyrics from one service, lrclib.net. |
| Is the app a web browser or search engine | No | |

Expected result: around Teen / PEGI 12, or the local equivalent.

Note on lyrics: Attic can't know whether a downloaded lyric is explicit, so profanity is answered
Yes, the way music apps that show lyrics usually declare possible explicit content. Answering No
would risk a rating that a reviewer later raises. `COMPLIANCE.md` gives the same answer.

### Target audience and content

| Field | Answer | Why |
|---|---|---|
| Target age groups | **13–15, 16–17 and 18 and over** | Leaving out every group under 13 keeps Attic out of the Families policy (its ads, data and age-screen rules). Nothing in Attic is unsuitable for teens, and the expected Teen / PEGI 12 rating fits these groups. `COMPLIANCE.md` gives the same answer. |
| Could the app unintentionally appeal to children | No | No cartoon characters, games or child-themed content. |

### News apps

**No.** Attic isn't a news app.

### COVID-19 contact tracing and status apps

**My app is not a publicly available COVID-19 contact tracing or status app.**

### Data safety

Use the answers in `data-safety.md` in this folder, which is written from the code. In short, the
lyrics request sends song metadata to lrclib.net when the user asks for lyrics, nothing is
collected by the developer, and data is encrypted in transit (HTTPS).

### Government apps

**No.** Attic isn't developed by or for a government.

### Financial features

**My app doesn't provide any financial features.**

### Health apps

**My app does not have any health features.**

### Advertising ID

**No.** Attic doesn't use the advertising ID, and its manifest doesn't request
`com.google.android.gms.permission.AD_ID`.

### Foreground service permissions

Attic declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (playback with the screen off) and
`FOREGROUND_SERVICE_DATA_SYNC` (song analysis with its progress notification). Play asks for a
description and a short video for each. Use the wording and video plan in `COMPLIANCE.md`.

## Pricing and distribution

| Field | Answer |
|---|---|
| Countries | All available countries |
| Contains ads | No |
| In-app purchases | None |
| Devices | Phones. The release build is 64-bit ARM only (`arm64-v8a`), so Play hides it from other devices automatically. |

## Testing

New personal accounts need a closed test with at least 12 testers opted in for 14 days in a row
before production access. See `DISTRIBUTION.md` for the rule, its source and practical notes, and
for the signing key decision that has to be made before the first upload.
