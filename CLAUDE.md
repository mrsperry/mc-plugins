# CLAUDE.md

## What this is

A Maven monorepo of independent Paper plugins, each building its own jar. The git root is
`mc-plugins/` — `pocket-plugins/` is a module, not the root.

| Path | What |
|---|---|
| `mc-utils` | Shared library, consumed by the others |
| `pocket-plugins` | 27 gameplay modules behind an internal module system |
| `auto-stack`, `chat`, `compressed-mobs`, `death-chest`, `level-up`, `mob-eggs` | Standalone plugins |
| `pack` | Companion data pack + resource pack. No sources, no jar — zips only |
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

Tests are JUnit 6 + MockBukkit, run with `mvn -P all test` — the `all` profile aggregates every
module (including the standalone plugins that no other profile reaches) into one reactor. MockBukkit
is `mockbukkit-v26.1.2:4.114.0`; it declares no transitive paper-api of its own, so the
provided-scope paper-api from dependencyManagement is what lands on the test classpath. Bump it
alongside the paper-api version. Coverage is unit tests over pure logic and config/serialization
round-trips — behavioral/gameplay changes still need an in-game check on the dev server, so say so
rather than implying it's verified.

`RegistryDriftTest` in mc-utils diffs the hand-maintained content tables (tools, saplings, mobs)
against the live registry, so content added by a Paper upgrade fails the build instead of silently
falling out of whatever the table feeds. Prefer a Bukkit `Tag` or a registry-derived predicate over
a hand-rolled `Material`/`EntityType` list wherever one exists.

## Version posture

**Paper 26.1.2 / Java 25.** Tracks the latest Minecraft release. Keep dependencies, Maven plugins,
and the CI toolchain on their latest **stable** versions — avoid the pre-release lines
(`maven-compiler-plugin:4.0.0` is beta, `surefire:3.6.0` is a milestone; Paper's `26.2` line is
still BETA). Java 25 means `Math.clamp` and the other 18–25 APIs are available.

Paper's calendar versioning changed the `paper-api` coordinate: the `26.x` line pins a specific
build (`26.1.2.build.74-stable`, set once via the `paper.version` property) instead of resolving a
rolling `{MC}-R0.1-SNAPSHOT`. The MockBukkit **artifactId** encodes the line too
(`mockbukkit-v26.1.2`), so it changes in the parent *and* all eight child poms on a line bump.
`UPGRADING.md` is the full runbook — follow it rather than editing versions ad hoc.

**Folia is not a goal — and won't be.** This is a standard single-threaded Paper server, not a
massively-multiplayer multi-threaded one. Don't migrate the Bukkit scheduler calls, and don't treat
plain `HashMap` shared state as a bug — it's correct under Paper's single-threaded model.

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

## Data pack vs plugin

Anything the vanilla data-pack format can express belongs in `pack/`, not in a plugin — recipes,
loot tables, advancements, tags, predicates, item model definitions. Reach for a plugin module only
when the behavior needs code: event handlers, per-tick logic, mutable state. `pack/README.md` has
the current format details (pack formats track `paper.version`; the directory names are singular
since 1.21 — `recipe/`, `advancement/`, `loot_table/`).

Data pack recipes need a sibling unlock advancement to show up in the recipe book; plugin-registered
ones get that from `Module`'s join-time `discoverRecipe` loop instead.

## Conventions

- Adventure components for player-facing text. No legacy `ChatColor`, no `§` codes.
- PDC + `NamespacedKey` via `Module.createKey()` for item/entity state. No NMS anywhere — keep it
  that way.
- Plugin logger over `printStackTrace()`.
- Commit messages are a single imperative line in sentence case, with no body.
