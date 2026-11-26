import AtticCore
import AtticKit
import SwiftUI

/// STUB: built by its screen task against ios/DESIGN.md and AtticKit.
struct FolderScreen: View {
    let path: String
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router

    var body: some View {
        Text("Folder")
    }
}
