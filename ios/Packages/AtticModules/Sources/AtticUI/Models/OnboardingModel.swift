import AtticCore
import AtticKit
import AtticLibrary
import Foundation
import SwiftUI

/// The three pages of the first run.
public enum OnboardingStep: Sendable {
    case welcome, chooseFolder, scanning
}

/// What the first run shows. The picked URL stays in the view; only its name is shown here.
public struct OnboardingState: Sendable {
    public var step: OnboardingStep
    public var pickedFolderName: String?
    public var foundCount: Int?
    public var isCounting: Bool
}

@MainActor
public struct OnboardingActions {
    public let next: () -> Void
    /// Back to the folder page with the picker open, for "Choose another folder".
    public let back: () -> Void
    public let openPicker: () -> Void
    public let picked: (URL) -> Void
    public let start: () -> Void
}

/// The first run's state and actions. The view owns the step, the picked URL and the count.
@MainActor
public func makeOnboardingModel(
    app: AppModel,
    router: Router,
    step: Binding<OnboardingStep>,
    pickedURL: Binding<URL?>,
    foundCount: Binding<Int?>,
    isCounting: Binding<Bool>,
    showPicker: Binding<Bool>
) -> (state: OnboardingState, actions: OnboardingActions) {
    let state = OnboardingState(
        step: step.wrappedValue,
        pickedFolderName: pickedURL.wrappedValue?.lastPathComponent,
        foundCount: foundCount.wrappedValue,
        isCounting: isCounting.wrappedValue
    )

    let actions = OnboardingActions(
        next: {
            withAnimation(AtticMotion.defaultSpatial) {
                switch step.wrappedValue {
                case .welcome:
                    step.wrappedValue = .chooseFolder
                case .chooseFolder:
                    guard let url = pickedURL.wrappedValue else { return }
                    foundCount.wrappedValue = nil
                    isCounting.wrappedValue = true
                    step.wrappedValue = .scanning
                    // Only count files here. Adding the root now would switch RootView to the shell
                    // before the person sees this page, so the real scan happens there instead.
                    Task { @MainActor in
                        let root = LibraryRoot(name: url.lastPathComponent, bookmark: Data())
                        let opened = url.startAccessingSecurityScopedResource()
                        defer { if opened { url.stopAccessingSecurityScopedResource() } }
                        let files = await FolderScanner().scan(root: root, at: url)
                        guard !Task.isCancelled else { return }
                        foundCount.wrappedValue = files.count
                        isCounting.wrappedValue = false
                    }
                case .scanning:
                    break
                }
            }
        },
        back: {
            withAnimation(AtticMotion.defaultSpatial) {
                step.wrappedValue = .chooseFolder
                foundCount.wrappedValue = nil
            }
            showPicker.wrappedValue = true
        },
        openPicker: {
            showPicker.wrappedValue = true
        },
        picked: { url in
            pickedURL.wrappedValue = url
            foundCount.wrappedValue = nil
            showPicker.wrappedValue = false
        },
        start: {
            guard let url = pickedURL.wrappedValue else { return }
            Task { try? await app.library.addFolder(url) }
        }
    )

    return (state, actions)
}
