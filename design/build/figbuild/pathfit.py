"""Fit a closed outline sampled as points with a few cubic Béziers (Schneider, Graphics Gems 1990).

The shape maths in the foundations modules samples outlines the way the Compose code does (a
superellipse at 360 points, a radial shape at 180). Stored as straight segments, every instance of
a cover or card carries all of them, which made the files several times larger. A fit within a
tolerance of 0.1 % of the shape's size is indistinguishable at any zoom a designer uses and keeps
the path editable: a squircle comes out as 8 to 16 curves instead of 360 lines.

    from figbuild.pathfit import fit_closed
    d = fit_closed(points, tolerance=0.1)   # SVG path data, "M … C … Z"
"""
from __future__ import annotations

import math

Pt = tuple[float, float]


def _sub(a, b): return (a[0] - b[0], a[1] - b[1])
def _add(a, b): return (a[0] + b[0], a[1] + b[1])
def _mul(a, s): return (a[0] * s, a[1] * s)
def _dot(a, b): return a[0] * b[0] + a[1] * b[1]
def _len(a): return math.hypot(a[0], a[1])


def _norm(a):
    n = _len(a)
    return (a[0] / n, a[1] / n) if n else (0.0, 0.0)


def _bez(ctrl, t):
    mt = 1 - t
    return (mt ** 3 * ctrl[0][0] + 3 * mt * mt * t * ctrl[1][0] + 3 * mt * t * t * ctrl[2][0] + t ** 3 * ctrl[3][0],
            mt ** 3 * ctrl[0][1] + 3 * mt * mt * t * ctrl[1][1] + 3 * mt * t * t * ctrl[2][1] + t ** 3 * ctrl[3][1])


def _bez_d1(ctrl, t):
    mt = 1 - t
    return (3 * mt * mt * (ctrl[1][0] - ctrl[0][0]) + 6 * mt * t * (ctrl[2][0] - ctrl[1][0]) + 3 * t * t * (ctrl[3][0] - ctrl[2][0]),
            3 * mt * mt * (ctrl[1][1] - ctrl[0][1]) + 6 * mt * t * (ctrl[2][1] - ctrl[1][1]) + 3 * t * t * (ctrl[3][1] - ctrl[2][1]))


def _bez_d2(ctrl, t):
    mt = 1 - t
    return (6 * mt * (ctrl[2][0] - 2 * ctrl[1][0] + ctrl[0][0]) + 6 * t * (ctrl[3][0] - 2 * ctrl[2][0] + ctrl[1][0]),
            6 * mt * (ctrl[2][1] - 2 * ctrl[1][1] + ctrl[0][1]) + 6 * t * (ctrl[3][1] - 2 * ctrl[2][1] + ctrl[1][1]))


def _chord_params(pts):
    u = [0.0]
    for i in range(1, len(pts)):
        u.append(u[-1] + _len(_sub(pts[i], pts[i - 1])))
    total = u[-1] or 1.0
    return [x / total for x in u]


def _generate(pts, u, t1, t2):
    first, last = pts[0], pts[-1]
    c00 = c01 = c11 = x0 = x1 = 0.0
    for p, t in zip(pts, u):
        a1, a2 = _mul(t1, 3 * (1 - t) ** 2 * t), _mul(t2, 3 * (1 - t) * t * t)
        c00 += _dot(a1, a1); c01 += _dot(a1, a2); c11 += _dot(a2, a2)
        tmp = _sub(p, _bez((first, first, last, last), t))
        x0 += _dot(a1, tmp); x1 += _dot(a2, tmp)
    det = c00 * c11 - c01 * c01
    seg = _len(_sub(last, first))
    if abs(det) > 1e-12:
        al, ar = (x0 * c11 - x1 * c01) / det, (c00 * x1 - c01 * x0) / det
    else:
        al = ar = seg / 3
    if al < 1e-6 * seg or ar < 1e-6 * seg:
        al = ar = seg / 3
    return (first, _add(first, _mul(t1, al)), _add(last, _mul(t2, ar)), last)


def _max_error(pts, ctrl, u):
    worst, idx = 0.0, len(pts) // 2
    for i, (p, t) in enumerate(zip(pts, u)):
        e = _len(_sub(_bez(ctrl, t), p))
        if e > worst:
            worst, idx = e, i
    return worst, idx


def _reparam(pts, ctrl, u):
    out = []
    for p, t in zip(pts, u):
        d = _sub(_bez(ctrl, t), p)
        d1, d2 = _bez_d1(ctrl, t), _bez_d2(ctrl, t)
        den = _dot(d1, d1) + _dot(d, d2)
        out.append(min(1.0, max(0.0, t - _dot(d, d1) / den)) if den else t)
    return out


def _fit(pts, t1, t2, tol, out, depth=0):
    if len(pts) == 2:
        seg = _len(_sub(pts[1], pts[0])) / 3
        out.append((pts[0], _add(pts[0], _mul(t1, seg)), _add(pts[1], _mul(t2, seg)), pts[1]))
        return
    u = _chord_params(pts)
    ctrl = _generate(pts, u, t1, t2)
    err, split = _max_error(pts, ctrl, u)
    if err <= tol:
        out.append(ctrl); return
    if err <= tol * 4:
        for _ in range(6):
            u = _reparam(pts, ctrl, u)
            ctrl = _generate(pts, u, t1, t2)
            err, split = _max_error(pts, ctrl, u)
            if err <= tol:
                out.append(ctrl); return
    split = max(1, min(len(pts) - 2, split))
    tc = _norm(_sub(pts[split - 1], pts[split + 1]))
    _fit(pts[: split + 1], t1, tc, tol, out, depth + 1)
    _fit(pts[split:], _mul(tc, -1), t2, tol, out, depth + 1)


def fit_closed(points: list[Pt], tolerance: float | None = None, segments_hint: int = 8, digits: int = 3) -> str:
    """Closed outline -> SVG path of cubic Béziers. `tolerance` defaults to 0.1 % of the larger side.
    The outline is cut into `segments_hint` arcs at evenly spaced points first, so a symmetric shape
    gets symmetric curves."""
    pts = [tuple(map(float, p)) for p in points]
    if pts[0] == pts[-1]:
        pts = pts[:-1]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    if tolerance is None:
        tolerance = 0.001 * max(max(xs) - min(xs), max(ys) - min(ys))
    n = len(pts)
    k = max(1, min(segments_hint, n // 3))
    cuts = [round(i * n / k) for i in range(k)] + [n]
    curves = []
    for a, b in zip(cuts, cuts[1:]):
        arc = [pts[i % n] for i in range(a, b + 1)]
        prev, nxt = pts[(a - 1) % n], pts[(a + 1) % n]
        t1 = _norm(_sub(nxt, prev))
        prev2, nxt2 = pts[(b - 1) % n], pts[(b + 1) % n]
        t2 = _norm(_sub(prev2, nxt2))
        _fit(arc, t1, t2, tolerance, curves)
    f = lambda v: f"{v:.{digits}f}".rstrip("0").rstrip(".")
    d = [f"M{f(curves[0][0][0])} {f(curves[0][0][1])}"]
    for c in curves:
        d.append(f"C{f(c[1][0])} {f(c[1][1])} {f(c[2][0])} {f(c[2][1])} {f(c[3][0])} {f(c[3][1])}")
    return " ".join(d) + " Z"


def fit_stats(points, path, samples=4000) -> float:
    """Largest distance from any input point to the fitted path (sampled), for checking a fit."""
    import re
    nums = [float(x) for x in re.findall(r"-?\d+\.?\d*", path)]
    start, rest = (nums[0], nums[1]), nums[2:]
    curves, p0 = [], start
    for i in range(0, len(rest), 6):
        c = (p0, (rest[i], rest[i + 1]), (rest[i + 2], rest[i + 3]), (rest[i + 4], rest[i + 5]))
        curves.append(c); p0 = c[3]
    dense = [_bez(c, j / 400) for c in curves for j in range(401)]
    return max(min(_len(_sub(p, q)) for q in dense) for p in points)
