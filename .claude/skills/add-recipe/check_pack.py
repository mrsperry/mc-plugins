"""Validate the data pack: every file parses, every recipe is unlocked by an
advancement, and every advancement points at a recipe that exists.

Run from the repo root: python .claude/skills/add-recipe/check_pack.py
"""
import json
import pathlib
import sys

ROOT = pathlib.Path("pack/src/main/datapack/data/pocket")


def load(path, problems):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as ex:
        problems.append(f"{path}: {ex}")
        return None


def main():
    problems = []

    recipes = set()
    for path in sorted((ROOT / "recipe").rglob("*.json")):
        load(path, problems)
        recipes.add("pocket:" + path.relative_to(ROOT / "recipe").with_suffix("").as_posix())

    unlocked = set()
    for path in sorted((ROOT / "advancement").rglob("*.json")):
        advancement = load(path, problems)
        if advancement:
            unlocked.update(advancement.get("rewards", {}).get("recipes", []))

    for path in sorted((ROOT / "tags").rglob("*.json")):
        load(path, problems)

    problems += [f"no unlock advancement: {r}" for r in sorted(recipes - unlocked)]
    problems += [f"advancement unlocks a missing recipe: {r}" for r in sorted(unlocked - recipes)]

    if problems:
        print("\n".join(problems))
        sys.exit(1)

    print(f"ok: {len(recipes)} recipes, all valid JSON and unlocked")


if __name__ == "__main__":
    main()
