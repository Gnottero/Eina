#!/usr/bin/env python3
"""Download the anatomical animations (posed figure, worked muscles coloured) for the catalog
exercises and convert them to animated WebP under app/src/main/assets/media/.

Source: https://github.com/omercotkd/exercises-gifs (MIT), files assets/<id>.gif.
The mapping between catalog names and ids lives in tools/exercise_gifs.json and is curated by
hand, since the two collections name things differently.

For every mapped exercise it writes app/src/main/assets/media/<folder>/anim.webp and deletes the
two free-exercise-db photographic frames (0.webp / 1.webp) the animation replaces. Exercises
without a mapping (null value) are left untouched.

The source GIFs are 360x360, the only resolution that collection has. The exercise screen draws
them full width (~1050px on a density-3 phone), so Android scales them up almost 3x and the pixels
show. The default conversion therefore runs an AI upscale with realesrgan-ncnn-vulkan
(realesrgan-x4plus 4x, then down to 720px) before recompressing: these are flat-shaded 3D renders,
and the model reconstructs the edges instead of blurring them. Roughly 5 s per exercise.

  --no-upscale   direct conversion, with no upscaling.
  --quality N    forces lossy quality; without upscale and without -q the conversion is lossless
                 (the animation stays identical to the source GIF).

At 720px lossless would cost ~950 KB per file (assets over 250 MB), so with the upscale the default
quality is 90: ~180 KB per file, in line with the previous lossless 360px output.

Usage:
    python3 tools/fetch_exercise_gifs.py [--no-upscale] [--width N] [--quality N] [--force]

Requires ffmpeg with libwebp, Pillow and — unless --no-upscale — realesrgan-ncnn-vulkan.
Re-run it only when the catalog or the mapping changes (existing files are skipped), then bump
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
    # quality None means lossless: the animation stays identical to the source GIF.
    codec = ["-lossless", "1"] if quality is None else ["-lossless", "0", "-q:v", str(quality)]
    # ffmpeg reads the GIF from a file and not from stdin: the gif demuxer needs to seek.
    with tempfile.NamedTemporaryFile(suffix=".gif") as source:
        source.write(gif_bytes)
        source.flush()
        subprocess.run(
            [
                "ffmpeg", "-v", "error", "-y",
                "-i", source.name,
                # min(iw,width): the source is never enlarged here, it would only blur.
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
    """Like convert(), but runs the frames through realesrgan before recompressing them.

    The upscaler works on PNGs in a folder and not on a GIF, so frames are extracted and
    reassembled with Pillow — which is also the only way to rewrite per-frame durations. These
    GIFs have no constant frame rate (1000 ms on the opening pose, 100 ms on the in-between
    frames), and a fixed fps would change their rhythm.
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
            # Flattened onto white: the upscaler ignores the alpha channel and would leave halos.
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
    parser.add_argument("--width", type=int, default=None, help="output width (720 with upscale, 360 without)")
    parser.add_argument("--quality", type=int, default=None, help="lossy conversion at this quality (default: 90 with upscale, lossless without)")
    parser.add_argument("--no-upscale", dest="upscale", action="store_false", help="direct conversion, without realesrgan")
    # x4plus upscales 4x and the result is scaled down to 720: oversampling smooths the artefacts
    # better than a direct 2x. realesr-animevideov3 with --scale 2 is ~9x faster but hardens the
    # outlines; realesrgan-x4plus-anime darkens them outright and changes the style.
    parser.add_argument("--model", default="realesrgan-x4plus", help="realesrgan model")
    parser.add_argument("--scale", type=int, default=4, help="upscaling factor of the model")
    parser.add_argument("--force", action="store_true", help="reconvert files that already exist")
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

    # The photographic frames are only needed where no animation exists.
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
