import AtticCore
import Foundation
import Observation

public enum AppTab: Hashable, CaseIterable, Sendable {
    case home, search, library
}

/// A pushed screen inside a tab's NavigationStack.
public enum Route: Hashable, Sendable {
    case album(key: String)
    case artist(key: String)
    case folder(path: String)
    case playlist(id: UUID)
    case favourites
    case settings
    case licences
    case licence(id: String)

    public init(_ destination: Destination) {
        switch destination {
        case .album(let key): self = .album(key: key)
        case .artist(let key): self = .artist(key: key)
        case .folder(let path): self = .folder(path: path)
        case .playlist(let id): self = .playlist(id: id)
        case .favourites: self = .favourites
        }
    }
}

/// Navigation state: the selected tab, each tab's stack, and the overlays over them.
@MainActor
@Observable
public final class Router {
    public var tab: AppTab = .home
    public var homePath: [Route] = []
    public var searchPath: [Route] = []
    public var libraryPath: [Route] = []
    public var showNowPlaying = false
    public var showQueue = false
    public var showLyrics = false

    public init() {}

    /// Pushes onto the selected tab's stack.
    public func push(_ route: Route) {
        switch tab {
        case .home: homePath.append(route)
        case .search: searchPath.append(route)
        case .library: libraryPath.append(route)
        }
    }

    public func open(_ destination: Destination) {
        showNowPlaying = false
        push(Route(destination))
    }

    public func pop() {
        switch tab {
        case .home: _ = homePath.popLast()
        case .search: _ = searchPath.popLast()
        case .library: _ = libraryPath.popLast()
        }
    }
}
