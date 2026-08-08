#!/usr/bin/env python3
"""Scarica le animazioni anatomiche (figura in posa, muscoli lavorati colorati) per gli
esercizi del catalogo e le converte in WebP animate dentro app/src/main/assets/media/.

Fonte: https://github.com/omercotkd/exercises-gifs (MIT), file assets/<id>.gif.
La corrispondenza fra i nomi del catalogo e gli id sta in tools/exercise_gifs.json ed e'
curata a mano: i nomi delle due raccolte non combaciano.

Per ogni esercizio mappato scrive app/src/main/assets/media/<cartella>/anim.webp e cancella
i due fotogrammi fotografici (0.webp / 1.webp) di free-exercise-db, che l'animazione
sostituisce. Gli esercizi senza mappatura (valore null) restano com'erano.

Le GIF di partenza sono 360x360: la conversione tiene quella risoluzione (non si ingrandisce,
non ci sarebbe dettaglio in piu') e di default e' lossless, cioe' l'animazione nell'app e'
identica alla GIF di GitHub. Lossless costa meno della GIF stessa (la GIF ha 256 colori:
WebP lossless li tiene tutti e comprime meglio), quindi non c'e' motivo di degradarla.
Con --quality N si torna a una conversione lossy (era q85 fino alla Fase 20).

Uso:
    python3 tools/fetch_exercise_gifs.py [--width 360] [--quality N] [--force]

Serve ffmpeg con libwebp. Rilancialo solo se cambia il catalogo o la mappatura (salta i
file gia' presenti); poi alza CATALOG_VERSION in ExerciseSeeder.
"""

import argparse
import json
import os
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "app/src/main/assets/seed/exercises.json")
MAPPING = os.path.join(ROOT, "tools/exercise_gifs.json")
OUT_DIR = os.path.join(ROOT, "app/src/main/assets/media")
BASE_URL = "https://raw.githubusercontent.com/omercotkd/exercises-gifs/main/assets/"


def convert(gif_bytes, dest, width, quality):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    # quality None = lossless: l'animazione resta quella originale di GitHub.
    codec = ["-lossless", "1"] if quality is None else ["-lossless", "0", "-q:v", str(quality)]
    # ffmpeg legge la GIF da file e non da stdin: il demuxer gif vuole poter fare seek.
    with tempfile.NamedTemporaryFile(suffix=".gif") as source:
        source.write(gif_bytes)
        source.flush()
        subprocess.run(
            [
                "ffmpeg", "-v", "error", "-y",
                "-i", source.name,
                # min(iw,width): la sorgente non si ingrandisce mai, si sfocherebbe e basta.
                "-vf", f"scale='min(iw,{width})':-1:flags=lanczos",
                "-loop", "0",
                "-c:v", "libwebp_anim",
                *codec,
                "-compression_level", "6",
                dest,
            ],
            check=True,
        )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--width", type=int, default=360)
    parser.add_argument("--quality", type=int, default=None, help="conversione lossy a questa qualita' (default: lossless)")
    parser.add_argument("--force", action="store_true", help="riconverte anche cio' che c'e' gia'")
    args = parser.parse_args()

    with open(CATALOG, encoding="utf-8") as handle:
        folders = {
            exercise["name"]: (exercise.get("mediaUri") or "").split("/")[0]
            for exercise in json.load(handle)
        }
    with open(MAPPING, encoding="utf-8") as handle:
        mapping = {k: v for k, v in json.load(handle).items() if not k.startswith("_")}

    written = 0
    failed = []
    unmapped = sorted(name for name, gif_id in mapping.items() if not gif_id)

    for index, (name, gif_id) in enumerate(sorted(mapping.items()), 1):
        folder = folders.get(name)
        if not gif_id or not folder:
            continue
        dest = os.path.join(OUT_DIR, folder, "anim.webp")
        if os.path.exists(dest) and not args.force:
            continue
        try:
            with urllib.request.urlopen(f"{BASE_URL}{gif_id}.gif", timeout=60) as response:
                data = response.read()
        except (urllib.error.URLError, urllib.error.HTTPError) as error:
            print(f"! {name} ({gif_id}): {error}", file=sys.stderr)
            failed.append(name)
            continue
        convert(data, dest, args.width, args.quality)
        written += 1
        if index % 20 == 0:
            print(f"  {index}/{len(mapping)}")

    # I fotogrammi fotografici servono solo dove non c'e' l'animazione.
    removed = 0
    for name, gif_id in mapping.items():
        folder = folders.get(name)
        if not gif_id or not folder:
            continue
        if not os.path.exists(os.path.join(OUT_DIR, folder, "anim.webp")):
            continue
        for frame in ("0.webp", "1.webp"):
            path = os.path.join(OUT_DIR, folder, frame)
            if os.path.exists(path):
                os.remove(path)
                removed += 1

    total = sum(
        os.path.getsize(os.path.join(root, f))
        for root, _, files in os.walk(OUT_DIR)
        for f in files
    )
    print(f"scritte {written} animazioni, tolti {removed} fotogrammi, assets/media {total / 1_000_000:.1f} MB")
    if unmapped:
        print(f"senza animazione (restano le foto): {unmapped}", file=sys.stderr)
    if failed:
        print(f"download falliti: {failed}", file=sys.stderr)


if __name__ == "__main__":
    main()
