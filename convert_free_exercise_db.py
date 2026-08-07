"""
Converte il dataset free-exercise-db (yuhonas/free-exercise-db, dominio pubblico)
nello schema esercizi di Eina (weightType, description, loggingInstructions, ecc.).

Input:  exercises_raw.json  (dist/exercises.json del repo originale)
Output: eina_exercises_seed.json

La classificazione di weightType è euristica (basata su equipment/category/nome).
Ogni esercizio ha un flag "needsReview": true quando l'euristica non è affidabile
al 100% — filtra su questo campo per fare una passata manuale mirata invece di
rivedere tutti gli 873 esercizi uno per uno.
"""
import json

LOGGING_INSTRUCTIONS = {
    "FREE_WEIGHT": "Inserisci il peso totale sollevato (bilanciere + dischi, o il peso indicato sul manubrio/kettlebell).",
    "BODYWEIGHT": "Il peso è il tuo corpo: non serve inserire nulla, verrà usato il tuo ultimo peso corporeo registrato.",
    "BODYWEIGHT_PLUS_LOAD": "Inserisci solo il sovraccarico aggiunto: il peso corporeo viene sommato automaticamente.",
    "ASSISTED": "Inserisci il peso di assistenza scaricato dalla macchina/banda: verrà sottratto dal tuo peso corporeo nel calcolo del volume.",
    "MACHINE_STACK": "Inserisci il peso indicato sullo stack della macchina.",
    "TIME_BASED": "Non inserire le ripetizioni: registra la durata in secondi.",
}

EQUIPMENT_TO_TYPE = {
    "body only": ("BODYWEIGHT", False),
    "machine": ("MACHINE_STACK", False),
    "cable": ("MACHINE_STACK", False),
    "barbell": ("FREE_WEIGHT", False),
    "dumbbell": ("FREE_WEIGHT", False),
    "kettlebells": ("FREE_WEIGHT", False),
    "e-z curl bar": ("FREE_WEIGHT", False),
    "bands": ("FREE_WEIGHT", True),          # la "resistenza" della banda non è un vero peso in kg
    "medicine ball": ("FREE_WEIGHT", True),
    "exercise ball": ("FREE_WEIGHT", True),
    "foam roll": ("FREE_WEIGHT", True),      # mobilità/recupero, forse da escludere dal tracking del volume
    "other": ("FREE_WEIGHT", True),
    None: ("BODYWEIGHT", True),
}


def classify(ex):
    name_lower = ex["name"].lower()
    if "assist" in name_lower:
        return "ASSISTED", False
    if "weighted" in name_lower:
        return "BODYWEIGHT_PLUS_LOAD", False
    return EQUIPMENT_TO_TYPE.get(ex.get("equipment"), ("FREE_WEIGHT", True))


def convert(ex):
    weight_type, needs_review = classify(ex)
    # la categoria "stretching" è ambigua per un tracker di forza: la segnaliamo sempre
    # per revisione manuale (potrebbe restare BODYWEIGHT, diventare TIME_BASED, o essere
    # esclusa del tutto dalla libreria principale)
    if ex.get("category") == "stretching":
        needs_review = True

    description = " ".join(ex.get("instructions") or []).strip()
    images = ex.get("images") or []

    return {
        "name": ex["name"],
        "description": description,
        "loggingInstructions": LOGGING_INSTRUCTIONS[weight_type],
        "weightType": weight_type,
        "muscleGroupsPrimary": ex.get("primaryMuscles") or [],
        "muscleGroupsSecondary": ex.get("secondaryMuscles") or [],
        "equipment": ex.get("equipment"),
        "mediaUri": images[0] if images else None,
        "mediaFrames": images,  # sequenza completa dei frame, utile per costruire il loop animato
        "isCustom": False,
        "source": "free-exercise-db",
        "needsReview": needs_review,
        "_originalCategory": ex.get("category"),  # utile in fase di revisione, rimuovibile poi
    }


def main():
    with open("exercises_raw.json", encoding="utf-8") as f:
        raw = json.load(f)

    converted = [convert(ex) for ex in raw]

    with open("eina_exercises_seed.json", "w", encoding="utf-8") as f:
        json.dump(converted, f, ensure_ascii=False, indent=2)

    total = len(converted)
    needs_review = sum(1 for e in converted if e["needsReview"])
    by_type = {}
    for e in converted:
        by_type[e["weightType"]] = by_type.get(e["weightType"], 0) + 1

    print(f"Totale esercizi convertiti: {total}")
    print(f"Da rivedere manualmente (needsReview=true): {needs_review}")
    print("Distribuzione weightType:")
    for k, v in sorted(by_type.items(), key=lambda x: -x[1]):
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
