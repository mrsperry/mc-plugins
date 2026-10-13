---
name: add-recipe
description: Add a crafting, smelting, stonecutting or smithing recipe to the companion data pack in pack/, together with the unlock advancement it needs to appear in the recipe book. Use whenever the user wants a new or changed recipe.
---

# Add a recipe

Recipes belong in the data pack, not a plugin. Use a module's `registerCraftingRecipe` only when
the recipe genuinely needs runtime logic, such as ingredients driven by plugin config (see
`biomebombs`).

All paths are under `pack/src/main/datapack/data/pocket/`. The namespace is `pocket`, and folder
names are singular: `recipe/`, `advancement/`, `tags/item/`.

## 1. Write the recipe

Write `recipe/<name>.json`. Its id becomes `pocket:<name>`. Put a family of related recipes in a
subfolder, e.g. `recipe/wash/candle_3.json` → `pocket:wash/candle_3`.

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": { "L": "minecraft:leather", "I": "#minecraft:planks" },
  "pattern": ["LLL", "I I"],
  "result": { "id": "minecraft:saddle", "count": 1 }
}
```

- **Ingredients are plain strings:** an item id, a `#tag`, or an array of item ids. The old
  `{"item": ...}` / `{"tag": ...}` object form no longer parses.
- **`result` is always an object** with `id`, an optional `count`, and optional `components`.
- **`category`:** crafting recipes use `building`, `redstone`, `equipment` or `misc`. Cooking
  recipes use `food`, `blocks` or `misc`.
- **Which type to use:**

| Type | Use it for |
|---|---|
| `crafting_shaped` | A fixed grid pattern |
| `crafting_shapeless` | Any arrangement. It matches an **exact** multiset of items, so each batch size (1–8 items) needs its own file. Give them a shared `group` so they collapse into one recipe book entry |
| `crafting_transmute` | Changing an item while keeping its components (enchantments, damage, names, container contents). A shapeless result is a fresh stack and would wipe them. Remove a component with a `"!minecraft:<component>": {}` patch in `result.components` |
| `smelting`, `blasting`, `smoking`, `campfire_cooking` | Cooking. These also take `experience` and `cookingtime` |
| `stonecutting`, `smithing_transform` | As in vanilla |

- **Tags:** if a set of items has no vanilla tag, add one at `tags/item/<path>.json` and reference it
  as `#pocket:<path>`.
- **Check you aren't shadowing a vanilla recipe.** An identical pattern conflicts, and the recipe
  book only shows one of the two.

### Custom items

A recipe can produce a custom item with no plugin code, using `result.components`:

```json
"result": {
  "id": "minecraft:diamond_sword",
  "components": {
    "minecraft:item_model": "pocket:flame_blade",
    "minecraft:item_name": { "text": "Flame Blade", "color": "gold", "italic": false },
    "minecraft:custom_data": { "pocket_id": "flame_blade" }
  }
}
```

- Use `item_name`, not `custom_name`. `custom_name` reads as a player rename, and an anvil can strip
  it.
- `custom_data` is the marker a plugin reads back.
- An `item_model` needs files in `pack/src/main/resourcepack/assets/pocket/`: an item definition in
  `items/<name>.json`, its model in `models/item/`, and its texture in `textures/item/`.

## 2. Write the unlock advancement

A data pack recipe doesn't show up in the recipe book without one. Put it at the same path under
`advancement/recipes/`, e.g. `advancement/recipes/<name>.json`:

```json
{
  "parent": "minecraft:recipes/root",
  "criteria": {
    "has_leather": {
      "trigger": "minecraft:inventory_changed",
      "conditions": { "items": [ { "items": "minecraft:leather" } ] }
    },
    "has_the_recipe": {
      "trigger": "minecraft:recipe_unlocked",
      "conditions": { "recipes": "pocket:saddle" }
    }
  },
  "requirements": [ [ "has_leather", "has_the_recipe" ] ],
  "rewards": { "recipes": [ "pocket:saddle" ] }
}
```

- The `recipe_unlocked` condition key is `recipes` since 26.3. The old `recipe` key fails to parse,
  and a data pack that fails to parse stops the server from starting.
- The `inventory_changed` criterion fires on the recipe's key ingredient. It can be a `#tag`.
- Both criteria sit in **one** inner `requirements` array, so either one unlocks the recipe.
- Leave out any `display` block. That keeps the advancement hidden and suppresses the toast.
- A family of batch recipes shares **one** advancement that lists every variant in
  `rewards.recipes` (see `advancement/recipes/wash/candle.json`).

## 3. Check

From the repo root:

```sh
python .claude/skills/add-recipe/check_pack.py
```

It confirms every file parses, every recipe is unlocked by an advancement, and every advancement
points at a recipe that exists.

To test in-game:

1. Run `mvn install -P survival`. This drops the pack into `_server/world/datapacks/`.
2. **Restart** the dev server. `/reload` doesn't pick up a newly copied zip.
3. Look for the recipe in the recipe book, or run `/recipe give @s pocket:<name>`, and craft it.

Don't touch `pack.mcmeta` formats. They move only with a Paper upgrade (the `upgrade-dependencies`
skill). If the recipe introduces a new mechanism rather than just another recipe, as washing did,
give it a short section in `pack/README.md`.

A recipe change bumps `pack/pom.xml`. The `commit` skill handles that.
