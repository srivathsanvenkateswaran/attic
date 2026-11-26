// swift-tools-version: 6.0
import PackageDescription

// One local package for the whole app. Targets, lowest first:
// AtticCore (models, keys, queue order, LRC) <- AtticLibrary, AtticPlayback
// AtticKit (tokens and components; depends on nothing)
// AtticUI (screens and view models) depends on all of them.
let package = Package(
    name: "AtticModules",
    platforms: [.iOS("26.0")],
    products: [
        .library(name: "AtticCore", targets: ["AtticCore"]),
        .library(name: "AtticLibrary", targets: ["AtticLibrary"]),
        .library(name: "AtticPlayback", targets: ["AtticPlayback"]),
        .library(name: "AtticKit", targets: ["AtticKit"]),
        .library(name: "AtticUI", targets: ["AtticUI"]),
    ],
    targets: [
        .target(name: "AtticCore"),
        .target(name: "AtticLibrary", dependencies: ["AtticCore"]),
        .target(name: "AtticPlayback", dependencies: ["AtticCore"]),
        .target(name: "AtticKit", resources: [.process("Resources")]),
        .target(
            name: "AtticUI",
            dependencies: ["AtticCore", "AtticLibrary", "AtticPlayback", "AtticKit"],
            resources: [.copy("Resources/Licences")]
        ),
    ],
    swiftLanguageModes: [.v5]
)
