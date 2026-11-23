#!/usr/bin/env python3
"""Writes the small audio files the lyrics tests read, into app/src/test/resources/lyrics/.

    python3 scripts/make_lyrics_fixtures.py

ffmpeg makes the audio. It can't write a real USLT or SYLT frame (it stores MP3 lyrics as a
TXXX frame), so the ID3 tags here are built by hand and put in front of ffmpeg's MPEG frames.
ffmpeg does write the MP4 ©lyr atom and Vorbis comments, so the M4A, FLAC and Opus files come
straight from it.
"""

import pathlib
import struct
import subprocess
import tempfile

OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/test/resources/lyrics"


def ffmpeg(args, out):
    subprocess.run(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
         "-f", "lavfi", "-i", "anullsrc=r=8000:cl=mono", "-t", "0.3", *args, str(out)],
        check=True,
    )


def mpeg_frames():
    """Silent MPEG audio with no ID3 tag of its own."""
    with tempfile.TemporaryDirectory() as tmp:
        path = pathlib.Path(tmp) / "a.mp3"
        ffmpeg(["-c:a", "libmp3lame", "-b:a", "8k", "-write_xing", "0", "-id3v2_version", "0"], path)
        return path.read_bytes()


def syncsafe(n):
    return bytes([(n >> 21) & 0x7F, (n >> 14) & 0x7F, (n >> 7) & 0x7F, n & 0x7F])


def frame(version, frame_id, body):
    size = syncsafe(len(body)) if version == 4 else struct.pack(">I", len(body))
    return frame_id.encode() + size + b"\0\0" + body


def tag(version, frames):
    body = b"".join(frames)
    return b"ID3" + bytes([version, 0, 0]) + syncsafe(len(body)) + body


def text(encoding, s):
    """A string and its terminator in an ID3 text encoding."""
    if encoding == 0:
        return s.encode("latin-1") + b"\0"
    if encoding == 1:
        return s.encode("utf-16") + b"\0\0"
    return s.encode("utf-8") + b"\0"


def uslt(encoding, lyrics, lang=b"eng", description=""):
    raw = text(encoding, lyrics)[: -2 if encoding == 1 else -1]
    return bytes([encoding]) + lang + text(encoding, description) + raw


def sylt(encoding, entries, lang=b"eng", description=""):
    """Timestamp format 2 is milliseconds; content type 1 is lyrics."""
    body = bytes([encoding]) + lang + bytes([2, 1]) + text(encoding, description)
    for words, ms in entries:
        body += text(encoding, words) + struct.pack(">I", ms)
    return body


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    audio = mpeg_frames()

    # ID3v2.3, UTF-16 with a BOM: plain lyrics only.
    (OUT / "uslt_plain_v23.mp3").write_bytes(
        tag(3, [
            frame(3, "TIT2", b"\0Fixture\0"),
            frame(3, "USLT", uslt(1, "Plain first\nPlain second — ünïcode")),
        ]) + audio
    )

    # ID3v2.4, UTF-8: an LRC document inside USLT, which counts as synced.
    (OUT / "uslt_lrc_v24.mp3").write_bytes(
        tag(4, [frame(4, "USLT", uslt(3, "[ar:Someone]\n[00:01.00]First line\n[00:02.50]Second line"))]) + audio
    )

    # ID3v2.3: SYLT and a plain USLT. SYLT should win. Line breaks start each line, as most taggers write them.
    (OUT / "sylt_v23.mp3").write_bytes(
        tag(3, [
            frame(3, "USLT", uslt(0, "Plain words")),
            frame(3, "SYLT", sylt(0, [("Hello", 1000), ("\nworld", 2500), (" again", 3000)])),
        ]) + audio
    )

    # No lyrics at all.
    (OUT / "no_lyrics.mp3").write_bytes(tag(3, [frame(3, "TIT2", b"\0Fixture\0")]) + audio)

    # ffmpeg's own MP3 lyrics: a TXXX frame described "USLT".
    ffmpeg(["-c:a", "libmp3lame", "-b:a", "8k", "-metadata", "lyrics=Txxx first\nTxxx second"], OUT / "txxx.mp3")

    # M4A with ©lyr. ffmpeg puts moov after mdat, so reading it needs a seek; the faststart copy doesn't.
    lyrics = "-metadata", "lyrics=[00:03.20]Mp4 line\n[00:04.00]Mp4 next"
    ffmpeg(["-c:a", "aac", "-b:a", "8k", *lyrics], OUT / "lyr_moov_last.m4a")
    ffmpeg(["-c:a", "aac", "-b:a", "8k", "-movflags", "+faststart", "-metadata", "lyrics=Plain mp4 words"],
           OUT / "lyr_faststart.m4a")

    # Vorbis comments: ffmpeg writes the lyrics key as LYRICS.
    ffmpeg(["-c:a", "flac", "-metadata", "lyrics=[00:00.50]Flac line"], OUT / "lyrics.flac")
    ffmpeg(["-c:a", "libopus", "-b:a", "8k", "-metadata", "lyrics=Opus words"], OUT / "lyrics.opus")


if __name__ == "__main__":
    main()
