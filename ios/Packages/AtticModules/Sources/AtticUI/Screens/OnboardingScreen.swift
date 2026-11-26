import AtticCore
import AtticKit
import SwiftUI
import UniformTypeIdentifiers

/// The first run: welcome, pick a folder, count its songs, then add the folder and let the shell scan it.
struct OnboardingScreen: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @Environment(\.atticScheme) private var scheme

    @State private var step: OnboardingStep = .welcome
    @State private var pickedURL: URL?
    @State private var foundCount: Int?
    @State private var isCounting = false
    @State private var showPicker = false

    var body: some View {
        let model = makeOnboardingModel(
            app: app,
            router: router,
            step: $step,
            pickedURL: $pickedURL,
            foundCount: $foundCount,
            isCounting: $isCounting,
            showPicker: $showPicker
        )

        VStack(spacing: 0) {
            ZStack {
                page(model.state, actions: model.actions)
                    .id(model.state.step)
                    .transition(.opacity)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            bottomButton(model.state, actions: model.actions)
        }
        .background(scheme.surface)
        .toolbar(.hidden, for: .navigationBar)
        .fileImporter(
            isPresented: $showPicker,
            allowedContentTypes: [.folder],
            allowsMultipleSelection: false
        ) { result in
            if case let .success(urls) = result, let url = urls.first {
                model.actions.picked(url)
            }
        }
    }

    @ViewBuilder
    private func page(_ state: OnboardingState, actions: OnboardingActions) -> some View {
        switch state.step {
        case .welcome:
            welcomePage()
        case .chooseFolder:
            folderPage(state, actions: actions)
        case .scanning:
            scanningPage(state, actions: actions)
        }
    }

    private func welcomePage() -> some View {
        VStack(spacing: 0) {
            Spacer()
            AtticMarkIcon(size: 72) // DESIGN.md, First run: 72pt mark.
                .foregroundStyle(scheme.primary)
                .padding(.bottom, AtticSpacing.xxxl)
            Text(S.onboardingWelcomeTitle)
                .atticFont(.headlineLargeEmphasized.tracking(-0.3))
                .foregroundStyle(scheme.onSurface)
                .multilineTextAlignment(.center)
                .accessibilityAddTraits(.isHeader)
                .padding(.bottom, 14) // DESIGN.md, First run: 14pt under the headline.
            Text(S.onboardingWelcomeBody)
                .atticFont(.bodyLarge)
                .foregroundStyle(scheme.onSurfaceVariant)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.horizontal, AtticSpacing.xxl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func folderPage(_ state: OnboardingState, actions: OnboardingActions) -> some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                stepLabel(S.onboardingStep(1, 2))
                    .padding(.bottom, AtticSpacing.s)
                Text(S.onboardingAccessTitle)
                    .atticFont(.headlineLargeEmphasized)
                    .foregroundStyle(scheme.onSurface)
                    .accessibilityAddTraits(.isHeader)
                    .padding(.bottom, 14) // DESIGN.md, First run: 14pt under the headline.
                Text(S.iosFolderBody)
                    .atticFont(.bodyLarge)
                    .foregroundStyle(scheme.onSurfaceVariant)
            }
            .padding(.horizontal, AtticSpacing.xxl)

            ListGroup {
                SettingsRow(
                    title: S.iosFolderRowTitle,
                    summary: state.pickedFolderName ?? S.iosFolderRowBody,
                    symbol: .folderOpen
                ) {
                    if state.pickedFolderName != nil {
                        SymbolIcon(.check, size: 24) // DESIGN.md, First run: 24pt tick.
                            .foregroundStyle(scheme.primary)
                    } else {
                        AtticButton(S.iosFolderChoose, variant: .tonal) {
                            actions.openPicker()
                        }
                    }
                }
            }
            .padding(.top, AtticSpacing.xxl)

            Spacer()
        }
        .padding(.top, AtticSpacing.xxxl) // DESIGN.md, First run: 32pt below the status bar.
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private func scanningPage(_ state: OnboardingState, actions: OnboardingActions) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            stepLabel(S.onboardingStep(2, 2))
                .padding(.horizontal, AtticSpacing.xxl)

            if state.isCounting {
                LoadingState(S.onboardingScanning)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let count = state.foundCount, count > 0 {
                Text(S.onboardingFound(count: count, S.grouped(count)))
                    .atticFont(.headlineLargeEmphasized)
                    .foregroundStyle(scheme.onSurface)
                    .accessibilityAddTraits(.isHeader)
                    .padding(.horizontal, AtticSpacing.xxl)
                    .padding(.top, AtticSpacing.s)
                Spacer()
            } else {
                EmptyState(
                    title: S.homeEmptyTitle,
                    body: S.homeEmptyBody,
                    symbol: .musicOff,
                    clover: true
                ) {
                    AtticButton(S.iosFolderChooseAnother, variant: .tonal) {
                        actions.back()
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .padding(.top, AtticSpacing.xxxl) // DESIGN.md, First run: 32pt below the status bar.
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private func stepLabel(_ text: String) -> some View {
        Text(text)
            .atticFont(.labelLarge)
            .foregroundStyle(scheme.primary)
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder
    private func bottomButton(_ state: OnboardingState, actions: OnboardingActions) -> some View {
        switch state.step {
        case .welcome:
            AtticButton(S.onboardingGetStarted, size: .medium, fillsWidth: true, action: actions.next)
                .padding(.horizontal, AtticSpacing.xxl)
                .padding(.bottom, AtticSpacing.xxl) // DESIGN.md, First run: 24pt above the home indicator.
        case .chooseFolder:
            AtticButton(S.onboardingContinue, size: .medium, fillsWidth: true, action: actions.next)
                .disabled(state.pickedFolderName == nil)
                .padding(.horizontal, AtticSpacing.xxl)
                .padding(.bottom, AtticSpacing.xxl)
        case .scanning:
            if !state.isCounting {
                AtticButton(S.onboardingStart, size: .medium, fillsWidth: true, action: actions.start)
                    .padding(.horizontal, AtticSpacing.xxl)
                    .padding(.bottom, AtticSpacing.xxl)
            }
        }
    }
}
