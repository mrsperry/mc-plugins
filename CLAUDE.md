# CLAUDE.md

Maven monorepo of Paper plugins, each building its own jar. Java 25. The dev server runs Paper 26.3;
plugins build against the 26.2 API, the newest line MockBukkit supports. The git root is `mc-plugins/`.

| Path | What |
|---|---|
| `mc-utils` | Shared library, shaded into the plugins that use it |
| `pocket-plugins` | Gameplay modules behind an internal module system — rules in `pocket-plugins/Contributing.md` |
| `auto-stack`, `chat`, `compressed-mobs`, `death-chest`, `level-up`, `mob-eggs`, `world-clusters` | Standalone plugins |
| `pack` | Companion data pack + resource pack. Zips only, no jar |
| `.archive/` | Retired plugins. Not built |
| `_server/`, `_dist/` | Dev server and build output. Gitignored |

## Building

Modules are declared only inside profiles, so a bare `mvn -pl <module>` fails. Always use a profile:

```
mvn -P all test               # every module — also what the pre-commit hook runs
mvn install -P survival       # also copies jars into _dist/ and _server/plugins/
```

Stop the dev server before `mvn install`: Windows locks the jars a running server has loaded, so the
install fails when it tries to replace them.

Tests are JUnit + MockBukkit over pure logic and config round-trips. Gameplay changes still need an
in-game check on the dev server — say so rather than implying they're verified.

## Rules

- **Never touch any running server except the dev server** (`_server/`, port 3005). Other servers on
  this machine are live: don't stop, restart, connect to, send commands to or edit them, even to free
  a port.
- Anything the data-pack format can express (recipes, loot tables, advancements, tags) goes in
  `pack/`, not a plugin.
- Prefer a Bukkit `Tag` or registry-derived predicate over a hand-written `Material`/`EntityType`
  list. `RegistryDriftTest` guards the lists that remain.
- Adventure components for player-facing text (no `ChatColor` or `§`), PDC + `NamespacedKey` for
  item/entity state, no NMS, plugin logger over `printStackTrace()`.
- Folia is not a goal. Keep the Bukkit scheduler; plain `HashMap` shared state is correct here.
- Latest **stable** versions only — no betas, milestones or RCs for Paper, dependencies or plugins.
