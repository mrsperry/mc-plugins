---
name: upgrade-dependencies
description: Upgrade Paper, the third-party plugins on the dev server (FastAsyncWorldEdit, FastAsyncVoxelSniper), the Maven dependencies and plugins, and the CI actions to their latest stable versions. Use when the user asks to upgrade, update or bump Paper, Minecraft, dependencies or server plugins.
---

# Upgrade dependencies

Latest **stable** only. No alpha, beta, pre, RC, milestone or snapshot builds, from any source.

## 1. See what's available

From the repo root:

```sh
python .claude/skills/upgrade-dependencies/check_updates.py          # newest lines
python .claude/skills/upgrade-dependencies/check_updates.py 26.1.2   # stay on a line
```

It's read-only and reports:

- **Current state:** `paper.version`, MockBukkit, and the third-party jars.
- **The server line:** the newest Paper line with a STABLE build, and its jar's URL and sha256. The
  dev server always moves to this line, even when MockBukkit hasn't caught up.
- **The build line:** the newest line that also has a `-stable` paper-api coordinate and a MockBukkit
  artifact. This is what `paper.version` and MockBukkit point at, because the tests can't start
  without a matching MockBukkit. It often trails the server line by a release for a while after a
  new line ships.
- **Plugins:** FAWE and VoxelSniper's newest Modrinth release for the server line, with URL and
  sha512.
- **Build tooling:** every pinned Maven plugin and dependency, and the GitHub Actions, each with its
  latest stable version.

**Show the user the report before changing anything.** A Paper line bump is the big one: world
upgrades are one-way. Plugin, tooling and same-line Paper build bumps are routine.

## 2. Paper

Follow `UPGRADING.md`, using the values the checker reported. In short:

1. **Branch.** Start from a clean tree on a feature branch (see the `commit` skill).
2. **Server jar** (server line).
   ```sh
   curl -fL -o paper.jar "<url>" && echo "<sha256>  paper.jar" | sha256sum -c
   cp paper.jar _server/paper.jar
   (cd _server && java -Dpaperclip.patchonly=true -jar paper.jar)   # unpacks without booting
   ```
   Back up `_server/world*` before the first boot on a new line.
3. **Poms** (build line).
   - Set `<paper.version>` and `<mockbukkit.version>` in the root `pom.xml`.
   - On a line change, also change the MockBukkit **artifactId** in the root pom **and every
     child pom**. The children declare it versionless, so missing one fails at POM-read time. The
     naming isn't consistent (`mockbukkit-v26.1.2` vs `mockbukkit-v26.2`), so use exactly what the
     checker printed.
   - Don't try to build against the server line with an older MockBukkit. It compiles, but
     MockBukkit can't initialise the newer registries and every server-backed test errors.
   - When the build line trails the server line, say so in the comment above `<paper.version>` and
     in CLAUDE.md's opening line.
4. **`api-version`** (build line). Update it in every `plugin.yml`, reading `currentApiVersion` from
   the build line's API jar as `UPGRADING.md` step 4 shows. It's a minimum, so it loads on newer
   servers.
5. **Pack formats** (server line, since the server reads the pack). Update
   `pack/src/main/{datapack,resourcepack}/pack.mcmeta`: `min_format` and `max_format` take
   `pack_version.data_major` and `resource_major` from
   `unzip -p _server/versions/<ver>/paper-<ver>.jar version.json`.
   A new line can also change the data formats themselves (26.3 renamed `recipe_unlocked`'s
   `recipe` condition to `recipes`). Nothing but a server boot validates the pack, and a pack that
   fails to parse stops the server from starting, so always boot. Compare against vanilla's own
   files in the server jar (`data/minecraft/advancement/recipes/...`) when something won't parse.
6. **Java.** If the line needs a newer Java than the poms compile for, follow `UPGRADING.md` step 4.
7. **The step 6 audit.** On a line change, run the registry and config audit in `UPGRADING.md`
   step 6. That's where the real bugs turn up.
   - The drift tests (`RegistryDriftTest`, `OutfitDriftTest`) only see the **build** line. When the
     server is ahead, diff the server line's additions by hand: list the `static final` constants of
     `EntityType`, `Biome` and `Material` with `javap` in both API jars and `comm` them.
   - A config entry for content only the server line has (e.g. a new biome in
     `villagermakeovers.outfits`) can't go in the shipped `config.yml` yet, because the drift tests
     reject names their registry doesn't know. Put it in the dev server's copy instead, and tell the
     user it's needed on their other servers.
8. **Existing servers keep their old `config.yml`.** Paper never overwrites it, so new list entries
   in the shipped config don't reach a server that already has one. Add them to
   `_server/plugins/<Plugin>/config.yml` by hand, and tell the user which ones their other servers
   need.

## 3. Third-party plugins on the dev server

These live only in `_server/plugins/`, which is gitignored, so there's nothing to commit. They come
from Modrinth. Fill and Modrinth both ask for a descriptive User-Agent, and the checker sends one.
For each plugin with a newer release:

```sh
curl -fL -o "_server/plugins/<filename>" "<url>" \
  && echo "<sha512>  _server/plugins/<filename>" | sha512sum -c
```

Then remove that plugin's old jar. Paper loads every jar it finds, so a leftover version loads
alongside the new one. Leave the plugin's data folder alone; it holds the config.

- **Spark is bundled into Paper.** `_server/plugins/spark/` is its data folder, not a missing jar.
- **Only use Modrinth.** FAWE's CI snapshots (ci.athion.net) are dev builds, and Hangar's listings
  for both plugins are years stale.
- **A new third-party jar** in `_server/plugins/` that the checker doesn't know about: ask the user
  where it comes from, then add its Modrinth slug to `PLUGINS` in `check_updates.py`.

## 4. Build tooling

Bump each `->` the checker shows:

- Maven plugin and dependency versions in the pom it names. Prefer the property when one exists
  (`junit.version`, `maven.surefire.version`).
- Action major tags in `.github/workflows/`.

Read the release notes for any **major** jump before taking it.

## 5. Verify

1. Run `mvn -P all clean test`. Read the compiler output for **deprecation warnings** as well as
   errors; deprecations are the warning for the next break.
2. Run `mvn install -P survival`, boot the dev server, and check the startup log for:
   - plugin load failures
   - `api-version` warnings
   - each plugin's version line
3. Exercise anything the audit touched.

Tell the user what was and wasn't checked in-game. A green `mvn test` alone doesn't verify an
upgrade.

## 6. Commit

Use the `commit` skill.

- A Paper bump is a **revision** bump for every plugin and for `pack` (its formats moved).
- Tooling-only changes ship nothing.
- Keep the mechanical bump and any audit fixes in separate commits, so a revert is clean.
