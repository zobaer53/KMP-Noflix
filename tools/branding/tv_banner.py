"""Generates the Android TV launcher banner (320x180) without external dependencies.

The shapes mirror the launcher icon in render.py; the banner is wider, so the mark is centred and
the rest is the same background gradient. Run directly or through generate.py.
"""
import os
import struct
import zlib

WIDTH, HEIGHT = 320, 180

SHIELD = [(160, 28), (222, 50), (222, 100), (160, 152), (98, 100), (98, 50)]
PLAY = [(146, 66), (146, 116), (192, 91)]


def inside(point, polygon):
    x, y = point
    result = False
    j = len(polygon) - 1
    for i in range(len(polygon)):
        xi, yi = polygon[i]
        xj, yj = polygon[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            result = not result
        j = i
    return result


def lerp(a, b, t):
    return a + (b - a) * t


def pixel(x, y):
    t = (x / WIDTH * 0.5 + y / HEIGHT * 0.5)
    base = [int(lerp(46, 20, t)), int(lerp(42, 18, t)), int(lerp(71, 29, t)), 255]
    distance = ((x - WIDTH / 2) / WIDTH) ** 2 + ((y - HEIGHT / 2) / HEIGHT) ** 2
    glow = max(0.0, 1 - distance * 9) * 0.35
    base[0] = int(base[0] + (102 - base[0]) * glow)
    base[1] = int(base[1] + (71 - base[1]) * glow)
    base[2] = int(base[2] + (240 - base[2]) * glow)
    if inside((x, y), SHIELD):
        shade = max(0.0, min(1.0, (y - 28) / 124))
        base[0], base[1], base[2] = int(lerp(18, 102, shade)), int(lerp(205, 71, shade)), int(lerp(217, 240, shade))
    if inside((x, y), PLAY):
        base[0], base[1], base[2] = 255, 255, 255
    return base


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def write_png(path):
    rows = b"".join(
        b"\x00" + bytes(v for x in range(WIDTH) for v in pixel(x, y))
        for y in range(HEIGHT)
    )
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(rows, 9))
        + chunk(b"IEND", b"")
    )
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as out:
        out.write(png)


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    write_png(os.path.join(root, "app", "src", "main", "res", "drawable", "tv_banner.png"))
