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
            RootView(app: app)
        }
    }
}
