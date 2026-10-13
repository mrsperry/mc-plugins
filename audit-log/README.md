# AuditLog

Keeps a daily log per player of everything that could cost them their items, so anything lost can
be rolled back by hand. It only records; it never changes or restores anything.

## Files

```
plugins/AuditLog/logs/2026-10-12/players/Alice.jsonl
plugins/AuditLog/logs/2026-10-12/server.jsonl      events with no player, e.g. console commands
```

One JSON object per line. A record that involves several players, such as Bob taking Alice's death
pile, is written to each of their files. Days older than `retention-days` (7) are deleted when the
server starts.

```json
{"time":"2026-10-12T21:49:17.123-04:00","plugin":"DeathChest","event":"pile-item-taken","owner":{"name":"Alice","uuid":"…"},"by":{"name":"Bob","uuid":"…"},"location":{"world":"world","x":12.5,"y":64.0,"z":-30.5},"item":"minecraft:diamond_sword[minecraft:enchantments={sharpness:5}] 1","dropped":[]}
```

Items are in `/give` syntax: put `give <player> ` in front of one to recreate it exactly, with its
enchantments, name, damage, and contents for shulker boxes and bundles. Inventories are objects of
slot to item: 0–8 hotbar, 9–35 main inventory, 36–39 boots to helmet, 40 off hand.

## What gets recorded

AuditLog itself, each switchable in `config.yml`:

| Event | When |
|---|---|
| `join`, `leave` | With location, game mode, and (with `inventory-snapshots`) the full inventory, ender chest and XP |
| `death` | The inventory they died with, drops, XP, cause, and killer, who gets it in their file too |
| `mob-death` | A tamed or named mob dies: owner, killer and drops |
| `command` | A command from `audited-commands` run by a player, the console or RCON. Players named in it get it too |
| `item-lost` | An item a player threw despawns, burns or falls out of the world |
| `game-mode` | Any game mode change, with its cause |

Other plugins:

| Plugin | Events |
|---|---|
| DeathChest | `pile-created`, `items-vanished` (Curse of Vanishing items lost on a pile death), `pile-item-taken`, `pile-taken` (including anything dropped for lack of room), `pile-item-destroyed` (a stack removed by anything else, e.g. `/kill`) |
| AutoStack | `hand-restacked` |
| WorldClusters | `cluster-left` (what was stored before the wipe), `cluster-entered` (what was restored) |
| MobEggs | `mob-captured` |
| PocketPlugins | `crafting-grid-stored`, `crafting-grid-restored`, `crafting-grid-dropped`, `armor-swapped`, `biome-bomb-thrown`, `biome-bomb-landed`, `mob-head-taken`, `bedrock-breaking`, `wood-pile-converted`, `inventory-inspected`, `inspected-item-cloned` |

## Recording from a plugin

Use mc-utils' `Audit`, and add `softdepend: [AuditLog]` to the plugin's `plugin.yml`:

```java
Audit.event(plugin, "pile-created").with("owner", player).at(center).with("items", stacks).record();
```

- Every player among the values gets the record. Players, entities, locations, items, item arrays,
  lists and maps are all written readably.
- Record on the main thread, before the items change: they're read on the spot.
- Without AuditLog installed, `record()` does nothing. Call `Audit.warnIfMissing(this)` from
  `onEnable` so the server log says so once.
