# Mini Additions

A small collection of additions made to enhance the survival Minecraft experience.

## Additions

Each addition comes with its own configuration file and can be turned on or off independently of other additions. Any related configuration options will be available in this file.

### Armor stands

Right clicking on an armor stand with a stick will add configurable arms; sugar will miniaturize it.

### Biome Bombs

Craftable "bombs" that you can throw on the ground to convert the surrounding biome into another.

Bombs have a limited range, and a unique recipe corresponding to the biome it will convert to.

### Cobble Generator

Allows different types of blocks to be formed when water and lava meet. The type of blocks that can form, and their respective chances are configurable.

This replaces vanilla behavior when any lava block spreads into a water block to form cobblestone.

### Concrete Mixer

Allows the use of water-filled cauldrons to convert concrete powder into its hardened form.

### Crafting Keeper

Retains items in crafting tables when the interface closes. This follows the same principle as [Tinkers Construct's](https://tinkers-construct.fandom.com/wiki/Crafting_Station) crafting station.

### Easy Paintings

Allows the cycling of valid paintings by right clicking them.

The dimensions of the paintings available can be changed by surrounding the wall with blocks.

### Easy Sleep

Turns night into day without needing all players to be sleeping. The percentage of online players that must be sleeping can be configured.

This will also reset everyone's phantom spawn timer.

### Feather Plucker

Chickens can be right clicked every so often for a free feather.

### Igneous Generator

A new type of generator that creates andesite, diorite, and granite. This uses water and magma blocks in place of lava.

### Improved Shears

Adds a new type of shears gives a chance to drop a dye item based on the color of the sheep.

### Lead Attacher

Allows you to attach leads to fences without needing an entity on the lead.

### Name Ping

Plays a sound to a player whenever their name is mentioned in the chat.

### No Sheep Griefing

Prevents sheep from converting grass blocks to dirt blocks when they eat grass.

This keeps the functionality of the wool regrowth and the animation of the sheep eating.

### Slimy Boots

Adds a new boot type that protects you from falls (similar to feather falling) and bounces you around as if you fell on a slime block.

### Woodpile

Adds a new multi-block structure that can be created by enclosing wood logs in dirt or grass. Using a flint and steel on one of the surrounding dirt blocks will start a fire and display smoke rising from the pile.

After a configurable amount of time per wood log, each of the logs will be converted to coal blocks.

## NMS Additions

NMS (Net.Minecraft.Server) modules allows for version specific features

### Creating an NMS Module

`NMSFeature.java`

```java
public class NMSFeature extends NMSModule {
  static {
    NMSTest.nmsModuleHandlers.put("1.21.4-R0.1-SNAPSHOT", NMSFeature_v1_21_4.class);
  }

  public NMSFeature() {
    super("NMSFeature");
  }

  public void sharedFunction() {
    Bukkit.getLogger().info("This is a shared function");
  }
}
```

The parent module class is similar to a regular module class with two difference. It extends `NMSModule` and it has an extra static block used to declare supported versions and the classes that correspond to them.

`NMSFeature_v1_21_4.java`

```java
public class NMSFeature_v1_21_4 extends Module {
  private NMSFeature shared;

  public NMSFeature_v1_21_4(String name, NMSModule _shared) {
    super(name);
    this.shared = (NMSFeature) _shared;
  }

  @Override
  public void init(YamlConfiguration configuration) {
    super.init(configuration);
    Bukkit.getLogger().info("This is version v1.21.4");
    this.shared.sharedFunction();
  }
}
```

The version specific modules are set up like any other module except for one change. The constructor must accept both a `String` and `NMSModule`. The `NMSModule` will be the parent module that can be used to access shared code that is not version depndent.

`PocketPlugins.java`

```java
        ArrayList<Module> modules = Lists.newArrayList(
            ...
            new NMSFeature(),
            ...
            );
```

The module is registered just like any other module with only the parent module needing to be added to the `modules` array list.
