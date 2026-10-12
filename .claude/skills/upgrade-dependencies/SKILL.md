---
name: upgrade-dependencies
description: Upgrade Paper, the third-party plugins on the dev server (FastAsyncWorldEdit, FastAsyncVoxelSniper), the Maven dependencies and plugins, and the CI actions to their latest stable versions. Use when the user asks to upgrade, update or bump Paper, Minecraft, dependencies or server plugins.
---

# Upgrade dependencies

Latest **stable** only. No alpha, beta, pre, RC, milestone or snapshot builds, from any source.

## 1. See what's available

From the repo root:

```sh
python .claude/skills/upgrade-dependencies/check_updates.py          # newest usable line
python .claude/skills/upgrade-dependencies/check_updates.py 26.1.2   # stay on a line
```

It's read-only and reports:

- **Current state:** `paper.version`, MockBukkit, what the dev server runs, and the third-party jars.
- **The target Paper line.** That's the newest line with all three of:
  - a STABLE server build on Fill
  - a `-stable` paper-api coordinate
  - a MockBukkit artifact

  Lines that fail are listed with the reason. A new Paper line often goes stable before MockBukkit
  catches up, and without MockBukkit the tests can't run.
- **The line's details:** the server jar's URL and sha256, the `paper.version` coordinate, the
  MockBukkit artifact and version, and the Java the line needs.
- **Plugins:** FAWE and VoxelSniper's newest Modrinth release for that line, with URL and sha512.
- **Build tooling:** every pinned Maven plugin and dependency, and the GitHub Actions, each with its
  latest stable version.

**Show the user the report and agree what to take before changing anything.** A Paper line bump is
the big one: world upgrades are one-way. Plugin, tooling and same-line Paper build bumps are
routine.

## 2. Paper

Follow `UPGRADING.md`, using the values the checker reported. In short:

1. **Branch.** Start from a clean tree on a feature branch (see the `commit` skill).
2. **Server jar.**
   ```sh
   curl -fL -o paper.jar "<url>" && echo "<sha256>  paper.jar" | sha256sum -c
   cp paper.jar _server/paper.jar
   ```
   Back up `_server/world*` before the first boot on a new line.
3. **Poms.**
   - Set `<paper.version>` and `<mockbukkit.version>` in the root `pom.xml`.
   - On a line change, also change the MockBukkit **artifactId** in the root pom **and every
     child pom**. The children declare it versionless, so missing one fails at POM-read time. The
     naming isn't consistent (`mockbukkit-v26.1.2` vs `mockbukkit-v26.2`), so use exactly what the
     checker printed.
4. **`api-version`.** Update it in every `plugin.yml`, reading the value from the API jar as
   `UPGRADING.md` step 4 shows.
5. **Pack formats.** Update `pack/src/main/{datapack,resourcepack}/pack.mcmeta`: `min_format` and
   `max_format` take `pack_version.data_major` and `resource_major` from
   `unzip -p _server/versions/<ver>/paper-<ver>.jar version.json`. That jar exists after the first
   boot.
6. **Java.** If the line needs a newer Java than the poms compile for, follow `UPGRADING.md` step 4.
7. **The step 6 audit.** On a line change, run the registry and config audit in `UPGRADING.md`
   step 6. That's where the real bugs turn up.

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
