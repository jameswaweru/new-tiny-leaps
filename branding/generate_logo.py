"""
Tiny Leaps logo generator.

A chubby baby frog (big shiny eyes, rosy cheeks, a single baby curl) mid-leap under a dotted
leap arc with little stars, on a soft pink -> cream gradient.

    python3 branding/generate_logo.py      # writes the SVG variants into branding/

Rasterise to PNG with scripts/rasterize-svg.mjs (headless Chrome), see README.
"""
import math
import os

OUT = os.path.dirname(os.path.abspath(__file__))
INK = '#4A3F6B'
GREEN, BELLY, CHEEK = '#7FD6A0', '#FFF4D6', '#FFB3C7'


def frog(cx, cy, s):
    """Baby frog centred at (cx, cy); design space is 1024 with the frog centred at (512, 560)."""
    T = lambda x, y: (cx + (x - 512) * s, cy + (y - 560) * s)
    sw = 20 * s

    def ell(x, y, rx, ry, fill, rot=0, stroke=True, op=1.0):
        X, Y = T(x, y)
        tr = f' transform="rotate({rot} {X:.1f} {Y:.1f})"' if rot else ''
        st = f' stroke="{INK}" stroke-width="{sw:.1f}"' if stroke else ''
        o = f' opacity="{op}"' if op != 1 else ''
        return f'<ellipse cx="{X:.1f}" cy="{Y:.1f}" rx="{rx * s:.1f}" ry="{ry * s:.1f}" fill="{fill}"{st}{tr}{o}/>'

    def circ(x, y, r, fill, stroke=True):
        X, Y = T(x, y)
        st = f' stroke="{INK}" stroke-width="{sw:.1f}"' if stroke else ''
        return f'<circle cx="{X:.1f}" cy="{Y:.1f}" r="{r * s:.1f}" fill="{fill}"{st}/>'

    def path(cmds, w=20):
        d = ''
        for cmd, pts in cmds:
            d += cmd + ' ' + ' '.join(f'{T(pts[i], pts[i + 1])[0]:.1f} {T(pts[i], pts[i + 1])[1]:.1f}'
                                      for i in range(0, len(pts), 2)) + ' '
        return (f'<path d="{d.strip()}" fill="none" stroke="{INK}" stroke-width="{w * s:.1f}" '
                f'stroke-linecap="round" stroke-linejoin="round"/>')

    g = [
        # back legs kicked out (leaping)
        ell(300, 760, 120, 58, GREEN, rot=-28),
        ell(724, 760, 120, 58, GREEN, rot=28),
        # body + belly
        ell(512, 690, 205, 165, GREEN),
        ell(512, 715, 128, 108, BELLY),
        # eye bumps + head, then a borderless fill to merge them into one soft silhouette
        circ(392, 380, 108, GREEN),
        circ(632, 380, 108, GREEN),
        ell(512, 500, 272, 190, GREEN),
        ell(512, 470, 250, 150, GREEN, stroke=False),
        # baby curl
        path([('M', [500, 318]), ('C', [470, 250, 560, 230, 560, 280]), ('C', [560, 310, 520, 312, 522, 290])], w=18),
    ]
    for ex in (392, 632):
        g += [circ(ex, 385, 74, '#FFFFFF'),
              circ(ex + 8, 395, 46, INK, stroke=False),
              circ(ex + 24, 376, 16, '#FFFFFF', stroke=False),
              circ(ex - 6, 414, 8, '#FFFFFF', stroke=False)]
    g += [ell(330, 535, 50, 32, CHEEK, stroke=False, op=0.9),
          ell(694, 535, 50, 32, CHEEK, stroke=False, op=0.9),
          path([('M', [450, 540]), ('Q', [512, 600, 574, 540])])]
    return '\n'.join(g)


def arc_dots(p0, p1, p2, n, r):
    out = []
    for i in range(n):
        t = 0.04 + 0.92 * i / (n - 1)
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="#FFFFFF"/>')
    return '\n'.join(out)


def star(cx, cy, R, fill='#FFD166', rot=0):
    pts = []
    for i in range(10):
        a = math.radians(rot - 90 + i * 36)
        rr = R if i % 2 == 0 else R * 0.48
        pts.append(f'{cx + rr * math.cos(a):.1f},{cy + rr * math.sin(a):.1f}')
    return (f'<polygon points="{" ".join(pts)}" fill="{fill}" stroke="{INK}" '
            f'stroke-width="{R * 0.16:.1f}" stroke-linejoin="round"/>')


DEFS = '''<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#FFE1EC"/>
    <stop offset="0.55" stop-color="#FFEFE2"/>
    <stop offset="1" stop-color="#FFF6D8"/>
  </linearGradient>
</defs>'''
DECO = '\n'.join([
    '<circle cx="150" cy="170" r="54" fill="#FFFFFF" opacity="0.55"/>',
    '<circle cx="900" cy="560" r="30" fill="#FFFFFF" opacity="0.6"/>',
    '<circle cx="870" cy="890" r="66" fill="#FFFFFF" opacity="0.45"/>',
    '<circle cx="130" cy="880" r="30" fill="#FFFFFF" opacity="0.6"/>',
])
ARC = arc_dots((110, 760), (512, -250), (914, 760), 17, 12)
STARS = star(120, 700, 40, rot=10) + star(905, 690, 34, '#9AD8FF', rot=-12) + star(790, 150, 28, '#FFB3C7', rot=20)
FROG = frog(512, 650, 0.78)


def svg(body, rounded=True):
    radius = ' rx="230"' if rounded else ''
    rect = f'<rect width="1024" height="1024"{radius} fill="url(#bg)"/>'
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">\n'
            f'{DEFS}\n{rect}\n{body}\n</svg>')


def write(name, content):
    with open(os.path.join(OUT, name), 'w') as f:
        f.write(content)


if __name__ == '__main__':
    body = f'{DECO}\n{ARC}\n{STARS}\n{FROG}'
    write('logo.svg', svg(body))                       # rounded app logo (in-app use)
    write('icon_square.svg', svg(body, rounded=False))  # launcher icon (launcher applies the mask)
    # adaptive icon foreground: transparent, artwork inside the 66% safe zone
    write('icon_foreground.svg',
          '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">\n'
          f'<g transform="translate(512 512) scale(0.64) translate(-512 -512)">\n{ARC}\n{STARS}\n{FROG}\n</g>\n</svg>')
    # frog mark only, for coloured backgrounds
    write('mark.svg', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="150 240 724 690" width="724" height="690">'
                      f'{frog(512, 600, 0.86)}</svg>')
    print('Logo SVGs written to', OUT)
