import AtticCore
import AtticKit
import SwiftUI

/// The queue sheet, on the current song's cover scheme from the outside in.
struct QueueScreen: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router

    var body: some View {
        let model = makeQueueModel(app: app, router: router)
        QueueSheet(state: model.state, actions: model.actions)
            .fromCover(model.state.current)
    }
}

private struct QueueSheet: View {
    @Environment(\.atticScheme) private var scheme
    let state: QueueState
    let actions: QueueActions

    var body: some View {
        VStack(spacing: 0) {
            SheetHandle()
            bar
            list
        }
        .background(scheme.surface)
        .toolbar(.hidden, for: .navigationBar)
        .presentationDetents([.large])
        .presentationDragIndicator(.hidden)
        .presentationCornerRadius(AtticRadius.extraLarge)
        .presentationBackground(scheme.surface)
    }

    private var bar: some View {
        HStack(spacing: 4) {
            AtticIconButton(.keyboardArrowDown, label: S.queueCloseCd, action: actions.close)
            VStack(alignment: .leading, spacing: 0) {
                Text(S.queueTitle)
                    .atticFont(.titleLargeEmphasized)
                    .foregroundStyle(scheme.onSurface)
                    .lineLimit(1)
                    .accessibilityAddTraits(.isHeader)
                HStack(spacing: 6) {
                    if let symbol = state.contextSymbol {
                        SymbolIcon(symbol, size: 16)
                            .foregroundStyle(scheme.primary)
                    }
                    Text(state.contextLabel)
                        .atticFont(.labelMedium)
                        .foregroundStyle(scheme.onSurfaceVariant)
                        .lineLimit(1)
                }
                .accessibilityElement(children: .combine)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 8)
        .frame(height: AtticSpacing.appBarHeight)
    }

    private var list: some View {
        List {
            if let current = state.current {
                ListLabel(S.queueNowPlaying)
                ArtworkImage(track: current, maxPixel: 144) { image in
                    SongRow(
                        title: current.title,
                        subtitle: current.artist,
                        lead: .cover(image),
                        isCurrent: true,
                        isPlaying: state.isPlaying
                    ) {
                        RowMoreMenu(title: current.title) { SongMenuItems(track: current) }
                    }
                }
                .groupCell(index: 0, count: 1)
            }

            if state.upNext.isEmpty {
                EmptyState(title: S.queueNothingUpNext, body: nil, symbol: .queueMusic, clover: true)
                    .padding(.top, 48)
            } else {
                HStack(alignment: .bottom, spacing: 8) {
                    ListLabel(state.upNextLabel)
                    AtticButton(S.queueClear, variant: .text) { actions.clearUpNext() }
                }
                .padding(.trailing, 20)

                ForEach(Array(state.upNext.enumerated()), id: \.element.id) { position, row in
                    ArtworkImage(track: row.track, maxPixel: 144) { image in
                        SongRow(
                            title: row.track.title,
                            subtitle: row.track.artist,
                            lead: .cover(image),
                            action: { actions.jumpTo(row.queueIndex) }
                        ) {
                            RowDragHandle()
                        }
                    }
                    .groupCell(index: position, count: state.upNext.count)
                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                        Button(role: .destructive) { actions.remove(row.queueIndex) } label: {
                            SymbolIcon(.delete)
                                .foregroundStyle(scheme.error)
                        }
                        .tint(scheme.error)
                        .accessibilityLabel(S.queueRemove)
                    }
                }
                .onMove { from, to in
                    guard let source = from.first,
                          state.upNext.indices.contains(source), state.upNext.indices.contains(to) else { return }
                    actions.move(state.upNext[source].queueIndex, to)
                }
            }

            // 40pt under the last row (DESIGN.md, Queue).
            Color.clear.frame(height: 40)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets())
        .listRowBackground(Color.clear)
        .groupCellColor(scheme.surfaceContainerHigh)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
