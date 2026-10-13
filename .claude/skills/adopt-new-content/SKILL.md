---
name: adopt-new-content
description: Find the content a new Minecraft/Paper version adds (mobs, biomes, wood sets, dyed blocks) and bring every feature that lists content by hand up to date. That covers washing recipes, villager outfits, mob head textures and the mc-utils tables. Use after a Paper upgrade, or when the user wants the plugins or data pack to handle new blocks, items, mobs or biomes.
---

# Adopt new content

New content rarely breaks anything. It just falls outside every feature that lists content by
hand, so nobody notices until a player tries to wash a wool slab. This skill finds the new content
and works through each feature that should know about it.

## 1. Diff the content

Compare the API jars of the version the server ran before and the one it runs now:

```sh
python .claude/skills/adopt-new-content/diff_content.py 26.2.build.133-stable 26.3.build.178-stable
```

The output lists new and removed mobs, biomes, and items, with the items grouped into **dyed
families** (all 16 colors), **wood sets**, and everything else. A removed constant is usually a
rename, so grep the configs for its old name.

The jars come from `_server/libraries` or `~/.m2`. Use the **server** lines, because the server is
what players see, even when the build line trails it.

## 2. Map it to features

| New content | Feature | What to do | When it can ship |
|---|---|---|---|
| Dyed family, with the color in the id | Washing (pack) | `wash_family.py`, step 4 | Now: the pack follows the server line |
| Item dyed through the `dyed_color` component | Washing (pack) | A `crafting_transmute` recipe by hand (see `pack/README.md`) | Now |
| Biome | VillagerMakeovers `outfits` / `unstyled` | Give it an outfit, or list it as unstyled | Shipped `config.yml` only once the build line has it, because `OutfitDriftTest` rejects unknown biomes. Until then, the dev server's config only |
| Mob | MobHeads `textures` | A texture hash for each variant | Now, but only with real hashes. Ask the user; never invent one |
| Mob | CompressedMobs `creatures`, MobEggs blacklist | Optional | Now |
| Wood set, sapling, tool tier, crop | mc-utils `CropTypes`, `ToolTypes` | Add the constants. `RegistryDriftTest` flags the gap | Only once the build line has them, since they're Java constants |
| Stone or ore | CobbleGenerator / IgneousGenerator config | Optional | Now |
| Renamed or removed constant | Any config that names it | Fix the name | Now |

These update themselves, so leave them alone:

- WoodPile (`Tag.LOGS_THAT_BURN`)
- ExtendedBeacons (`Tag.BEACON_BASE_BLOCKS`)
- CropTweaks (`Tag.DIRT`)
- AutoBreeding (`Animals#isBreedItem`)
- mc-utils `EntityTypes`, which is registry-derived

ConcreteMixer and DyeShears use the fixed 16 dye colors, so they only change if a new dye color
appears.

## 3. Agree the list with the user

Sort each item into one of four groups:

- **Required:** a feature promises "all of X" (washing every dyed item, an outfit for every biome),
  or a drift test fails. Do these.
- **Style calls:** which outfit a biome gets, or whether a new bed washes like the others. Propose a
  default that matches its nearest neighbor, e.g. forests wear plains, and let the user overrule it.
- **Optional:** new content that could join a feature, like new trades or a new compressed mob.
  List these and ask.
- **External data:** a mob head texture is a hash that can't be derived. Ask the user for it.
  Never invent one.

## 4. Make the changes

**Washing.** One command per dyed family:

```sh
python .claude/skills/adopt-new-content/wash_family.py wool_slab "minecraft:{color}_wool_slab" minecraft:white_wool_slab building
```

- It writes the `washable/<family>` tag (every color except the result), the shapeless recipes for
  batches 1–8, and the shared unlock advancement, all matching the existing families byte for byte.
- Pass `1` as a fifth argument for items that don't stack, as beds do. If you don't know an item's
  stack size, ask the user or check it in-game. Don't guess.
- The result is the undyed item if one exists (`minecraft:glass`, `minecraft:candle`), otherwise the
  white variant.
- Add the new family to the list in `pack/README.md`'s washing section.

**Biomes.** Put the biome in `villagermakeovers.outfits` next to similar biomes. The lists are
grouped by theme, not sorted.

**When the build line trails the server line,** anything gated by a drift test or written as a Java
constant has to wait (see the table). Put biome entries in the dev server's
`plugins/PocketPlugins/config.yml` for now, and keep a list of what's waiting.

**Existing servers keep their old `config.yml`.** Paper never overwrites it, so a list entry added
to the shipped config doesn't reach them. Tell the user exactly which entries their other servers
need. Never edit another server's files yourself.

## 5. Verify

1. Run `python .claude/skills/add-recipe/check_pack.py` for washing and other pack changes.
2. Run `mvn -P all test`.
3. Run `mvn install -P survival` and boot the dev server. Check the log for:
   - registry errors (a pack that fails to parse stops the server starting)
   - VillagerMakeovers' "no outfit is configured" warning
   - any plugin warnings
4. Give the user a short in-game checklist: wash one item from each new family, walk a villager into
   each new biome, and kill each new mob.

Commit with the `commit` skill. Pack changes bump `pack`, and config changes bump the plugin that
owns them.
