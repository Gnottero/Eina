#!/usr/bin/env python3
"""Reduce the full free-exercise-db seed to the curated Eina catalog.

    python3 tools/curate_exercises.py

Reads the complete export (eina_exercises_seed.json), keeps only the names listed in
tools/common_exercises.txt and rewrites app/src/main/assets/seed/exercises.json.

Two things are dropped from the export:
  - `loggingInstructions`, one of the five canonical sentences derived from `weightType`:
    it now lives in strings.xml and follows the UI language;
  - the conversion script's working fields (`needsReview`, `_originalCategory`), unused
    at runtime.

Description translations live in tools/translations/*.json, one file per batch, each a
name -> {it, fr} map, merged here as `descriptionIt` / `descriptionFr`. Translated names
live in a single file, tools/exercise_names.json (same shape), and become `nameIt` /
`nameFr`: the English name stays the key the seeder matches exercises by.
An untranslated exercise stays in English rather than dropping out of the catalog.
"""

import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT / "eina_exercises_seed.json"
WHITELIST = ROOT / "tools" / "common_exercises.txt"
TRANSLATIONS_DIR = ROOT / "tools" / "translations"
NAMES_FILE = ROOT / "tools" / "exercise_names.json"
FACTORS_FILE = ROOT / "tools" / "bodyweight_factors.json"
WEIGHT_TYPES_FILE = ROOT / "tools" / "weight_type_overrides.json"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "seed" / "exercises.json"

# Bodyweight only enters the volume for these types, so only for these does the lifted share
# need to be known.
BODYWEIGHT_TYPES = ("BODYWEIGHT", "BODYWEIGHT_PLUS_LOAD", "ASSISTED")

DROPPED_FIELDS = ("loggingInstructions", "needsReview", "_originalCategory")


def read_whitelist():
    names = []
    for line in WHITELIST.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            names.append(line)
    duplicates = [n for n in set(names) if names.count(n) > 1]
    if duplicates:
        sys.exit(f"Nomi ripetuti nella whitelist: {sorted(duplicates)}")
    return names


def main():
    catalog = {e["name"]: e for e in json.loads(SOURCE.read_text(encoding="utf-8"))}
    wanted = read_whitelist()

    # A typo in the whitelist would silently drop an exercise, so stop instead.
    missing = [n for n in wanted if n not in catalog]
    if missing:
        sys.exit("Nomi non presenti nel dataset:\n  " + "\n  ".join(missing))

    translations = {}
    for batch in sorted(TRANSLATIONS_DIR.glob("*.json")):
        for name, texts in json.loads(batch.read_text(encoding="utf-8")).items():
            if name in translations:
                sys.exit(f"{name} tradotto due volte, l'ultima in {batch.name}")
            translations[name] = texts

    unknown = [n for n in translations if n not in catalog]
    if unknown:
        sys.exit("Tradotti nomi fuori dal dataset:\n  " + "\n  ".join(unknown))

    names = json.loads(NAMES_FILE.read_text(encoding="utf-8"))
    unknown_names = [n for n in names if n not in catalog]
    if unknown_names:
        sys.exit("Nomi tradotti fuori dal dataset:\n  " + "\n  ".join(unknown_names))

    # The original dataset counts everything as reps on a weight stack: cardio machines come out
    # as MACHINE_STACK and the plank as BODYWEIGHT. The types that do not fit are rewritten here
    # (see weight_type_overrides.json).
    type_overrides = {k: v for k, v in json.loads(WEIGHT_TYPES_FILE.read_text(encoding="utf-8")).items()
                      if not k.startswith("_")}
    unknown_overrides = [n for n in type_overrides if n not in catalog]
    if unknown_overrides:
        sys.exit("Tipi riscritti fuori dal dataset:\n  " + "\n  ".join(unknown_overrides))

    def weight_type_of(name):
        return type_overrides.get(name, catalog[name]["weightType"])

    factors = {k: v for k, v in json.loads(FACTORS_FILE.read_text(encoding="utf-8")).items()
               if not k.startswith("_")}
    unknown_factors = [n for n in factors if n not in catalog]
    if unknown_factors:
        sys.exit("Fattori di peso corporeo fuori dal dataset:\n  " + "\n  ".join(unknown_factors))
    # The factor applies to the final type and not the dataset one: a plank rewritten to
    # TIME_BASED no longer has a volume in kg to weigh.
    senza_fattore = [n for n in wanted if weight_type_of(n) in BODYWEIGHT_TYPES and n not in factors]
    if senza_fattore:
        sys.exit("Manca il fattore di peso corporeo in bodyweight_factors.json:\n  " + "\n  ".join(senza_fattore))

    curated = []
    for name in wanted:
        entry = {k: v for k, v in catalog[name].items() if k not in DROPPED_FIELDS}
        entry["weightType"] = weight_type_of(name)
        translated = translations.get(name, {})
        # Two exercises in the original dataset have an empty description: the "en" key in the
        # translation file fills it without touching the upstream export.
        if translated.get("en"):
            entry["description"] = translated["en"]
        if translated.get("it"):
            entry["descriptionIt"] = translated["it"]
        if translated.get("fr"):
            entry["descriptionFr"] = translated["fr"]
        translated_name = names.get(name, {})
        if translated_name.get("it"):
            entry["nameIt"] = translated_name["it"]
        if translated_name.get("fr"):
            entry["nameFr"] = translated_name["fr"]
        if entry["weightType"] in BODYWEIGHT_TYPES:
            entry["bodyweightFactor"] = factors[name]
        curated.append(entry)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        json.dumps(curated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    untranslated = [e["name"] for e in curated if "descriptionIt" not in e or "descriptionFr" not in e]
    unnamed = [e["name"] for e in curated if "nameIt" not in e or "nameFr" not in e]
    print(f"{len(curated)} esercizi scritti in {OUTPUT.relative_to(ROOT)}")
    print(f"  con descrizione IT+FR: {len(curated) - len(untranslated)}")
    print(f"  con nome IT+FR: {len(curated) - len(unnamed)}")
    if unnamed:
        print(f"  nome ancora solo in inglese: {len(unnamed)}")
        for name in unnamed:
            print(f"    - {name}")
    if untranslated:
        print(f"  ancora solo in inglese: {len(untranslated)}")
        for name in untranslated:
            print(f"    - {name}")


if __name__ == "__main__":
    main()
