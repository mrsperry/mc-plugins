# Contributing

## Adding a module

Modules are discovered by scanning the jar at runtime — there is no registration list to add
yourself to. A class is picked up as a module only if all of the following hold:

- It lives in `modules/<name>/`, and **its class name matches its package name**,
  case-insensitively. `CropTweaks` must be in package `croptweaks`.
- It extends `Module`.
- It declares a `(ConfigurationSection readable, ConfigurationSection writable)` constructor.

Get any of these wrong and the class is skipped in silence — no warning, no error, the module just
never runs. That is by far the most common way a new module fails to show up.

## Configuration

The loader hands both config sections to the constructor:

- **`readable`** — the module's section of `src/main/resources/config.yml`, named for the class in
  lowercase. Read-only settings. Add a section here if the module has any; a module without one
  still loads and is enabled.
- **`writable`** — `configs/<lowercased class name>.yml`, for state the module owns and updates
  (macros, recipes, play times). Ship a default at `src/main/resources/configs/` and it is copied
  out on first run. Call `saveConfig()` to persist it.

`saveConfig()` writes to disk synchronously on the main thread. Mutate the section freely, but keep
the saves off per-tick timers.

## Event handlers

`Module` implements `Listener`, and the loader registers it for you — don't call `registerEvents`
yourself.

**Don't name a handler after one `Module` already declares.** Bukkit collects handlers by
reflection over the concrete class, so a same-named method silently *replaces* the base one instead
of running alongside it, and whatever the base handler did quietly stops happening for your module.
`Module`'s handlers are `final` so this is a compile error rather than a surprise. For the same
reason they can't be made `private` — Bukkit would stop finding them on subclasses entirely.

**Don't register events or hand out `this` from a constructor.** A subclass assigns its fields only
after `super(...)` returns, so anything reaching the module before then sees them unset. The loader
calls `onEnable()` once construction is complete.

## Style

- Adventure components (`Component.text`, `NamedTextColor`) for all player-facing text. No legacy
  `ChatColor` or `§` codes.
- Persistent data containers via `Module.createKey()` for item and entity state. No NMS.
- The plugin logger over `printStackTrace()`.

## Versioning

When bumping the plugin version, refer to the following:

- Major: sweeping changes to how modules work
- Minor: new modules or total rewrites/removals of old modules
- Revision: fixes and updates to modules and the Paper version
