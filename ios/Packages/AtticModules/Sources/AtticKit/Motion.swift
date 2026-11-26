import SwiftUI

/// The Expressive springs. Android writes them as a damping ratio and a stiffness with mass 1; the same
/// spring here has `response = 2π / √stiffness` and `dampingFraction = dampingRatio`.
///
/// Spatial springs (position, size, shape) overshoot. Effects springs (colour, alpha) do not.
public enum AtticMotion {
    /// 0.6 / 800. Corner morphs, the heart's pop, a paused cover stepping back.
    public static let fastSpatial = spring(dampingRatio: 0.6, stiffness: 800)
    /// 0.8 / 380. Moves across the screen: the mini player sliding in, the wave's amplitude.
    public static let defaultSpatial = spring(dampingRatio: 0.8, stiffness: 380)
    /// 0.8 / 200. Larger moves.
    public static let slowSpatial = spring(dampingRatio: 0.8, stiffness: 200)
    /// 1 / 3800. Colour and alpha that switch at once: the play glyph, toggle colours.
    public static let fastEffects = spring(dampingRatio: 1, stiffness: 3800)
    /// 1 / 1600. Most fades: an app bar taking its colour, a row's fill.
    public static let defaultEffects = spring(dampingRatio: 1, stiffness: 1600)
    /// 1 / 800. The scheme moving to a new cover.
    public static let slowEffects = spring(dampingRatio: 1, stiffness: 800)
    /// 1 / 380. Anything that rises from the bottom, with no overshoot so no gap opens under it.
    public static let overlay = spring(dampingRatio: 1, stiffness: 380)

    /// A spring from Android's terms: a damping ratio and a stiffness, mass 1.
    public static func spring(dampingRatio: Double, stiffness: Double) -> Animation {
        .spring(response: 2 * Double.pi / stiffness.squareRoot(), dampingFraction: dampingRatio, blendDuration: 0)
    }

    /// How long the wave takes to travel one cycle, in seconds.
    public static let wavePeriod: Double = 1.0
    /// How long the loading indicator spends on each shape, in seconds.
    public static let loaderStep: Double = 0.65
    /// How long a snackbar stays, in seconds.
    public static let snackbarDuration: Double = 3.0
    /// How long a loading state waits before it shows, in seconds.
    public static let loadingDelay: Double = 0.4
}
