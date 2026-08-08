#!/usr/bin/env python3
"""Riduce il seed completo di free-exercise-db al catalogo curato di Eina.

    python3 tools/curate_exercises.py

Legge l'export integrale (eina_exercises_seed.json), tiene solo i nomi elencati in
tools/common_exercises.txt e riscrive app/src/main/assets/seed/exercises.json.

Due cose spariscono rispetto all'export:
  - `loggingInstructions`, che era una delle cinque frasi canoniche derivate da
    `weightType`: ora vive in strings.xml e si traduce da sola con la UI;
  - i campi di lavorazione dello script di conversione (`needsReview`,
    `_originalCategory`), che al runtime non servivano a nessuno.

Le traduzioni delle descrizioni stanno in tools/translations/*.json, un file per blocco
di lavorazione, ciascuno una mappa nome -> {it, fr}. Vengono innestate qui come
`descriptionIt` / `descriptionFr`. I nomi tradotti stanno invece in un file unico,
tools/exercise_names.json (stessa forma), e diventano `nameIt` / `nameFr`: il nome
inglese resta la chiave con cui il seeder riconosce l'esercizio nel database.
Un esercizio senza traduzione resta in inglese: mancare una lingua non deve toglierlo
dal catalogo.
"""

import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT / "eina_exercises_seed.json"
WHITELIST = ROOT / "tools" / "common_exercises.txt"
TRANSLATIONS_DIR = ROOT / "tools" / "translations"
NAMES_FILE = ROOT / "tools" / "exercise_names.json"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "seed" / "exercises.json"

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

    # Un refuso nella whitelist toglierebbe un esercizio in silenzio: meglio fermarsi.
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

    curated = []
    for name in wanted:
        entry = {k: v for k, v in catalog[name].items() if k not in DROPPED_FIELDS}
        translated = translations.get(name, {})
        # Due esercizi del dataset originale hanno la descrizione vuota: la chiave "en"
        # nel file di traduzione permette di colmarla senza toccare l'export a monte.
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
