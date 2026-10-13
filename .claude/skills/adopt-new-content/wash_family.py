"""Generate a washing family for the data pack: the washable tag, one shapeless
recipe per batch size, and the unlock advancement, matching the existing families.

Run from the repo root:
    python .claude/skills/adopt-new-content/wash_family.py <family> <item> <result> <category> [max-batch]

    family     name used for the files, tag and recipe group, e.g. wool_slab
    item       the dyed item, with {color} where the color goes, e.g. minecraft:{color}_wool_slab
    result     what washing gives back, e.g. minecraft:white_wool_slab
    category   recipe book category: building, redstone, equipment or misc
    max-batch  largest batch, 8 by default; 1 for items that don't stack (like beds)

The tag holds every color variant except the result itself, so washing is never a no-op.
For items whose color lives in a component rather than the id (leather armor), write a
crafting_transmute recipe by hand instead; see pack/README.md.
"""
import json
import sys
from pathlib import Path

PACK = Path("pack/src/main/datapack/data/pocket")
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]


def line_ending():
    # Match the files already in the pack, so the working tree stays consistent
    sample = next((PACK / "recipe" / "wash").glob("*.json"), None)
    return "\r\n" if sample and b"\r\n" in sample.read_bytes() else "\n"


def write(path, data, newline):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, indent=2) + "\n"
    path.write_bytes(text.replace("\n", newline).encode("utf-8"))
    print(f"wrote {path}")


def main():
    if len(sys.argv) not in (5, 6):
        sys.exit(__doc__)
    family, item, result, category = sys.argv[1:5]
    max_batch = int(sys.argv[5]) if len(sys.argv) == 6 else 8
    if "{color}" not in item:
        sys.exit("item needs a {color} placeholder")

    newline = line_ending()
    tag = f"#pocket:washable/{family}"
    recipes = [f"pocket:wash/{family}_{n}" for n in range(1, max_batch + 1)]
    variants = [item.format(color=color) for color in COLORS if item.format(color=color) != result]

    write(PACK / "tags" / "item" / "washable" / f"{family}.json", {"values": variants}, newline)

    for n in range(1, max_batch + 1):
        write(PACK / "recipe" / "wash" / f"{family}_{n}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": category,
            "group": f"pocket_wash_{family}",
            "ingredients": ["minecraft:water_bucket"] + [tag] * n,
            "result": {"id": result, "count": n},
        }, newline)

    write(PACK / "advancement" / "recipes" / "wash" / f"{family}.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_water_bucket": {
                "trigger": "minecraft:inventory_changed",
                "conditions": {"items": [{"items": "minecraft:water_bucket"}, {"items": tag}]},
            },
            "has_the_recipe": {
                "trigger": "minecraft:recipe_unlocked",
                "conditions": {"recipes": recipes[0]},
            },
        },
        "requirements": [["has_water_bucket", "has_the_recipe"]],
        "rewards": {"recipes": recipes},
    }, newline)


if __name__ == "__main__":
    main()
