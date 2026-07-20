# Pocket Plugins

A collection of small changes and additions to enhance vanilla gameplay, built as 26 independent
modules behind an internal module system.

Every module can be turned off on its own — see [Configuration](#configuration). To add one, see
[Contributing](Contributing.md).

## Modules

| Module | Behavior |
|---|---|
| ArmorStandPoser | Sneak + right-click an armor stand: a stick adds arms, sugar makes it small |
| ArmorSwapper | Sneak + right-click an armor stand to swap all four armor pieces with it |
| AutoBreeding | Animals pathfind toward nearby dropped breeding items and enter love mode |
| BedrockBreaker | Sneak + right-click a powered beacon with a netherite ingot to destroy the bedrock column above it |
| BeePlanter | Bees pick up dropped seeds and plant them on free farmland |
| BiomeBombs | Craftable bombs (egg + 8× catalyst) thrown to convert the surrounding biome |
| CobbleGenerator | Replaces vanilla lava + water cobble with a weighted roll over configurable materials |
| CommandMacros | Config-defined command macros, op-gated |
| ConcreteMixer | Concrete powder dropped into a filled water cauldron hardens |
| CraftingKeeper | Crafting tables retain their grid contents on close, saved per location |
| Creeperworks | Creeper explosions spawn a creeper-shaped firework |
| CropTweaks | Right-click a mature crop with a hoe to harvest and replant; blaze powder acts as bonemeal on nether wart |
| Dispensery | Dispensers place configured blocks (anvil by default), and fill or empty cauldrons from buckets |
| DyeShears | Craftable shears (2 shears + diamond) with a chance to drop matching dye instead of wool |
| EasyPaintings | Sneak + right-click a painting to cycle through art variants |
| FeatherPlucker | Hit a chicken non-lethally for a free feather, on a cooldown tracked per chicken |
| IgneousGenerator | Water next to a magma block becomes andesite, diorite, or granite after a delay |
| InventoryInspector | In creative or spectator, sneak + right-click a player for a read-only snapshot of their inventory |
| LeadAttacher | Attach a lead to a fence without an entity on the other end |
| MobGriefing | Stops sheep eating grass and endermen picking up blocks, keeping the sound and animation |
| MobSizes | Randomizes mob scale; rideable mounts are sized by their health pool instead |
| SlimyBoots | Slime-crafted leather boots that negate fall damage and bounce you |
| TimePlayed | Tracks per-player play time |
| WanderingTraderBuffs | Config-defined wandering trader trades, added to the vanilla ones or replacing them |
| WoodPile | Enclose logs in dirt and light it to convert them to coal blocks over time |

## Commands

| Command | Module | Description |
|---|---|---|
| `/biomebombs`, `/bb` | BiomeBombs | Lists all available biome bombs and their crafting ingredients |
| `/macro <list \| run>` | CommandMacros | Command macro management |
| `/timeplayed` | TimePlayed | Reports your play time |

## Configuration

Configuration is split across two kinds of file.

**`config.yml`** holds read-only settings, one section per module, named for the module in
lowercase. Add `enabled: false` to a section to turn that module off:

```yaml
woodpile:
  enabled: false
```

Not every module has a section — those without one run on their defaults. A module is enabled
unless it says otherwise.

**`configs/<module>.yml`** holds state the module itself writes: macros, custom recipes, play
times. These are copied out of the jar on first run and shouldn't normally need hand-editing.
