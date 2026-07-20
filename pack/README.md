# pack

The companion data pack and resource pack for these plugins.

The split is deliberate: **anything the vanilla data-pack format can express belongs here, not in a
plugin.** A plugin is warranted only when behavior needs code — an event handler, per-tick logic,
mutable state. Recipes, loot tables, advancements, tags, predicates, and item model definitions are
all data, so they live here.

## Layout

```
src/main/datapack/          -> pocket-datapack.zip
  pack.mcmeta
  data/pocket/recipe/               crafting/smelting/stonecutting/smithing recipes
  data/pocket/advancement/recipes/  unlock advancements, one per recipe

src/main/resourcepack/      -> pocket-resourcepack.zip
  pack.mcmeta
  assets/pocket/items/              item model definitions (1.21.4+ format)
  assets/pocket/models/item/        the models those definitions point at
  assets/pocket/textures/item/      textures
```

Namespace is `pocket` throughout, so recipe ids read `pocket:saddle`.

## Building

The module is in the `survival` and `all` profiles. It has no sources and produces no jar — the
assembly plugin just zips the two trees.

```
mvn -P survival package     # builds both zips into target/
mvn -P survival install     # additionally copies them to _dist/ and drops the
                            # data pack into _server/world/datapacks/
```

A running server does **not** pick up a newly copied zip on `/reload` — it rescans on start, or on
`/datapack enable`. The dev server auto-loads new packs it finds ("Found new data pack ...,
loading it automatically").

## Pack formats

Pinned to Paper 26.1.2: data pack **101**, resource pack **84**. These are the calendar-versioned
numbers and they move with `paper.version` — see `UPGRADING.md`.

Since 25w31a, `pack_format` is replaced by `min_format` / `max_format` (and `supported_formats` is
gone). Both are pinned to a single version rather than a range, because a range only works if the
content is genuinely valid across all of it.

## Writing recipes

Ingredients are plain strings — an item id, a `#tag`, or an array of item ids. The pre-1.20.5
`{"item": ...}` / `{"tag": ...}` object form no longer parses. `result` is always an object:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": { "L": "minecraft:leather", "I": "#minecraft:planks" },
  "pattern": ["LLL", "I I"],
  "result": { "id": "minecraft:saddle", "count": 1 }
}
```

`result` also takes a `components` map, which is how a recipe produces a custom item without any
plugin code — `minecraft:item_model` points at an item definition in the resource pack, and
`minecraft:custom_data` carries a marker a plugin can read back:

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

Prefer `item_name` over `custom_name` for permanent item identity — `custom_name` reads as a player
rename and an anvil can strip it.

### Unlock advancements

Data pack recipes do not appear in the recipe book on their own. Each recipe needs a sibling
advancement under `advancement/recipes/` with `parent: minecraft:recipes/root`, a
`minecraft:recipe_unlocked` criterion, and the recipe in `rewards.recipes`. Having no `display`
block keeps it hidden and suppresses the toast. This replaces the join-time `discoverRecipe` loop
that `Module` does for plugin-registered recipes.

## Serving the resource pack

Nothing sends it yet. When that's needed, it is `Audience#sendResourcePacks(ResourcePackRequest)`
(Adventure) rather than the deprecated `Player#setResourcePack`. The hash is lowercase-hex SHA-1
and is the client's cache key, so it must be recomputed on every rebuild or clients serve stale
content.
