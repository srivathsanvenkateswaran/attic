import AtticCore
import AtticKit
import SwiftUI

/// The full-screen player. It draws `NowPlayingState` and calls `NowPlayingActions`; the logic is in
/// `Models/NowPlayingModel.swift`. Layout numbers are from ios/DESIGN.md, Screens, Now Playing.
struct NowPlayingScreen: View {

    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    /// The lyrics of one song, keyed so a late answer for the previous song is never shown.
    @State private var loaded: (key: String, document: LyricsDocument)?

    var body: some View {
        let track = app.player.state.current
        let document = loaded.flatMap { $0.key == track?.key ? $0.document : nil }
        let model = makeNowPlayingModel(app: app, router: router, lyrics: document)
        NowPlayingPage(state: model.state, actions: model.actions)
            .fromCover(track)
            // The page follows a downward drag, so the cover must not paint a background of its own.
            .presentationBackground(.clear)
            .task(id: track?.key) {
                guard let track, let found = await app.lyrics(for: track) else { return }
                loaded = (track.key, found)
            }
    }
}

/// Read inside `.fromCover`, so `scheme` is the cover's scheme.
private struct NowPlayingPage: View {
    let state: NowPlayingState
    let actions: NowPlayingActions

    @Environment(\.atticScheme) private var scheme
    /// How far the page has been pulled down.
    @State private var dragY: CGFloat = 0
    @State private var screenHeight: CGFloat = 900
    @State private var dragAxis: Axis?
    @State private var coverX: CGFloat = 0

    // DESIGN.md, Now Playing: close past 160pt of drag or faster than 800pt/s.
    private static let closeDistance: CGFloat = 160
    private static let closeVelocity: CGFloat = 800
    private static let pausedScale: CGFloat = 0.86

    var body: some View {
        GeometryReader { geometry in
            let shortPhone = screenHeight < 700
            VStack(spacing: 0) {
                if let track = state.track {
                    header(track)
                    coverSlot(track, minGap: shortPhone ? AtticSpacing.s : AtticSpacing.l)
                    titleBlock(track)
                    // DESIGN.md, Now Playing: progress 10 down, transport 22, tools 20.
                    NowPlayingProgress(state: state, actions: actions, title: track.title)
                        .padding(.horizontal, AtticSpacing.playerMargin)
                        .padding(.top, 10)
                    Transport(
                        isPlaying: state.isPlaying,
                        onPrevious: actions.previous,
                        onPlayPause: actions.togglePlayPause,
                        onNext: actions.next
                    )
                    .padding(.top, 22)
                    tools
                        .padding(.horizontal, AtticSpacing.playerMargin)
                        .padding(.top, AtticSpacing.xl)
                        .padding(.bottom, AtticSpacing.l)
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
        }
        .background(scheme.surface.ignoresSafeArea())
        .offset(y: dragY)
        .onGeometryChange(for: CGFloat.self) {
            $0.size.height + $0.safeAreaInsets.top + $0.safeAreaInsets.bottom
        } action: { screenHeight = $0 }
        .onChange(of: state.track == nil) { _, gone in
            if gone { actions.close() }
        }
    }

    // MARK: Header

    private func header(_ track: Track) -> some View {
        HStack(spacing: AtticSpacing.xs) {
            AtticIconButton(.keyboardArrowDown, label: S.nowPlayingCloseCd, action: dismiss)
            VStack(alignment: .leading, spacing: 0) {
                Text(S.nowPlayingPlayingFrom)
                    .atticFont(.labelMedium)
                    .foregroundStyle(scheme.onSurfaceVariant)
                HStack(spacing: 6) {
                    if let glyph = contextGlyph {
                        SymbolIcon(glyph, size: 18).foregroundStyle(scheme.primary)
                    }
                    Text(state.contextName)
                        .atticFont(.titleSmall.weight(.semibold).tracking(0))
                        .foregroundStyle(scheme.onSurface)
                }
                .lineLimit(1)
            }
            .accessibilityElement(children: .combine)
            Spacer(minLength: 0)
            RowMoreMenu(title: track.title) { SongMenuItems(track: track) }
        }
        .padding(.horizontal, AtticSpacing.s)
        .frame(height: AtticSpacing.appBarHeight)
        .contentShape(Rectangle())
        .gesture(pageDrag)
    }

    private var contextGlyph: AtticSymbol? {
        switch state.contextKind {
        case .album: .album
        case .artist: .person
        case .folder: .folder
        case .playlist: .playlistPlay
        case .favourites: .favorite
        case .other: nil
        }
    }

    // MARK: Cover

    /// Takes all the room the other rows leave. The cover is as wide as the screen less 48 and never
    /// taller than the slot less the minimum gap; what is left over under it is the flexible gap.
    private func coverSlot(_ track: Track, minGap: CGFloat) -> some View {
        GeometryReader { slot in
            let side = max(0, min(slot.size.width - 2 * AtticSpacing.playerMargin, slot.size.height - minGap))
            ArtworkImage(track: track, maxPixel: 1000) { image in
                Cover(image, shape: .rounded(state.isPlaying ? AtticRadius.extraLarge : AtticRadius.extraExtraLarge))
            }
            .frame(width: side, height: side)
            .scaleEffect(state.isPlaying ? 1 : Self.pausedScale)
            .animation(AtticMotion.fastSpatial, value: state.isPlaying)
            .offset(x: coverX)
            .contentShape(Rectangle())
            .gesture(coverDrag)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .padding(.top, AtticSpacing.xs)
    }

    // MARK: Title block

    private func titleBlock(_ track: Track) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            if state.hasSyncedLyrics {
                NowPlayingLyricRow(state: state, positionMs: actions.positionMs)
            }
            NarrowingText(track.title, style: .headlineLargeEmphasized.tracking(-0.3))
                .foregroundStyle(scheme.onSurface)
            artist(track)
                .padding(.top, AtticSpacing.xxs)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, AtticSpacing.playerMargin)
    }

    @ViewBuilder
    private func artist(_ track: Track) -> some View {
        let name = Text(track.artist)
            .atticFont(.bodyLarge)
            .foregroundStyle(scheme.onSurfaceVariant)
            .lineLimit(1)
        if let open = actions.openArtist {
            Button(action: open) { name }
                .buttonStyle(.plain)
                // A 48pt target without taking 48pt of layout.
                .padding(.vertical, 12)
                .contentShape(Rectangle())
                .padding(.vertical, -12)
        } else {
            name
        }
    }

    // MARK: Tools

    private var tools: some View {
        ToolsRow {
            ShuffleToggle(isOn: state.shuffle, action: actions.toggleShuffle)
            RepeatToggle(mode: state.repeatMode, action: actions.cycleRepeat)
            HeartButton(isFavourite: state.isLiked, action: actions.toggleLike)
            AtticIconButton(.lyrics, label: S.lyricsTitle, action: actions.openLyrics)
            AtticIconButton(.queueMusic, label: S.nowPlayingOpenQueueCd, action: actions.openQueue)
        }
    }

    // MARK: Closing and skipping

    /// The page is dragged in global coordinates because it moves under the finger.
    private var pageDrag: some Gesture {
        DragGesture(minimumDistance: 8, coordinateSpace: .global)
            .onChanged { dragY = max(0, $0.translation.height) }
            .onEnded { finishPull($0) }
    }

    /// Down closes the page; left and right skip. The first movement decides which.
    private var coverDrag: some Gesture {
        DragGesture(minimumDistance: 8, coordinateSpace: .global)
            .onChanged { value in
                let axis = dragAxis ?? (abs(value.translation.width) > abs(value.translation.height) ? .horizontal : .vertical)
                dragAxis = axis
                if axis == .vertical {
                    dragY = max(0, value.translation.height)
                } else {
                    coverX = value.translation.width * 0.4
                }
            }
            .onEnded { value in
                let axis = dragAxis
                dragAxis = nil
                if axis == .vertical {
                    finishPull(value)
                    return
                }
                withAnimation(AtticMotion.defaultSpatial) { coverX = 0 }
                // The skip needs a clear flick: 60pt, or a fast short one.
                let far = abs(value.translation.width) > 60 || abs(value.velocity.width) > 500
                guard far else { return }
                if value.translation.width < 0 { actions.next() } else { actions.previous() }
            }
    }

    private func finishPull(_ value: DragGesture.Value) {
        if value.translation.height > Self.closeDistance || value.velocity.height > Self.closeVelocity {
            dismiss()
        } else {
            withAnimation(AtticMotion.defaultSpatial) { dragY = 0 }
        }
    }

    /// Slides the page off the bottom, then closes with no second animation.
    private func dismiss() {
        withAnimation(AtticMotion.defaultSpatial) {
            dragY = screenHeight
        } completion: {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { actions.close() }
        }
    }
}

/// The line being sung: a 20pt row with 10pt under it. Its space stays while a song with synced lyrics
/// is between lines, so the title does not jump.
private struct NowPlayingLyricRow: View {
    let state: NowPlayingState
    let positionMs: () -> Int64
    @Environment(\.atticScheme) private var scheme

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.25)) { _ in
            let line = state.lyricLine(atMs: positionMs())
            HStack(spacing: AtticSpacing.s) {
                SymbolIcon(.lyrics, size: 18)
                Text(line ?? "")
                    .atticFont(.titleSmall.weight(.semibold))
                    .lineLimit(1)
            }
            .foregroundStyle(scheme.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(height: 20)
            .opacity(line == nil ? 0 : 1)
        }
        .padding(.bottom, 10)
        .accessibilityHidden(true)
    }
}

/// The slider, fed by the player's clock four times a second. While a finger is on it the slider holds
/// its own value; the seek happens on release, and the value is held a moment longer so the handle
/// does not jump back before the player has caught up.
private struct NowPlayingProgress: View {
    let state: NowPlayingState
    let actions: NowPlayingActions
    let title: String

    @State private var isEditing = false
    @State private var held: Double = 0
    @State private var holdUntil = Date.distantPast

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.25)) { context in
            let holding = isEditing || context.date < holdUntil
            ProgressSlider(
                position: Binding(
                    get: { holding ? held : Double(actions.positionMs()) / 1000 },
                    set: { held = $0 }
                ),
                duration: Double(state.durationMs) / 1000,
                isPlaying: state.isPlaying,
                label: S.nowPlayingSeekCd(title),
                onEditingChanged: { began in
                    if began {
                        held = Double(actions.positionMs()) / 1000
                        isEditing = true
                    } else {
                        isEditing = false
                        holdUntil = Date().addingTimeInterval(0.5)
                        actions.seek(Int64(held * 1000))
                    }
                }
            )
        }
    }
}
