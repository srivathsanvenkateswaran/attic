import AtticCore
import AtticKit
import SwiftUI

/// The list of open-source notices, laid out as in ios/DESIGN.md.
struct LicencesScreen: View {
    @Environment(Router.self) private var router

    @State private var collapsed = false

    var body: some View {
        let model = makeLicencesModel(router: router)

        return VStack(spacing: 0) {
            DetailAppBar(
                collapsed ? S.settingsLicences : "",
                surface: collapsed ? .scrolled : .plain,
                onBack: model.actions.back
            )
            ScrollView {
                LazyVStack(spacing: 0) {
                    LargeTitle(S.settingsLicences)
                    ListGroup {
                        ForEach(model.state.notices) { notice in
                            SettingsNavigationRow(title: notice.name, summary: notice.licence) {
                                model.actions.open(notice.id)
                            }
                        }
                    }
                }
            }
            .onScrollOffset { offset in
                // The large title's 44pt line has gone (DESIGN.md, Open-source licences).
                collapsed = offset > 44
            }
            .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
        }
        .toolbar(.hidden, for: .navigationBar)
    }
}
