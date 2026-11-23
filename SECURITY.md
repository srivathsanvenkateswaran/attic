# Security policy

## Supported versions

Only the latest release gets security fixes. Attic has no server, so a fix ships as a new release
on Google Play and GitHub Releases.

## Reporting a vulnerability

Report it privately through GitHub: open the repository's **Security** tab and click **Report a
vulnerability**. Please don't open a public issue for it.

If that button isn't there, write to [CONTACT EMAIL: the maintainer adds this before launch].

Include the Attic version, your phone and Android version, and the steps that show the problem.
You should hear back within 7 days. Once a fix is out, the release notes credit you unless you
ask not to be named.

## What counts

Attic has no account and keeps your data on the phone, so the parts most worth a look are:

- anything that lets another app read Attic's data, its listening log or its saved lyrics
- the exported playback service and media button receiver
- the lyrics download from lrclib.net, including how responses are parsed
- the `language-labels.tsv` import from Attic's own files folder
- Recap images handed to other apps through the share sheet

A problem in LRCLIB itself belongs with [LRCLIB](https://lrclib.net).
