import Foundation
import UIKit

/// Hue, chroma and tone: the colour space the tonal palettes are built in. Hue and chroma are CAM16's,
/// under the default sRGB viewing conditions; tone is CIE L*, so two colours' contrast ratio depends on
/// their tones alone. A port of the Android kit's `Hct.kt`, which follows material-color-utilities
/// (Apache 2.0) with a simpler iterative solver.
public struct Hct: Equatable, Sendable {
    public var hue: Double
    public var chroma: Double
    public var tone: Double

    public init(hue: Double, chroma: Double, tone: Double) {
        self.hue = hue
        self.chroma = chroma
        self.tone = tone
    }

    /// The colour of an opaque `0xRRGGBB` value.
    public init(rgb: UInt32) {
        let xyz = ColorMath.xyz(fromRgb: rgb)
        let cam = Cam16.from(x: xyz.0, y: xyz.1, z: xyz.2)
        self.init(hue: cam.hue, chroma: cam.chroma, tone: ColorMath.lstar(fromY: xyz.1))
    }

    public init(_ color: UIColor) {
        self.init(rgb: color.atticRgb)
    }

    /// The sRGB colour closest to this one, as `0xRRGGBB`. Chroma drops until the colour fits in sRGB; tone is kept.
    public var rgb: UInt32 { HctSolver.solve(hue: hue, chroma: chroma, tone: tone) }
}

public extension UIColor {
    /// An opaque colour from `0xRRGGBB`.
    convenience init(atticRgb rgb: UInt32) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }

    /// This colour in sRGB as `0xRRGGBB`, alpha dropped.
    var atticRgb: UInt32 {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        if !getRed(&r, green: &g, blue: &b, alpha: &a) {
            var white: CGFloat = 0
            getWhite(&white, alpha: &a)
            r = white; g = white; b = white
        }
        func channel(_ value: CGFloat) -> UInt32 { UInt32(max(0, min(255, (value * 255).rounded()))) }
        return (channel(r) << 16) | (channel(g) << 8) | channel(b)
    }
}

/// WCAG contrast ratio between two opaque `0xRRGGBB` colours, from 1 to 21.
public func atticContrastRatio(_ a: UInt32, _ b: UInt32) -> Double {
    let ya = ColorMath.xyz(fromRgb: a).1
    let yb = ColorMath.xyz(fromRgb: b).1
    return (max(ya, yb) + 5) / (min(ya, yb) + 5)
}

enum ColorMath {
    private static let e = 216.0 / 24389.0
    private static let kappa = 24389.0 / 27.0

    static let whitePointD65 = (95.047, 100.0, 108.883)

    /// 0–255 channel to linear RGB on a 0–100 scale.
    static func linearized(_ channel: UInt32) -> Double {
        let n = Double(channel) / 255
        return n <= 0.040449936 ? n / 12.92 * 100 : pow((n + 0.055) / 1.055, 2.4) * 100
    }

    /// Linear RGB on a 0–100 scale to a 0–255 channel.
    static func delinearized(_ linear: Double) -> UInt32 {
        let n = linear / 100
        let d = n <= 0.0031308 ? n * 12.92 : 1.055 * pow(n, 1 / 2.4) - 0.055
        return UInt32(max(0, min(255, (d * 255).rounded())))
    }

    static func xyz(fromRgb rgb: UInt32) -> (Double, Double, Double) {
        let r = linearized((rgb >> 16) & 0xFF)
        let g = linearized((rgb >> 8) & 0xFF)
        let b = linearized(rgb & 0xFF)
        return (
            0.41233895 * r + 0.35762064 * g + 0.18051042 * b,
            0.2126 * r + 0.7152 * g + 0.0722 * b,
            0.01932141 * r + 0.11916382 * g + 0.95034478 * b
        )
    }

    static func linearRgb(x: Double, y: Double, z: Double) -> (Double, Double, Double) {
        (
            3.2413774792388685 * x - 1.5376652402851851 * y - 0.49885366846268053 * z,
            -0.9691452513005321 * x + 1.8758853451067872 * y + 0.04156585616912061 * z,
            0.05562093689691305 * x - 0.20395524564742123 * y + 1.0571799111220335 * z
        )
    }

    static func rgb(fromLinear rgb: (Double, Double, Double)) -> UInt32 {
        (delinearized(rgb.0) << 16) | (delinearized(rgb.1) << 8) | delinearized(rgb.2)
    }

    static func y(fromLstar lstar: Double) -> Double { 100 * labInvf((lstar + 16) / 116) }

    static func lstar(fromY y: Double) -> Double { 116 * labF(y / 100) - 16 }

    static func rgb(fromLstar lstar: Double) -> UInt32 {
        let c = delinearized(y(fromLstar: lstar))
        return (c << 16) | (c << 8) | c
    }

    private static func labF(_ t: Double) -> Double { t > e ? cbrt(t) : (kappa * t + 16) / 116 }

    private static func labInvf(_ ft: Double) -> Double {
        let cube = ft * ft * ft
        return cube > e ? cube : (116 * ft - 16) / kappa
    }
}

/// The default viewing conditions: sRGB white, 200/π lux adapting luminance, a mid-grey background, average surround.
struct ViewingConditions {
    let n: Double
    let aw: Double
    let nbb: Double
    let ncb: Double
    let c: Double
    let nc: Double
    let rgbD: (Double, Double, Double)
    let fl: Double
    let z: Double

    static let standard = ViewingConditions()

    private init() {
        let white = ColorMath.whitePointD65
        let adaptingLuminance = 200 / Double.pi * ColorMath.y(fromLstar: 50) / 100
        let rW = white.0 * 0.401288 + white.1 * 0.650173 + white.2 * -0.051461
        let gW = white.0 * -0.250268 + white.1 * 1.204414 + white.2 * 0.045854
        let bW = white.0 * -0.002079 + white.1 * 0.048952 + white.2 * 0.953127
        let f = 1.0
        c = 0.69
        nc = f
        let d = max(0, min(1, f * (1 - (1 / 3.6) * exp((-adaptingLuminance - 42) / 92))))
        rgbD = (d * (100 / rW) + 1 - d, d * (100 / gW) + 1 - d, d * (100 / bW) + 1 - d)
        let k = 1 / (5 * adaptingLuminance + 1)
        let k4 = k * k * k * k
        let k4F = 1 - k4
        fl = k4 * adaptingLuminance + 0.1 * k4F * k4F * cbrt(5 * adaptingLuminance)
        n = ColorMath.y(fromLstar: 50) / white.1
        z = 1.48 + n.squareRoot()
        nbb = 0.725 / pow(n, 0.2)
        ncb = nbb
        let factors = (
            pow(fl * rgbD.0 * rW / 100, 0.42),
            pow(fl * rgbD.1 * gW / 100, 0.42),
            pow(fl * rgbD.2 * bW / 100, 0.42)
        )
        let rgbA = (
            400 * factors.0 / (factors.0 + 27.13),
            400 * factors.1 / (factors.1 + 27.13),
            400 * factors.2 / (factors.2 + 27.13)
        )
        aw = (2 * rgbA.0 + rgbA.1 + 0.05 * rgbA.2) * nbb
    }
}

struct Cam16 {
    let hue: Double
    let chroma: Double
    let j: Double

    private static func sign(_ value: Double) -> Double { value < 0 ? -1 : (value > 0 ? 1 : 0) }

    static func from(x: Double, y: Double, z: Double) -> Cam16 {
        let vc = ViewingConditions.standard
        let rC = 0.401288 * x + 0.650173 * y - 0.051461 * z
        let gC = -0.250268 * x + 1.204414 * y + 0.045854 * z
        let bC = -0.002079 * x + 0.048952 * y + 0.953127 * z
        let rA = adapt(vc.rgbD.0 * rC)
        let gA = adapt(vc.rgbD.1 * gC)
        let bA = adapt(vc.rgbD.2 * bC)
        let a = (11 * rA + -12 * gA + bA) / 11
        let b = (rA + gA - 2 * bA) / 9
        let u = (20 * rA + 20 * gA + 21 * bA) / 20
        let p2 = (40 * rA + 20 * gA + bA) / 20
        var hue = atan2(b, a) * 180 / .pi
        if hue < 0 { hue += 360 } else if hue >= 360 { hue -= 360 }
        let ac = p2 * vc.nbb
        let j = 100 * pow(ac / vc.aw, vc.c * vc.z)
        let huePrime = hue < 20.14 ? hue + 360 : hue
        let eHue = 0.25 * (cos(huePrime * .pi / 180 + 2) + 3.8)
        let p1 = 50000 / 13 * eHue * vc.nc * vc.ncb
        let t = p1 * hypot(a, b) / (u + 0.305)
        let alpha = pow(t, 0.9) * pow(1.64 - pow(0.29, vc.n), 0.73)
        let chroma = alpha * (j / 100).squareRoot()
        return Cam16(hue: hue, chroma: chroma, j: j)
    }

    private static func adapt(_ component: Double) -> Double {
        let af = pow(ViewingConditions.standard.fl * abs(component) / 100, 0.42)
        return sign(component) * 400 * af / (af + 27.13)
    }

    /// The XYZ colour with this lightness J, chroma and hue.
    static func xyz(j: Double, chroma: Double, hue: Double) -> (Double, Double, Double) {
        let vc = ViewingConditions.standard
        let alpha = (chroma == 0 || j == 0) ? 0 : chroma / (j / 100).squareRoot()
        let t = pow(alpha / pow(1.64 - pow(0.29, vc.n), 0.73), 1 / 0.9)
        let hRad = hue * .pi / 180
        let eHue = 0.25 * (cos(hRad + 2) + 3.8)
        let ac = vc.aw * pow(j / 100, 1 / vc.c / vc.z)
        let p1 = eHue * (50000 / 13) * vc.nc * vc.ncb
        let p2 = ac / vc.nbb
        let hSin = sin(hRad)
        let hCos = cos(hRad)
        let gamma = 23 * (p2 + 0.305) * t / (23 * p1 + 11 * t * hCos + 108 * t * hSin)
        let a = gamma * hCos
        let b = gamma * hSin
        let rA = (460 * p2 + 451 * a + 288 * b) / 1403
        let gA = (460 * p2 - 891 * a - 261 * b) / 1403
        let bA = (460 * p2 - 220 * a - 6300 * b) / 1403
        let rF = unadapt(rA) / vc.rgbD.0
        let gF = unadapt(gA) / vc.rgbD.1
        let bF = unadapt(bA) / vc.rgbD.2
        return (
            1.86206786 * rF - 1.01125463 * gF + 0.14918677 * bF,
            0.38752654 * rF + 0.62144744 * gF - 0.00897398 * bF,
            -0.01584150 * rF - 0.03412294 * gF + 1.04996444 * bF
        )
    }

    private static func unadapt(_ adapted: Double) -> Double {
        let base = max(0, 27.13 * abs(adapted) / (400 - abs(adapted)))
        return sign(adapted) * (100 / ViewingConditions.standard.fl) * pow(base, 1 / 0.42)
    }
}

/// Finds the sRGB colour for a hue, chroma and tone. For a given chroma it bisects CAM16 lightness J
/// until the colour's luminance matches the tone; if that colour falls outside sRGB it bisects chroma
/// down to the most that fits. Tone is always met (up to 8-bit rounding).
enum HctSolver {
    private static let gamutSlack = 0.01

    static func solve(hue: Double, chroma: Double, tone: Double) -> UInt32 {
        if chroma < 0.0001 || tone < 0.0001 || tone > 99.9999 { return ColorMath.rgb(fromLstar: tone) }
        let h = (hue.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
        let targetY = ColorMath.y(fromLstar: tone)
        if let rgb = inGamut(hue: h, chroma: chroma, targetY: targetY) { return ColorMath.rgb(fromLinear: rgb) }
        var low = 0.0
        var high = chroma
        var best: (Double, Double, Double)?
        for _ in 0..<22 {
            let mid = (low + high) / 2
            if let rgb = inGamut(hue: h, chroma: mid, targetY: targetY) {
                best = rgb
                low = mid
            } else {
                high = mid
            }
        }
        return best.map(ColorMath.rgb(fromLinear:)) ?? ColorMath.rgb(fromLstar: tone)
    }

    /// Linear RGB for the hue and chroma at luminance `targetY`, or nil if it is outside sRGB.
    private static func inGamut(hue: Double, chroma: Double, targetY: Double) -> (Double, Double, Double)? {
        var low = 0.0
        var high = 100.0
        var xyz = Cam16.xyz(j: 50, chroma: chroma, hue: hue)
        for _ in 0..<36 {
            let j = (low + high) / 2
            xyz = Cam16.xyz(j: j, chroma: chroma, hue: hue)
            if xyz.1 < targetY { low = j } else { high = j }
        }
        if abs(xyz.1 - targetY) > 0.05 * max(1, targetY) { return nil }
        let rgb = ColorMath.linearRgb(x: xyz.0, y: xyz.1, z: xyz.2)
        let range = (-gamutSlack)...(100 + gamutSlack)
        return range.contains(rgb.0) && range.contains(rgb.1) && range.contains(rgb.2) ? rgb : nil
    }
}
