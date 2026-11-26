import AtticCore
import AtticKit
import SwiftUI

/// One notice and its licence text, laid out as in ios/DESIGN.md.
struct LicenceScreen: View {
    let id: String

    @Environment(Router.self) private var router
    @Environment(\.atticScheme) private var scheme

    @State private var paragraphs: [String]?

    var body: some View {
        let model = makeLicenceModel(id: id, router: router)

        return VStack(spacing: 0) {
            DetailAppBar(model.state.notice.name, onBack: model.actions.back)
            ScrollView {
                LazyVStack(spacing: 0) {
                    if let paragraphs {
                        ForEach(paragraphs, id: \.self) { paragraph in
                            Text(paragraph)
                                .atticFont(.bodyMedium)
                                .foregroundStyle(scheme.onSurfaceVariant)
                                .textSelection(.enabled)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                // 16 at the sides, 6 above and below (DESIGN.md, Open-source licences).
                                .padding(.horizontal, AtticSpacing.screenMargin)
                                .padding(.vertical, 6)
                        }
                    } else {
                        LoadingState()
                            .frame(height: 200)
                    }
                }
            }
            .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
        }
        .toolbar(.hidden, for: .navigationBar)
        .task(id: id) {
            paragraphs = nil
            paragraphs = await model.actions.load()
        }
    }
}
