#!/usr/bin/env python3
"""Scarica i due fotogrammi di free-exercise-db per i 197 esercizi del catalogo curato e
li converte in WebP dentro app/src/main/assets/media/.

I due fotogrammi sono l'inizio e la fine del movimento: alternati in dissolvenza danno
l'animazione della scheda esercizio (vedi ui/components/ExerciseAnimation.kt).

Uso:
    python3 tools/fetch_exercise_media.py [--width 480] [--quality 70]

Rilancialo solo se cambia il catalogo; il download salta i file gia' presenti.
Serve ImageMagick (`magick`) con delegato WebP.
"""

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "app/src/main/assets/seed/exercises.json")
RAW_DATASET = os.path.join(ROOT, "eina_exercises_seed.json")
OUT_DIR = os.path.join(ROOT, "app/src/main/assets/media")
BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"


def frames_by_name():
    with open(RAW_DATASET, encoding="utf-8") as handle:
        return {e["name"]: (e.get("mediaFrames") or []) for e in json.load(handle)}


def catalog_names():
    with open(CATALOG, encoding="utf-8") as handle:
        return [e["name"] for e in json.load(handle)]


def convert(jpg_bytes, dest, width, quality):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    subprocess.run(
        [
            "magick", "-",
            "-resize", f"{width}x{width}>",
            "-strip",
            "-quality", str(quality),
            f"webp:{dest}",
        ],
        input=jpg_bytes,
        check=True,
    )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--width", type=int, default=480)
    parser.add_argument("--quality", type=int, default=70)
    args = parser.parse_args()

    frames = frames_by_name()
    names = catalog_names()
    missing = []
    written = 0

    for index, name in enumerate(names, 1):
        paths = frames.get(name) or []
        if not paths:
            missing.append(name)
            continue
        for path in paths:
            dest = os.path.join(OUT_DIR, os.path.splitext(path)[0] + ".webp")
            if os.path.exists(dest):
                continue
            try:
                with urllib.request.urlopen(BASE_URL + path, timeout=30) as response:
                    data = response.read()
            except (urllib.error.URLError, urllib.error.HTTPError) as error:
                print(f"! {path}: {error}", file=sys.stderr)
                missing.append(name)
                continue
            convert(data, dest, args.width, args.quality)
            written += 1
        if index % 20 == 0:
            print(f"  {index}/{len(names)}")

    total = sum(
        os.path.getsize(os.path.join(root, f))
        for root, _, files in os.walk(OUT_DIR)
        for f in files
    )
    print(f"scritti {written} fotogrammi, totale {total / 1_000_000:.1f} MB")
    if missing:
        print(f"senza immagini: {sorted(set(missing))}", file=sys.stderr)


if __name__ == "__main__":
    main()
