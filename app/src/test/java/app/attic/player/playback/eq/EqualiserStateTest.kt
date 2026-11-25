package app.attic.player.playback.eq

import app.attic.player.data.EQ_SCOPE_ALBUM
import app.attic.player.data.EQ_SCOPE_SONG
import app.attic.player.data.EqSettingEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class EqualiserStateTest {
    private val default = EqPresets.byId(EqPresets.LOUDNESS)!!.curve
    private val albumCurve = EqPresets.byId(EqPresets.BASS_BOOST)!!.curve
    private val songCurve = EqPresets.byId(EqPresets.VOCAL)!!.curve

    private val state = EqualiserState(
        on = true,
        default = default,
        songs = mapOf("Music/Monsoon/01 Rain.mp3" to songCurve),
        albums = mapOf("monsoon tapes|kavya" to albumCurve),
    )

    @Test
    fun aSongsOwnCurveComesFirst() {
        assertEquals(ResolvedEq(songCurve, EqScope.Song), state.resolve("Music/Monsoon/01 Rain.mp3", "monsoon tapes|kavya"))
    }

    @Test
    fun thenItsAlbums() {
        assertEquals(ResolvedEq(albumCurve, EqScope.Album), state.resolve("Music/Monsoon/02 Tide.mp3", "monsoon tapes|kavya"))
    }

    @Test
    fun thenTheDefault() {
        assertEquals(ResolvedEq(default, EqScope.Default), state.resolve("Music/Other.mp3", "night bus|"))
        assertEquals(ResolvedEq(default, EqScope.Default), state.resolve("Music/Other.mp3", null))
        // A file opened from another app has no library key or album.
        assertEquals(ResolvedEq(default, EqScope.Default), state.resolve(null, null))
    }

    @Test
    fun removingTheSongsCurveFallsBackToTheAlbum() {
        val after = state.copy(songs = state.songs - "Music/Monsoon/01 Rain.mp3")
        assertEquals(EqScope.Album, after.resolve("Music/Monsoon/01 Rain.mp3", "monsoon tapes|kavya").scope)
    }

    @Test
    fun savedRowsAreSortedIntoSongsAndAlbumsAndBadOnesDropped() {
        val built = EqualiserState.of(
            on = false,
            default = EqCurve.FLAT,
            rows = listOf(
                EqSettingEntity(EQ_SCOPE_SONG, "a.mp3", songCurve.encode(), 1),
                EqSettingEntity(EQ_SCOPE_ALBUM, "a.mp3", albumCurve.encode(), 2),
                EqSettingEntity(EQ_SCOPE_SONG, "b.mp3", "not a curve", 3),
                EqSettingEntity("folder", "Music/", songCurve.encode(), 4),
            ),
        )
        assertEquals(mapOf("a.mp3" to songCurve), built.songs)
        assertEquals(mapOf("a.mp3" to albumCurve), built.albums)
    }
}
