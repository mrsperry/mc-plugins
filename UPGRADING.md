# Upgrading Paper

Runbook for moving the repo to a new Paper/Minecraft version. Steps 1–5 are mechanical; step 6 is
the part that actually catches bugs.

Point Claude at this file: *"follow UPGRADING.md"*, or *"run the step 6 audit"* on its own.

---

## 1. Preflight

```sh
git status                      # must be clean — this touches many files
grep -n "paper-api" -A1 pom.xml # current target
cat _server/version_history.json # what the dev server actually runs
```

If the pom version and `version_history.json` disagree, note it — the server and the compile target
have drifted apart and the upgrade is really two upgrades.

## 2. Resolve the target version

Paper's fill API is the source of truth:

```sh
# All version families
curl -s https://fill.papermc.io/v3/projects/paper | jq .versions

# Latest build for a family — check the "channel" field
curl -s https://fill.papermc.io/v3/projects/paper/versions/<VERSION>/builds/latest | jq '{id, channel, url: .downloads."server:default".url}'
```

Take a version only when `channel` is `STABLE`. `BETA`/`ALPHA` families are not upgrade targets.

**Then confirm a matching `paper-api` exists before committing to it** — the server jar ships ahead
of the API artifact, and the coordinate scheme differs by line:

```sh
curl -s https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml \
  | grep -o "<version>[^<]*</version>" | tail -30
```

- `1.21.x` line → `1.21.11-R0.1-SNAPSHOT` (rolling snapshot)
- `26.x` line → `26.1.2.build.74-stable` (pinned per build; there is **no** `-R0.1-SNAPSHOT`)

Pick the highest `-stable` build in the target family. If nothing `-stable` exists for it yet, stop —
upgrade the server jar only, or wait.

## 3. Swap the server jar

```sh
URL=$(curl -s https://fill.papermc.io/v3/projects/paper/versions/<VERSION>/builds/latest | jq -r '.downloads."server:default".url')
curl -L -o paper.jar "$URL"
sha256sum paper.jar   # compare to .downloads."server:default".checksums.sha256
cp paper.jar _server/paper.jar
```

`mvn install -P <profile>` also copies root `paper.jar` → `_server/`, so the `cp` is belt-and-braces.

Back up `_server/world*` before first boot on a new version — world format migrations are one-way.

## 4. Bump the build

| File | What |
|---|---|
| `pom.xml` | `<paper.version>` — the single source for the `paper-api` version |
| `pom.xml` | `<mockbukkit.version>` |
| `pom.xml` **and all 8 child poms** | `mockbukkit-v<line>` **artifactId** — see below |
| `*/src/main/resources/plugin.yml` (7 files) | `api-version` |
| `CLAUDE.md` | "Version posture" and the MockBukkit note |
| `repo.md`, `.github/workflows/tests.yml` (comment) | version references |

**The MockBukkit artifactId is the trap.** It encodes the Paper line (`mockbukkit-v1.21` →
`mockbukkit-v26.1.2`), so it is not covered by the version property. The child poms declare it
*versionless*, relying on dependencyManagement — so changing it only in the parent makes all eight
modules fail to resolve at POM-read time, before compiling. Change it everywhere at once:

```sh
grep -rln "mockbukkit-v" --include=pom.xml . | grep -v .archive | xargs sed -i 's|mockbukkit-v<OLD>|mockbukkit-v<NEW>|'
```

MockBukkit must exist for the target line or tests won't run at all — list
`https://repo1.maven.org/maven2/org/mockbukkit/mockbukkit/` before starting, and check whether the
artifact still carries a transitive `paper-api` (`mockbukkit-v26.1.2` does not; `mockbukkit-v1.21`
did). `dependency-reduced-pom.xml` files are shade output; ignore them, they regenerate.

`api-version` in `plugin.yml` takes the value the target line reports as `currentApiVersion` —
read it straight from the API jar rather than guessing:

```sh
unzip -p _server/libraries/io/papermc/paper/paper-api/<VERSION>/paper-api-<VERSION>.jar apiVersioning.json
```

Old values keep loading (`"1.21"` plugins ran fine on a 26.1.2 server), so this is forward hygiene
rather than a hard break — which also means a stale value won't announce itself.

Also bump Java if the version requires it: `maven-compiler-plugin/<release>`,
`maven.compiler.source`/`target`, and `.github/workflows/tests.yml` `java-version`.

## 5. Build and test

```sh
mvn -P all clean test      # every module in one reactor
mvn install -P survival    # jars → _dist/ and _server/plugins/
```

Read the compiler output for **deprecation warnings**, not just errors — deprecations are the
advance notice for the next break.

## 6. Codebase audit — new content vs. hardcoded tables

The plugins enumerate game content by hand in a number of places. New mobs, wood types, tools, and
blocks do **not** cause compile errors — they cause silently-incomplete behavior. Sweep these:

### Hardcoded registries (highest risk)

| File | Table | Goes stale when |
|---|---|---|
**`RegistryDriftTest` (mc-utils) now covers the mc-utils tables automatically** — mobs, tool tiers
and saplings all fail the build on drift. What remains is the tables it cannot derive:

| File | Table | Goes stale when | Guarded? |
|---|---|---|---|
| `mc-utils/.../types/EntityTypes.java` | hostile/neutral mobs | any mob is added | derived from registry |
| `mc-utils/.../types/ToolTypes.java` | per-tier and per-class tools | a tool tier is added | drift test |
| `mc-utils/.../types/CropTypes.java` | saplings | a wood type is added | drift test |
| `pocket-plugins/**/config.yml` | `EntityType`/`Material` **names as config keys** | a type is *renamed* | **no** |
| `pocket-plugins/.../mobheads/` `config.yml` | head texture hashes per mob | a mob is added | **no** |
| `mc-utils/.../BreedingUtils.java` | breeding food per mob | a breedable mob is added | no (dead code) |
| `mc-utils/.../RandomItems.java` | item blacklist | broad `Material` additions | no (dead code) |

Config-file enumerations are the most dangerous of these: they are strings, so a **renamed** enum
constant fails at runtime, not compile time, and only ever surfaces as a `severe` log line. The
`compressed-mobs` `creatures` key was still `mushroom_cow` long after the constant became
`MOOSHROOM`; `EntityType.valueOf("MUSHROOM_COW")` throws on 26.1.2 (verified), and both handlers
early-return when a mob has no `Settings`. Grep the configs for type names and round-trip them
through `valueOf` after every upgrade.

Tables judged *intentionally* partial — don't "fix" these: `MobHeads.VANILLA_HEADS` (only six mob
head **items** exist), `NATURAL_HEAD_DROPPERS` (wither skeleton only), `ConcreteMixer` (all 16 dyes
present), `Dispensery` (three fillable cauldrons), and `ColorTypes` — which is an HTML/CSS palette
(olive, teal, fuchsia), not Minecraft's dye colors, and so cannot drift at all.

To find the full set:

```sh
# Hardcoded tables in code
grep -rn "EnumSet.of\|Lists.newArrayList(\s*Material\|Arrays.asList(Material\|Map.of(EntityType" \
  --include=*.java . | grep -v .archive

# Type names used as config keys — these fail at runtime, not compile time
grep -rn "valueOf(.*toUpperCase\|Material.matchMaterial" --include=*.java . | grep -v .archive
```

### Diffing against the new registry

The reliable check is to enumerate the *actual* enum from the new paper-api and diff it against the
hardcoded list, rather than reasoning from the changelog. E.g. for mobs: list every `EntityType`
that is `isAlive() && isSpawnable()` and subtract what `EntityTypes.getAllTypes()` returns; anything
left over is a gap. A throwaway JUnit test against the new MockBukkit is the cheapest way to do it,
and a permanent one is better — see "Follow-ups" below.

**Prefer Bukkit `Tag`s over hand-rolled lists** wherever one exists (`Tag.LOGS_THAT_BURN`,
`Tag.PLANKS`, `Tag.WOOL`, …). Tags come from the server and update themselves; that's the real fix
for wood/stone/color families. `WoodPileConstruct.isValidLog()` already does this correctly.

### API surface

- Deprecations and removals flagged by the compiler (step 5).
- Registry-vs-enum migrations — Bukkit keeps converting enums (`Enchantment`, `Attribute`,
  `PotionEffectType`, `Biome`, `Villager.Profession`) into `Registry` lookups. Constant references
  compile until they don't.
- Serialization compatibility. `death-chest` and `beeplanter` persist data; Bukkit's
  `serializeAsBytes` payloads are version-stamped and read back through data fixers. Verify a chest
  saved on the **old** version still opens on the new one — the round-trip unit tests only prove
  new↔new.
- `NamespacedKey`/PDC keys are stable across versions; no action, just don't rename them.

## 7. In-game verification

Unit tests cover pure logic and config round-trips only. Behavioral changes need the dev server:

```sh
cd _server && java -jar paper.jar nogui
```

Check the startup log for plugin load failures and `api-version` warnings, then exercise the modules
touched by the audit. Don't report an upgrade as verified on a green `mvn test` alone.

## 8. Commit

Single imperative line, sentence case, no body — e.g. `Upgrade to Paper 26.1.2`. Separate the
mechanical bump from any audit fixes so a revert is clean.

---

## Follow-ups worth doing once

- ~~Registry-drift tests~~ — done, `RegistryDriftTest` in mc-utils.
- ~~A `<paper.version>` property~~ — done.
- **A config-key round-trip test.** The `compressed-mobs` outage would have been caught by a test
  that loads each shipped `config.yml` and asserts every type name still resolves. This is the
  biggest remaining unguarded surface.
- **A startup warning for unmapped mobs in MobHeads** — iterate the registry, warn for any `Mob`
  with neither a `VANILLA_HEADS` entry nor a texture. The hashes are external data that can't be
  derived, but the *gap* can be detected automatically instead of by a player noticing no drop.
- **Delete or replace `BreedingUtils` and `RandomItems`.** Both are stale, both have no non-archived
  callers, and both have registry-backed replacements (`Animals#isBreedItem`, `Material#isItem`).
  The live `AutoBreeding` module already uses the former.
- **Replace list-based tables with `Tag` lookups** where a tag exists — `Tag.ITEMS_HOES` and friends
  could retire `ToolTypes` entirely; `Tag.FENCES` would tidy `LeadAttacher`.
