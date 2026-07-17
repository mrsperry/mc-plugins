# CLAUDE.md

## What this is

A Maven monorepo of independent Paper plugins, each building its own jar. The git root is
`mc-plugins/` — `pocket-plugins/` is a module, not the root.

| Path | What |
|---|---|
| `mc-utils` | Shared library, consumed by the others |
| `pocket-plugins` | 25 gameplay modules behind an internal module system |
| `auto-stack`, `chat`, `compressed-mobs`, `death-chest`, `level-up`, `mob-eggs` | Standalone plugins |
| `.archive/` | Retired plugins, kept for reference. Not part of the build |
| `_server/`, `_dist/` | Dev server and build output. Gitignored |

## Building

Child modules are declared **only inside profiles**, so a bare `mvn -pl <module>` fails with
"Could not find the selected project in the reactor". Always build through a profile:

```
mvn -P survival compile     # chat + mc-utils + pocket-plugins
mvn -P creative compile     # chat + mc-utils
```

`mvn install -P <profile>` additionally copies the jars into `_dist/` and `_server/plugins/`.

Tests are JUnit 5 + MockBukkit, run with `mvn -P all test` — the `all` profile aggregates every
module (including the standalone plugins that no other profile reaches) into one reactor. MockBukkit
is pinned to `mockbukkit-v1.21:4.45.0`, the last release built against Paper 1.21.4; bump it in
lockstep with the paper-api upgrade, not before. Coverage is unit tests over pure logic and
config/serialization round-trips — behavioral/gameplay changes still need an in-game check on the
dev server, so say so rather than implying it's verified.

## Version posture

**Paper 1.21.4 / Java 17, deliberately.** Paper has since moved to calendar versioning (26.x) and
Java 25, but the upgrade is not planned and isn't worth re-raising. Target 1.21.4 and don't reach
for newer APIs. Java 17 in particular means no `Math.clamp` — use `Math.max`/`Math.min`.

**Folia is not a goal.** Don't migrate the existing Bukkit scheduler calls, and don't treat plain
`HashMap` shared state as a bug — it's correct under Paper's single-threaded model. In new code,
just avoid digging deeper: no new "scan every entity in every world" timers.

## pocket-plugins module system

Full detail in `pocket-plugins/Contributing.md`. The parts worth knowing before touching anything:

- Modules are found by **scanning the jar at runtime**, not from a registry. A class is only picked
  up if its simple name matches its package name case-insensitively (`CropTweaks` in `croptweaks`),
  it extends `Module`, and it has a `(ConfigurationSection, ConfigurationSection)` constructor.
  Violations are skipped **silently**.
- Config is a two-file split: `config.yml` (read-only, one section per module, keyed by lowercased
  class name) and `configs/<module>.yml` (module-owned mutable state).
- `Module` handlers are `final` on purpose. Bukkit collects handlers by reflection over the
  concrete class, so a same-named subclass method silently replaces the base handler — and they
  can't be `private` instead, since Bukkit would then stop finding them on subclasses at all.
- Constructors must not register events or leak `this`; the loader calls `onEnable()` afterward.

## Conventions

- Adventure components for player-facing text. No legacy `ChatColor`, no `§` codes.
- PDC + `NamespacedKey` via `Module.createKey()` for item/entity state. No NMS anywhere — keep it
  that way.
- Plugin logger over `printStackTrace()`.
- Commit messages are a single imperative line in sentence case, with no body.
