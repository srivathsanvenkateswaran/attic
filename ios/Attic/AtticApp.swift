import AtticKit
import AtticUI
import SwiftUI

@main
struct AtticApp: App {
    @State private var app: AppModel

    init() {
        AtticKit.registerFonts()
        _app = State(initialValue: AppModel())
    }

    var body: some Scene {
        WindowGroup {
            // `-kitGallery` shows the component gallery, for design review.
            if ProcessInfo.processInfo.arguments.contains("-kitGallery") {
                KitGallery()
            } else {
                RootView(app: app)
            }
        }
    }
}
