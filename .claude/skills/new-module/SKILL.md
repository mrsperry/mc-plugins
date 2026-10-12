---
name: new-module
description: Add a new gameplay module to pocket-plugins. Scaffolds the class, its config.yml section, any writable state file, a test and the README rows, then checks the rules that otherwise make a module silently fail to load. Use whenever the user wants a new pocket-plugins feature or module.
---

# New pocket-plugins module

Paths below are relative to `pocket-plugins/`. Java sources live under
`src/main/java/com/mrjoshuasperry/pocketplugins/`.

## 1. Should it be a module?

- **Can the data-pack format express it?** Recipes, loot tables, advancements, tags and predicates
  belong in `pack/` (see the `add-recipe` skill). A module is for behavior that needs code: event
  handlers, per-tick logic, mutable state.
- **Does an existing module already own this area?** A new toggle in `croptweaks`, `dispensery` or
  `mobgriefing` can beat a new module. Check `README.md`'s module table.

## 2. Name it

PascalCase class, same name lowercased for the package and the config key:
`LightningRods` → `modules/lightningrods/LightningRods.java` → `lightningrods:` in `config.yml`.

## 3. Write the class

Match the existing modules: 2-space indent, `protected` fields, a class javadoc describing the
behavior from the player's point of view.

```java
package com.mrjoshuasperry.pocketplugins.modules.lightningrods;

/**
 * Makes a lightning rod actually ground the strike it attracted ...
 */
public class LightningRods extends Module {
  protected int searchRadius;

  public LightningRods(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.searchRadius = readableConfig.getInt("search-radius", 2);
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockIgnite(BlockIgniteEvent event) { ... }
}
```

**Loader rules.** Break any of these and the class is skipped with no log line at all:

- The class name matches the package name, case-insensitively.
- It extends `Module`.
- It has a public `(ConfigurationSection readableConfig, ConfigurationSection writableConfig)`
  constructor.

**Lifecycle rules:**

- The constructor reads config and sets fields only. It must not register events, schedule tasks or
  hand out `this`, because subclass fields aren't assigned yet. Do startup work that touches the
  server in an `onEnable()` override that calls `super.onEnable()` first (see `chunkloaders`). The
  loader registers the listener; don't call `registerEvents` yourself.
- Handlers are `public` and must not reuse a name `Module` declares (`discoverRecipesOnJoin`).
  Those are `final`, so a clash is a compile error.

**What to use:**

| Need | Use |
|---|---|
| Commands | `registerCommand(...)` with Brigadier. See `biomebombs`, `commandmacros`, `mobheads` |
| A recipe that needs code | `registerCraftingRecipe(...)`, which is auto-discovered on join. Plain recipes go in `pack/` |
| Item or entity state | PDC with `createKey("...")` |
| Randomness | `getPlugin().getRandom()`, the shared plugin instance |
| Player text | Adventure components. No `ChatColor`, no `§` |
| Logging | `getPlugin().getLogger()`, never `printStackTrace()` |
| Content lists | A Bukkit `Tag` or a registry predicate, never a hand-written `Material`/`EntityType` list |

## 4. Make everything configurable

**The principle:** any value a server owner could reasonably want different goes in config. That
covers:

- radii, chances, cooldowns, durations and counts
- material and entity lists
- an on/off toggle for each optional sub-behavior
- player-facing message text

Hardcode only what isn't a preference: vanilla mechanics and the internals of the algorithm. When
unsure, make it configurable.

### How config works

Each module gets two sections, passed to its constructor:

| | Readable | Writable |
|---|---|---|
| File | `src/main/resources/config.yml`, under the lowercased class name | `configs/<name>.yml` in the plugin's data folder |
| Holds | Settings. Read-only, never written by code | State the module owns and changes at runtime (placed loaders, play times, macros) |
| Access | the `readableConfig` constructor arg / `getReadableConfig()` | `getWritableConfig()`, then `saveConfig()` to persist |
| Missing | The module still loads, enabled, on code defaults | Starts empty. Ship `src/main/resources/configs/<name>.yml` only if it needs initial content; it's copied out on first run |

- Every module gets `enabled` for free: it defaults to `true`, and `enabled: false` turns the module
  off. Don't add your own.
- `saveConfig()` writes synchronously on the main thread. Call it when state changes, never from a
  per-tick timer. It skips the write when the section is empty.
- For structured state, use a small record with `serialize()` and a static `deserialize(Map)`, and
  store a list of them (see `chunkloaders/Anchor.java`).
- Never put settings in the writable file or state in the readable one.

### Writing the readable section

Insert it in `config.yml` in alphabetical order:

```yaml
lightningrods:
  # How far from a strike to look for the rod that attracted it; fires are only
  # suppressed when one is found
  search-radius: 2
```

- **Keys** are kebab-case: `search-radius`, `water-use-chance`, `can-bonemeal-cactus`.
- **Comment the units and scale** above each key: `# Cooldown is in seconds`,
  `# Chance is out of 100`, `# These chances are a weighted ratio`.
- **Read every key in the constructor with a default equal to the shipped value**, e.g.
  `getInt("search-radius", 2)`. The code default and `config.yml` must never disagree.
- **Material and entity names** are lowercase strings, resolved through the registry or
  `Material.matchMaterial`. Log a warning and skip an entry that doesn't resolve; never throw. A
  renamed type only fails at runtime, and one bad entry shouldn't take the module down.
- **Validate ranges** (`Math.clamp` is available) rather than trusting the file.

## 5. Test

Tests are JUnit + MockBukkit over pure logic. Pull the decision or math out into a `static` or
`protected` method that takes plain values, as `MobSizes.healthToScale(...)` does, and test that.

- Put tests in `src/test/java/com/mrjoshuasperry/pocketplugins/modules/<name>/<Name>Test.java`:
  a package-private class, 2-space indent, descriptive method names.
- Persisted records get a `serialize` → `deserialize` round-trip test (see `AnchorTest`).
- Config parsing that resolves names gets a test showing an invalid entry is skipped, not fatal.

## 6. Document it

In `README.md`:

- Add a row to the **Modules** table, in alphabetical order. Describe what the player does and sees.
- If the module registers a command, add a row to the **Commands** table.

## 7. Verify

Confirm the loader rules mechanically. A miss here is invisible at runtime:

```sh
M=LightningRods; m=$(echo $M | tr A-Z a-z)
F=pocket-plugins/src/main/java/com/mrjoshuasperry/pocketplugins/modules/$m/$M.java
grep -q "^package com.mrjoshuasperry.pocketplugins.modules.$m;" $F && echo "package ok"
grep -q "class $M extends Module" $F && echo "extends ok"
grep -q "public $M(ConfigurationSection [a-zA-Z]*, ConfigurationSection [a-zA-Z]*)" $F && echo "constructor ok"
grep -q "^$m:" pocket-plugins/src/main/resources/config.yml && echo "config section ok"  # only if it has settings
```

Then:

1. Run `mvn -P all test`.
2. Run `mvn install -P survival` and start the dev server. The log must show
   `<Name> enabled!`. If that line is missing, the module was skipped.
3. Exercise the feature in-game.

Tell the user plainly which of these steps ran. Don't imply an in-game check that didn't happen.

A new module is a **minor** version bump for pocket-plugins. The `commit` skill handles that.
