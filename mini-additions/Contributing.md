# Contributing
## Additions
Each addition should have its own package under `additions` and a corresponding YML configuration file in the `resources` folder.

Additions must also be registered in the main class `MiniAdditions` modules list in order to read values from its configuration file.

## Versioning
When bumping the plugin version, refer to the following:

- Major: sweeping changes to how modules work
- Minor: new modules or total rewrites/removals of old modules
- Revision: fixes and updates to modules and the Spigot version