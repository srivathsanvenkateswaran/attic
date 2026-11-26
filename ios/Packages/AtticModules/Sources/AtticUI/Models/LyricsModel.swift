import AtticCore
import Foundation
import SwiftUI

/// What the lyrics sheet shows for the song that is playing.
struct LyricsState {
    enum Phase: Equatable {
        case loading
        case synced([LyricLine])
        case plain([String])
        case instrumental
        case notFound
        /// Nothing cached yet and no lookup tried.
        case notDownloaded
        /// The lookup threw, so the phone was probably offline.
        case failed
    }

    var track: Track?
    var phase: Phase
    var isPlaying: Bool
}

struct LyricsActions {
    var close: () -> Void
    var seek: (Int64) -> Void
    var togglePlayPause: () -> Void
    var retry: () -> Void
    /// The player's clock, polled by the screen's timeline.
    var positionMs: () -> Int64
    /// Reads the cache, then looks the song up once if nothing is saved.
    var load: () async -> Void
}

@MainActor
func makeLyricsModel(
    app: AppModel,
    router: Router,
    phase: Binding<LyricsState.Phase>
) -> (state: LyricsState, actions: LyricsActions) {
    let track = app.player.state.current
    let state = LyricsState(
        track: track,
        phase: phase.wrappedValue,
        isPlaying: app.player.state.isPlaying
    )

    func show(_ document: LyricsDocument) {
        phase.wrappedValue = LyricsState.phase(for: document)
    }

    func lookup(_ track: Track) async {
        phase.wrappedValue = .loading
        let key = track.key
        let document = await app.lyrics(for: track)
        // A song may change while the request is in flight.
        guard app.player.state.current?.key == key else { return }
        if let document { show(document) } else { phase.wrappedValue = .failed }
    }

    let actions = LyricsActions(
        close: { router.showLyrics = false },
        seek: { ms in app.seek(toMs: ms) },
        togglePlayPause: { app.player.togglePlayPause() },
        retry: {
            guard let track else { return }
            Task { await lookup(track) }
        },
        positionMs: { app.player.positionMs() },
        load: {
            guard let track else {
                phase.wrappedValue = .notDownloaded
                return
            }
            if let saved = app.store.lyrics(key: track.key) {
                show(saved)
            } else {
                await lookup(track)
            }
        }
    )
    return (state, actions)
}

private extension LyricsState {
    /// Unstored plain lines come in one per source line; group them into paragraphs on blank lines.
    static func phase(for document: LyricsDocument) -> Phase {
        switch document {
        case .synced(let lines): return .synced(lines)
        case .plain(let lines):
            var paragraphs: [String] = []
            var current: [String] = []
            for line in lines {
                let text = line.trimmingCharacters(in: .whitespaces)
                if text.isEmpty {
                    if !current.isEmpty {
                        paragraphs.append(current.joined(separator: "\n"))
                        current = []
                    }
                } else {
                    current.append(text)
                }
            }
            if !current.isEmpty { paragraphs.append(current.joined(separator: "\n")) }
            return .plain(paragraphs)
        case .instrumental: return .instrumental
        case .notFound: return .notFound
        }
    }
}
