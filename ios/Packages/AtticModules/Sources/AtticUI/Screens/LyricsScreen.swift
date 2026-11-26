import AtticCore
import AtticKit
import SwiftUI

/// The lyrics sheet, on the cover's scheme. The model is built from the app and router; the view only
/// draws its state and calls its actions.
struct LyricsScreen: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @State private var phase: LyricsState.Phase = .loading

    var body: some View {
        let model = makeLyricsModel(app: app, router: router, phase: $phase)
        LyricsSheet(state: model.state, actions: model.actions)
            .fromCover(model.state.track)
            .presentationDetents([.large])
            .presentationDragIndicator(.hidden)
            .presentationCornerRadius(AtticRadius.extraLarge)
    }
}

private struct LyricsSheet: View {
    let state: LyricsState
    let actions: LyricsActions

    @Environment(\.atticScheme) private var scheme
    @State private var scrubMs: Int64?
    @State private var lastUserScroll = Date.distantPast

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.2)) { _ in
            let shownMs = scrubMs ?? actions.positionMs()
            VStack(spacing: 0) {
                topBar
                bodyView(positionMs: shownMs)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .background(scheme.surface)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                bottomBar(positionMs: shownMs)
                    .background {
                        UnevenRoundedRectangle(
                            topLeadingRadius: AtticRadius.extraLarge,
                            topTrailingRadius: AtticRadius.extraLarge
                        )
                        .fill(scheme.surfaceContainer)
                        .ignoresSafeArea(edges: .bottom)
                    }
            }
        }
        .presentationBackground(scheme.surface)
        .toolbar(.hidden, for: .navigationBar)
        .task(id: state.track?.key) { await actions.load() }
    }

    private var topBar: some View {
        VStack(spacing: 0) {
            SheetHandle()
            HStack(spacing: AtticSpacing.xs) {
                AtticIconButton(.keyboardArrowDown, label: S.lyricsCloseCd, action: actions.close)
                VStack(alignment: .leading, spacing: 0) {
                    Text(state.track?.title ?? "")
                        .atticFont(.titleMedium.weight(.semibold).lineHeight(22))
                        .foregroundStyle(scheme.onSurface)
                        .lineLimit(1)
                    Text(state.track?.artist ?? "")
                        .atticFont(.bodySmall)
                        .foregroundStyle(scheme.onSurfaceVariant)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
            }
            .padding(.leading, AtticSpacing.s)
            .padding(.trailing, AtticSpacing.l)
            .frame(height: AtticSpacing.appBarHeight)
        }
        .background {
            UnevenRoundedRectangle(
                bottomLeadingRadius: AtticRadius.extraLarge,
                bottomTrailingRadius: AtticRadius.extraLarge
            )
            .fill(scheme.surfaceContainer)
            .ignoresSafeArea(edges: .top)
        }
    }

    @ViewBuilder
    private func bodyView(positionMs: Int64) -> some View {
        switch state.phase {
        case .loading:
            noLyrics(message: S.lyricsNotDownloaded, buttonSymbol: nil, loading: true)
        case .synced(let lines):
            syncedLines(lines, positionMs: positionMs)
        case .plain(let paragraphs):
            plainLyrics(paragraphs)
        case .instrumental:
            noLyrics(message: S.lyricsInstrumental, buttonSymbol: nil, loading: false)
        case .notFound:
            noLyrics(message: S.lyricsNotFound, buttonSymbol: nil, loading: false)
        case .notDownloaded:
            noLyrics(message: S.lyricsNotDownloaded, buttonSymbol: .download, loading: false)
        case .failed:
            noLyrics(message: S.lyricsNotDownloaded, buttonSymbol: .refresh, loading: false)
        }
    }

    private func syncedLines(_ lines: [AtticCore.LyricLine], positionMs: Int64) -> some View {
        let current = LRCParser.activeIndex(in: lines, at: positionMs)
        return ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(lines.indices, id: \.self) { index in
                        let line = lines[index]
                        SyncedLyricLine(
                            line.text.isEmpty ? S.lyricBlankLine : line.text,
                            state: lineState(index: index, current: current),
                            action: { actions.seek(line.timeMs) }
                        )
                        .id(index)
                    }
                }
                .padding(.horizontal, AtticSpacing.playerMargin)
                // DESIGN.md, Screens → Lyrics: the lines start 22pt under the top bar.
                .padding(.top, 22)
                // Room below the last lines so they can still reach the anchor.
                .padding(.bottom, 220)
            }
            .simultaneousGesture(DragGesture().onChanged { _ in
                lastUserScroll = Date()
            })
            .onChange(of: current) { _, index in
                guard let index, Date.now.timeIntervalSince(lastUserScroll) > 3 else { return }
                withAnimation(AtticMotion.defaultSpatial) {
                    proxy.scrollTo(index, anchor: UnitPoint(x: 0.5, y: 0.33))
                }
            }
        }
    }

    private func lineState(index: Int, current: Int?) -> LyricLineState {
        guard let current else { return .upcoming }
        if index == current { return .current }
        return index < current ? .sung : .upcoming
    }

    private func plainLyrics(_ paragraphs: [String]) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(paragraphs.indices, id: \.self) { index in
                    Text(paragraphs[index])
                        .atticFont(.headlineSmall.weight(.medium))
                        .foregroundStyle(scheme.onSurface)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        // DESIGN.md, Screens → Lyrics: 28pt between paragraphs.
                        .padding(.bottom, 28)
                }
            }
            .padding(.horizontal, AtticSpacing.playerMargin)
            // DESIGN.md, Screens → Lyrics: the lines start 22pt under the top bar.
            .padding(.top, 22)
        }
    }

    private func noLyrics(message: String, buttonSymbol: AtticSymbol?, loading: Bool) -> some View {
        VStack(spacing: 0) {
            ZStack {
                CloverShape().fill(scheme.secondaryContainer)
                SymbolIcon(.lyrics, size: 48)
                    .foregroundStyle(scheme.onSecondaryContainer)
            }
            .frame(width: 128, height: 128)
            .accessibilityHidden(true)
            .padding(.bottom, AtticSpacing.xl)
            Text(message)
                .atticFont(.bodyLarge)
                .foregroundStyle(scheme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.bottom, AtticSpacing.xl)
            if loading {
                LoadingIndicator()
            } else if let buttonSymbol {
                AtticButton(S.lyricsDownload, leading: buttonSymbol, leadingFilled: false, action: actions.retry)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding(.horizontal, AtticSpacing.xxxl)
        // DESIGN.md, Screens → Lyrics: the empty state starts 120pt under the bar.
        .padding(.top, 120)
    }

    private func bottomBar(positionMs: Int64) -> some View {
        let duration = Double(max(1, state.track?.durationMs ?? 0)) / 1000
        return HStack(spacing: AtticSpacing.m) {
            Text(Format.clock(ms: positionMs))
                .atticFont(.labelMedium.tabular())
                .foregroundStyle(scheme.onSurfaceVariant)
            ProgressSlider(
                position: Binding(
                    get: { Double(positionMs) / 1000 },
                    set: { scrubMs = Int64($0 * 1000) }
                ),
                duration: duration,
                isPlaying: state.isPlaying,
                label: S.nowPlayingSeekCd(state.track?.title ?? ""),
                showsTimes: false,
                onEditingChanged: { editing in
                    guard !editing, let value = scrubMs else { return }
                    actions.seek(value)
                    scrubMs = nil
                }
            )
            .frame(maxWidth: .infinity)
            Text(Format.clock(ms: state.track?.durationMs ?? 0))
                .atticFont(.labelMedium.tabular())
                .foregroundStyle(scheme.onSurfaceVariant)
            PlayButton(isPlaying: state.isPlaying, size: .large, action: actions.togglePlayPause)
                .padding(.leading, AtticSpacing.s)
        }
        .padding(.leading, AtticSpacing.playerMargin)
        .padding(.trailing, AtticSpacing.xl)
        // DESIGN.md, Screens → Lyrics: the bottom bar is 88pt tall above the home indicator.
        .frame(height: 88)
    }
}
