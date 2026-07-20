# world-clusters

Groups worlds into **clusters** that share one player state, with a hard boundary between clusters.

## The model

A cluster is a set of worlds that share an inventory, XP bar, health, hunger and game mode.

- **Within a cluster**, nothing happens. Overworld → nether → end carries everything, exactly like
  vanilla. Creative → redstone-testing carries everything too.
- **Between clusters**, you cross a *boundary*. Your entire state is captured and stored under the
  cluster you left, you're wiped to a blank player, and the destination cluster's stored state is
  applied. Come back later and everything is where you left it — including where you were standing.

The separating axis is **cluster membership, not game mode**. A cluster can hold worlds of mixed
game modes, and two survival worlds in different clusters still get a boundary between them.

A typical setup:

| Cluster | Worlds | Why |
|---|---|---|
| `survival` | `world`, `world_nether`, `world_the_end` | Normal vanilla progression |
| `creative` | `build`, `redstone` | Building and testing, sealed off from survival |

Worlds not listed in the config fall into an implicit `default` cluster, so a fresh install behaves
like vanilla and forgetting to register a world can't break anything.

## Commands

| Command | What |
|---|---|
| `/worlds` | The world picker. Icons are config-driven; worlds across a boundary say so. |
| `/wc list` | Clusters and their worlds, with load state and role |
| `/wc info <world>` | Environment, seed, role, cluster |
| `/wc tp <world>` | Typed teleport |
| `/wc create <name> <cluster> [environment]` | New world, registered into a cluster |
| `/wc import <name> <cluster>` | Register a folder that already exists |
| `/wc load\|unload <world>` | Bring a world up or down |
| `/wc delete <world>` | Deletes the folder. Requires `/confirm`. |
| `/wc cluster create <id>` | New empty cluster |
| `/wc cluster delete <id>` | Unregisters the cluster and its worlds. Requires `/confirm`. Worlds stay on disk; stored profiles are kept, so recreating the id restores them. |
| `/wc cluster assign <world> <cluster>` | Move a world between clusters |
| `/wc reload` | Re-read `config.yml` |

Permissions: `worldclusters.use` for `/worlds`, `worldclusters.admin` for `/wc`.

## Portals and respawning

Portals route **within the cluster**. A nether portal looks for its own cluster's `nether` world,
not the server's. A cluster with no nether has inert nether portals — better than silently dumping
a builder into the survival nether. Vanilla's 8:1 coordinate scaling still applies.

Respawning likewise stays in-cluster. A bed or anchor is honoured only if it's in the same cluster;
otherwise you get that cluster's overworld spawn. Dying is the worst possible moment to have an
inventory swapped.

## Per-cluster plugin access

**You cannot enable a plugin for one world and not another** — plugins are server-wide. What you can
do is gate the permission nodes they check. List them under a cluster:

```yaml
creative:
  permissions:
    - worldedit.*
    - voxelsniper.*
```

Nodes are granted inside that cluster and set **explicitly false** everywhere else. The explicit
deny matters: an attachment value overrides op's implicit yes, so this works even on an op-only
server with no permissions plugin.

Two caveats:

1. It only works for plugins that actually permission-check their commands. Nearly all do.
2. A wildcard like `worldedit.*` only cascades to child nodes if that plugin registered them as
   children. If `//set` still works where it shouldn't, list the specific nodes instead.

## Config

`config.yml` is read-write — worlds made with `/wc create` are written back. See the shipped file
for the annotated format.

Per-player state lives in `playerdata/<uuid>.yml`, one section per cluster, plus a `current-cluster`
key. That key is what lets the plugin notice you logged in somewhere your inventory doesn't belong
(a world was deleted while you were offline) and swap it out instead of letting it leak.

## Shared utilities

Two reusable pieces live in `mc-utils`, not here:

- `com.mrjoshuasperry.mcutils.menu` — `Menu`, `PaginatedMenu`, `MenuListener`, `MenuManager`.
  Click-guarded by default; construct one menu **per player**, since `MenuItem`s carry mutable
  state.
- `com.mrjoshuasperry.mcutils.confirm` — `ConfirmationManager`. One pending confirmation per player;
  a new request invalidates the old one immediately, and stale ones expire on a timer.
