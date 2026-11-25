# Distributing Attic

Where Attic can be published besides the Play listing, what each channel needs, and what stands in
the way. Rules were read on 2026-11-23; each section links its source. The Play compliance work
(signing, foreground services, data safety) is in `COMPLIANCE.md` and `data-safety.md` in this
folder.

## Settle before publishing anywhere

**The song analysis model's licence.** `app/src/main/assets/discogs-effnet-bsdynamic-1.onnx` is
under CC BY-NC-SA 4.0 according to Essentia's models page and CC BY-NC-ND 4.0 according to MTG's
licensing page (`third_party/discogs-effnet-NOTICE.txt`). Both forbid commercial use. Attic is free
and has no ads, but Play is a commercial store, and the two pages disagree on whether derivatives
are allowed. Asking MTG for written confirmation that a free, ad-free app on Play and F-Droid counts
as non-commercial use would settle it for every channel below. It also decides F-Droid inclusion.

**One signing key, or two apps.** With Play App Signing, Google re-signs what users download from
Play. If Google generates that key, an APK from GitHub Releases is signed with a different key, and
Android refuses to update one with the other: someone who installs from GitHub has to uninstall,
losing favourites, playlists and the listening log, before installing from Play. Play's own advice
for this case is to "generate the app signing key you want to use for all app stores and transfer a
copy of it to Google when you configure Play App Signing"
([Use Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)).
New apps get a Google-generated key by default, which can be changed only "before a release is
rolled out in the open testing or production track". Decide before the first upload, so closed
testers don't have to reinstall either.

## Google Play

New personal developer accounts must run a closed test before they can publish to production:

> a minimum of 12 testers who have been opted in continuously for at least 14 days

This applies to personal accounts created after 13 November 2023. It used to be 20 testers. After
the 14 days, apply for production access from the Dashboard; Play asks how the test went and what
changed. Source:
[App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465).

Practical notes:

- Testers have to stay opted in for the whole 14 days. A tester who leaves on day 10 doesn't count,
  so recruit 15 to 20.
- Testers need a Google account and an Android phone. Attic's release build runs on 64-bit ARM
  phones with Android 13 or newer, so check that before inviting someone.
- Use the AAB that `.github/workflows/release.yml` keeps as a workflow artifact.
- `docs/play/CONSOLE.md` has the answers for the Console forms.

Store assets are in `fastlane/metadata/android/en-US/`, and `fastlane supply` can upload them. The
phone, 7-inch and 10-inch tablet screenshots are under `images/`; the READMEs in
`images/phoneScreenshots/` and `images/tenInchScreenshots/` say how they are made.

## F-Droid (main repository)

[Inclusion policy](https://f-droid.org/docs/Inclusion_Policy/):
"All applications in the repository must be Free, Libre and Open Source Software (FLOSS)", built
from source with a FLOSS toolchain. Tracking, ads and proprietary analytics are forbidden. F-Droid
builds and signs the APK itself.

What Attic already meets:

- GPL-3.0-or-later source, a Gradle build, no Google Play Services, Firebase or analytics.
- Fastlane metadata in the layout F-Droid reads
  ([descriptions, graphics and screenshots](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)).
  F-Droid allows a 50-character title, so the 30-character Play title fits.
- Lyrics come from LRCLIB, whose server is open source under the MIT licence
  ([tranxuanthang/lrclib](https://github.com/tranxuanthang/lrclib)). The NonFreeNet anti-feature
  covers apps that "promote or depend entirely on a proprietary network service", so it shouldn't
  apply. That is our reading; the reviewer decides.

What will come up:

- **The model is the blocker.** The [anti-features page](https://f-droid.org/docs/Anti-Features/)
  defines NonFreeAssets as assets "under a license that restricts commercial usage or making
  derivative works", naming any Creative Commons licence with NC or ND. Anti-features label an app
  "without necessarily disqualifying" it, and the inclusion policy lets "non-functional assets
  including artwork and fonts" use such licences if they allow redistribution. A neural network
  that drives radio is functional, not artwork, so the model may not be accepted at all. Ask in the
  request before building anything. The fallback is an F-Droid build flavour without the model,
  where radio and instant mixes are hidden and the rest of the app works; that is app code and
  hasn't been written.
- **Prebuilt native code.** `com.microsoft.onnxruntime:onnxruntime-android` is MIT-licensed but
  ships prebuilt `.so` files from Maven Central. F-Droid's scanner and reviewers sometimes ask for
  such libraries to be built from source. Expect the question.
- **Toolchain.** The build needs AGP 9.4, Kotlin 2.4 and compileSdk 37. F-Droid's build server
  has to have those available.

How to submit ([quick start guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)):
fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata), add `metadata/app.attic.player.yml`, run
`fdroid lint`, `fdroid rewritemeta` and `fdroid build`, and open a merge request titled
"New App: app.attic.player". Or file a request in the [RFP tracker](https://gitlab.com/fdroid/rfp/issues)
and let a packager do it. Either way, mention the model licence up front. A starting point for the
recipe, to be checked with `fdroid lint`:

```yaml
Categories:
  - Multimedia
License: GPL-3.0-or-later
SourceCode: https://github.com/srivathsanvenkateswaran/attic
IssueTracker: https://github.com/srivathsanvenkateswaran/attic/issues
AntiFeatures:
  NonFreeAssets:
    en-US: The song analysis model (Discogs-EffNet) is licensed for non-commercial use only.

RepoType: git
Repo: https://github.com/srivathsanvenkateswaran/attic.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

Because F-Droid signs with its own key, an F-Droid install and a Play or GitHub install can't
update each other, unless the build is reproducible and F-Droid is set up to ship the developer's
signature. That is optional and can come later.

## IzzyOnDroid

IzzyOnDroid serves APKs that developers build themselves, taken from GitHub Releases, so it reuses
the release workflow. Its [inclusion policy](https://izzyondroid.org/docs/general/AppInclusionPolicy/)
is a poor fit for Attic today.

- **AI-written code.** The policy says: "We are strongly opposed to apps which are fully or in part
  created by generative AI tools", "Vibe-coded apps will be rejected", and that for documentation
  it is acceptable but "the code itself should be free of it". It adds that "any lack of
  transparency discovered by us, can lead to the project being degraded to rejected state". Attic's
  code has been written with AI coding agents, so under this policy a request would most likely be
  rejected, and it must not be submitted without saying so.
- **Size.** IzzyOnDroid "usually reserves up to 30 megabytes per app", which is also the limit for
  a single APK. Attic's release APK is 64 MB, nearly all of it the analysis model, its runtime and the mood classifiers.

If you still want to ask, request inclusion at
[IzzyOnDroid/repodata issues](https://codeberg.org/IzzyOnDroid/repodata/issues), state how the code
was written, and ask about the size. Its updater checks the source repository daily for new tagged
releases ([FAQ](https://izzyondroid.org/faq/)). Tags must match the app's version, which brings up
the next point.

## GitHub Releases

`.github/workflows/release.yml` runs on a tag like `v1.0.0`. It decodes the upload keystore from
the `ATTIC_UPLOAD_*` secrets, builds the APK and AAB, refuses to publish an APK signed with the
debug key, and attaches `attic-v1.0.0.apk` and its SHA-256 checksum to a release whose notes come
from that version's section of `CHANGELOG.md`.

Before the first tag:

- **Version name.** `app/build.gradle.kts` has `versionName = "1.0"`, while the README, the
  changelog and the tag say 1.0.0. Set `versionName = "1.0.0"` so the three agree; F-Droid's and
  IzzyOnDroid's updaters match tags to it. The release workflow warns if they differ.
- **Signing config.** The release build type still uses the debug key. Until the signing config
  reads the `ATTIC_UPLOAD_*` values, the release workflow stops at its debug-key check, which is
  the intended behaviour.
- **Secrets.** Add the four secrets under Settings > Secrets and variables > Actions. The
  keystore goes in as base64: `base64 -i upload.jks | pbcopy` on a Mac.
- **Which key.** See "One signing key, or two apps" above.

Then:

```bash
git tag -a v1.0.0 -m "Attic 1.0.0"
git push origin v1.0.0
```

## Android developer verification

Google now verifies developers and registers package names and signing keys. From 30 September
2026 this applies to "users installing apps from participating stores in select regions on
certified Android devices" in Brazil, Indonesia, Singapore and Thailand, and it is due to expand
globally in 2027. Direct sideloads and `adb` installs aren't covered yet. For open-source
repositories, Google's guide says you "don't have to register your app" to keep distributing
there; registering keeps the install experience unchanged, and where a repository re-signs the
APK, as F-Droid does, it is the repository's key that gets registered.
Sources: [developer verification](https://developer.android.com/developer-verification) and its
[open-source registration guide](https://developer.android.com/developer-verification/guides/open-source-app-registration),
read 2026-11-23.
