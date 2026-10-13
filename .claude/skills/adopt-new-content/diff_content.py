"""List the game content one paper-api version adds over another, grouped the way
this repo's features consume it. Read-only.

Run from the repo root:
    python .claude/skills/adopt-new-content/diff_content.py 26.2.build.133-stable 26.3.build.178-stable

API jars are found in _server/libraries (any version the dev server has unpacked) or the
local Maven repository.
"""
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

CLASSES = {
    "mobs and other entities": "org.bukkit.entity.EntityType",
    "biomes": "org.bukkit.block.Biome",
    "items and blocks": "org.bukkit.Material",
}

# Minecraft's dye order, which is also the order the washable tags list colors in
COLORS = ["WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
          "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"]


def find_jar(version):
    name = f"paper-api-{version}.jar"
    roots = [Path("_server/libraries/io/papermc/paper/paper-api"),
             Path.home() / ".m2/repository/io/papermc/paper/paper-api"]
    for root in roots:
        jar = root / version / name
        if jar.exists():
            return jar
    sys.exit(f"No {name} in _server/libraries or ~/.m2. Unpack that Paper (java "
             f"-Dpaperclip.patchonly=true -jar paper.jar) or build against it once.")


def constants(jar, cls):
    out = subprocess.run(["javap", "-cp", str(jar), cls], capture_output=True, text=True).stdout
    return set(re.findall(r"static final \S+ ([A-Z0-9_]+);", out))


def dyed_families(names):
    """New materials that come in all 16 colors, keyed by their name without the color."""
    families = defaultdict(set)
    for name in names:
        for color in sorted(COLORS, key=len, reverse=True):  # LIGHT_BLUE before BLUE
            if name.startswith(color + "_"):
                families[name[len(color) + 1:]].add(color)
                break
    return {family: colors for family, colors in families.items() if len(colors) == len(COLORS)}


def wood_sets(names):
    return sorted({name[:-len("_PLANKS")] for name in names if name.endswith("_PLANKS")})


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    old_jar, new_jar = find_jar(sys.argv[1]), find_jar(sys.argv[2])

    added_materials = set()
    for label, cls in CLASSES.items():
        old, new = constants(old_jar, cls), constants(new_jar, cls)
        added, removed = sorted(new - old), sorted(old - new)
        if cls.endswith("Material"):
            added_materials = set(added)
        print(f"{label}: {len(added)} added, {len(removed)} removed")
        if not cls.endswith("Material"):
            for name in added:
                print(f"  + {name}")
        for name in removed:
            print(f"  - {name}  (renamed or gone: check configs that name it)")

    families = dyed_families(added_materials)
    print(f"\nnew dyed families (all 16 colors): {', '.join(sorted(families)) or 'none'}")
    print(f"new wood sets: {', '.join(wood_sets(added_materials)) or 'none'}")
    grouped = set().union(*({f"{c}_{f}" for c in COLORS} for f in families)) if families else set()
    for wood in wood_sets(added_materials):
        grouped |= {name for name in added_materials if wood in name}
    rest = sorted(added_materials - grouped)
    print(f"other new items and blocks ({len(rest)}):")
    for name in rest:
        print(f"  + {name}")


if __name__ == "__main__":
    main()
