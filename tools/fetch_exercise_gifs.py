#!/usr/bin/env python3
"""Scarica le animazioni anatomiche (figura in posa, muscoli lavorati colorati) per gli
esercizi del catalogo e le converte in WebP animate dentro app/src/main/assets/media/.

Fonte: https://github.com/omercotkd/exercises-gifs (MIT), file assets/<id>.gif.
La corrispondenza fra i nomi del catalogo e gli id sta in tools/exercise_gifs.json ed e'
curata a mano: i nomi delle due raccolte non combaciano.

Per ogni esercizio mappato scrive app/src/main/assets/media/<cartella>/anim.webp e cancella
i due fotogrammi fotografici (0.webp / 1.webp) di free-exercise-db, che l'animazione
sostituisce. Gli esercizi senza mappatura (valore null) restano com'erano.

Le GIF di partenza sono 360x360, l'unica risoluzione che quella raccolta ha. La scheda
esercizio pero' le disegna a tutta larghezza (~1050px su un telefono a densita' 3), quindi
Android le ingrandisce di quasi 3x e si vedono i pixel. Per questo la conversione di default
(Fase 23) passa da un upscale AI con realesrgan-ncnn-vulkan (realesrgan-x4plus 4x, poi giu' a
720px) prima di ricomprimere: sono render 3D a tinte piatte, il modello ricostruisce i bordi
invece di sfocarli. Conta ~5 s a esercizio.

  --no-upscale   torna alla conversione diretta delle Fasi 19-21 (nessun ingrandimento).
  --quality N    forza la qualita' lossy; senza upscale e senza -q la conversione e' lossless
                 (l'animazione resta identica alla GIF di GitHub, come in Fase 21).

A 720px il lossless costerebbe ~950 KB a file (assets oltre 250 MB), quindi con l'upscale la
qualita' di default e' 90: ~180 KB a file, in linea col lossless a 360px di prima.

Uso:
    python3 tools/fetch_exercise_gifs.py [--no-upscale] [--width N] [--quality N] [--force]

Serve ffmpeg con libwebp, Pillow e — salvo --no-upscale — realesrgan-ncnn-vulkan.
Rilancialo solo se cambia il catalogo o la mappatura (salta i file gia' presenti); poi alza
CATALOG_VERSION in ExerciseSeeder.
"""

import argparse
import io
import json
import os
import shutil
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
UPSCALER = "realesrgan-ncnn-vulkan"


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


def upscale_convert(gif_bytes, dest, width, quality, model, scale):
    """Come convert(), ma passa i fotogrammi per realesrgan prima di ricomprimerli.

    L'upscaler lavora su PNG in una cartella, non su una GIF: i fotogrammi si estraggono e si
    rimontano con Pillow, che e' anche l'unico modo di riscrivere le durate per fotogramma —
    queste GIF non hanno un frame rate costante (tengono 1000 ms sulla posa iniziale e 100 ms
    sulle intermedie), e passare per un fps fisso ne cambierebbe il ritmo.
    """
    from PIL import Image

    os.makedirs(os.path.dirname(dest), exist_ok=True)
    source = Image.open(io.BytesIO(gif_bytes))
    durations = []
    with tempfile.TemporaryDirectory() as work:
        raw, big = os.path.join(work, "raw"), os.path.join(work, "big")
        os.makedirs(raw)
        os.makedirs(big)
        for index in range(source.n_frames):
            source.seek(index)
            durations.append(source.info.get("duration", 100))
            # Fondo bianco appiattito: l'upscaler ignora il canale alfa e lascerebbe aloni.
            source.convert("RGB").save(os.path.join(raw, f"{index:04d}.png"))
        subprocess.run(
            [UPSCALER, "-i", raw, "-o", big, "-n", model, "-s", str(scale), "-f", "png"],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        frames = []
        for index in range(source.n_frames):
            frame = Image.open(os.path.join(big, f"{index:04d}.png")).convert("RGB")
            if frame.width != width:
                frame = frame.resize((width, width * frame.height // frame.width), Image.LANCZOS)
            frames.append(frame)
        frames[0].save(
            dest,
            format="WEBP",
            save_all=True,
            append_images=frames[1:],
            duration=durations,
            loop=0,
            lossless=quality is None,
            quality=quality if quality is not None else 100,
            method=6,
        )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--width", type=int, default=None, help="larghezza in uscita (720 con upscale, 360 senza)")
    parser.add_argument("--quality", type=int, default=None, help="conversione lossy a questa qualita' (default: 90 con upscale, lossless senza)")
    parser.add_argument("--no-upscale", dest="upscale", action="store_false", help="conversione diretta, senza realesrgan")
    # x4plus ingrandisce 4x e poi si scende a 720: il sovracampionamento smussa gli artefatti
    # meglio di un 2x diretto. realesr-animevideov3 con --scale 2 e' ~9x piu' veloce ma marca
    # di piu' i contorni; realesrgan-x4plus-anime li annerisce proprio, cambia lo stile.
    parser.add_argument("--model", default="realesrgan-x4plus", help="modello realesrgan")
    parser.add_argument("--scale", type=int, default=4, help="fattore di ingrandimento del modello")
    parser.add_argument("--force", action="store_true", help="riconverte anche cio' che c'e' gia'")
    args = parser.parse_args()
    if args.width is None:
        args.width = 720 if args.upscale else 360
    if args.quality is None and args.upscale:
        args.quality = 90
    if args.upscale and not shutil.which(UPSCALER):
        sys.exit(f"{UPSCALER} non trovato: installalo (AUR realesrgan-ncnn-vulkan-bin) o usa --no-upscale")

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
        if args.upscale:
            upscale_convert(data, dest, args.width, args.quality, args.model, args.scale)
        else:
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
