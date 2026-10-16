"""Regenerates every NoFlix icon and branding image from render.py / social.py.

    python3 -m venv .venv && .venv/bin/pip install cairosvg pillow
    .venv/bin/python tools/branding/generate.py

cairosvg needs the cairo library (macOS: brew install cairo; may need
DYLD_FALLBACK_LIBRARY_PATH=/opt/homebrew/lib).
"""
import render
import social

ROOT = social.ROOT
RES = f"{ROOT}/app/src/main/res"
IMAGES = f"{ROOT}/images"

# Launcher icon: legacy round mipmaps (API < 26) and Play Store sources.
for density, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    render.masked(px, 0.5).save(f"{RES}/mipmap-{density}/zed_movies_round.png", optimize=True)
render.masked(512, 0).save(f"{ROOT}/app/src/main/ic_launcher-playstore.png", optimize=True)
render.masked(512, 0.5).save(f"{ROOT}/app/src/main/zed_movies_round-playstore.png", optimize=True)

# README logo and the repository's social preview (GitHub settings). The website lives in another project.
render.masked(256, 0.22).save(f"{IMAGES}/noflix-ad-free-movies-app-icon.png", optimize=True)
social.social_preview().save(f"{IMAGES}/social-preview.png", optimize=True)
