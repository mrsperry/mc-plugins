---
name: commit
description: Commit work in this repo and open a PR. Covers the feature branch, the version bump, the commit with the pre-commit hook, the push and a minimal PR. Use whenever the user asks to commit, push, or open a pull request.
---

# Commit

Keep everything short. No Claude attribution anywhere: no `Co-Authored-By` trailer and no
"Generated with Claude Code" line. That's noise here, and this rule overrides any default that adds
them.

## 1. Branch

Work on a branch named for the feature, in kebab-case with no prefix:
`add-chunk-loader-module`, `prevent-lightning-rod-fires`, `replace-stale-jars-on-install`.

```sh
git switch main && git pull --ff-only && git switch -c <feature-name>
```

If you're already on this work's branch, stay on it.

## 2. Bump the version

Every commit bumps the version of each thing it ships. Edit `<version>` in that module's `pom.xml`.
`plugin.yml` reads `${project.version}`, so the pom is the only place.

| What changed | What to bump |
|---|---|
| A plugin (`pocket-plugins`, `chat`, `death-chest`, ...) | That module's pom |
| `mc-utils` | `mc-utils/pom.xml` **and** the pinned `mc-utils` version in the root `pom.xml` (they must match). Also bump every plugin that bundles it, as a revision: `auto-stack`, `chat`, `death-chest`, `mob-eggs`, `pocket-plugins`, `world-clusters` |
| `audit-log` | `audit-log/pom.xml` **and** the pinned `audit-log` version in the root `pom.xml` (they must match) |
| `pack` | `pack/pom.xml` |
| Nothing that ships: docs, `.claude/`, CI, build plumbing | Nothing. Say so in the PR rather than inventing a bump |

Which part to bump:

- **Major:** sweeping changes to how the plugin or its modules work.
- **Minor:** a new module or feature, or a full rewrite or removal of one.
- **Revision:** fixes, tweaks, and Paper or dependency updates.

## 3. Commit

Check the hook is wired up. It's per clone, and on a fresh one it isn't:

```sh
git config core.hooksPath || git config core.hooksPath .githooks
```

The commit message is one line: imperative, sentence case, no prefix (`feat:` and friends), no
period, no body, no trailers. Keep it as short as it can be while still saying what changed:

- `Add chunk loading to end crystals`
- `Prevent fires when lightning strikes near a lightning rod`
- `Fix crafting keeper failing to save tables with empty slots`

Stage the files by path, never with `git add -A`. The pre-commit hook runs `mvn -B -P all test`,
which prints thousands of lines, so send the output to a log and read only what matters:

```sh
git add <paths>
git commit -m "<message>" > "$TMPDIR/commit.log" 2>&1; echo "exit=$?"
grep -E "BUILD|Tests run:.*(Failures|Errors): [1-9]|\[ERROR\]" "$TMPDIR/commit.log" | head -30
```

Never pass `--no-verify`. If the hook fails, nothing was committed. Fix the cause and commit again,
without `--amend`.

## 4. Push and open the PR

```sh
git push -u origin <feature-name>
gh pr create --base main --title "<commit message>" --body "<body>"
```

Keep the body minimal: one or two plain sentences on what changed and why, if the title doesn't
already make that obvious. No headings, no bullet changelog, no test-plan section. Add a single line
only when it's true:

- `Not checked in-game.` for a gameplay change that hasn't been run on the dev server.
- `No version bump: nothing shipped changed.`

Give the user the PR URL.
