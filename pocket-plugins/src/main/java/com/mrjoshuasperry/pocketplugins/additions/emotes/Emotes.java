package com.mrjoshuasperry.pocketplugins.additions.emotes;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.mrjoshuasperry.mcutils.BlockUtils;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class Emotes extends Module implements CommandExecutor {
  private int[][] image = {
      { 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB,
          0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB, 0x87CEEB },
      { 0x87CEEB, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5,
          0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x8A8AE5, 0x87CEEB },
      { 0x8A8AE5, 0x8E8EDF, 0x8E8EDF, 0x8E8EDF, 0x000000, 0x000000, 0x8E8EDF, 0x8E8EDF, 0x8E8EDF, 0x000000, 0x000000,
          0x8E8EDF, 0x8E8EDF, 0x8E8EDF, 0x8E8EDF, 0x8A8AE5 },
      { 0x9191D9, 0x9191D9, 0x9191D9, 0x000000, 0xFFD700, 0xFFD700, 0x000000, 0x9191D9, 0x000000, 0xFFD700, 0xFFD700,
          0x000000, 0x9191D9, 0x9191D9, 0x9191D9, 0x9191D9 },
      { 0x9595D3, 0x9595D3, 0x000000, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0x000000, 0xFFD700, 0xFFD700, 0xFFD700,
          0xFFD700, 0x000000, 0x9595D3, 0x9595D3, 0x9595D3 },
      { 0x9898CD, 0x9898CD, 0x000000, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0x000000, 0xFFD700, 0xFFD700, 0xFFD700,
          0xFFD700, 0x000000, 0x9898CD, 0x9898CD, 0x9898CD },
      { 0x9C9CC7, 0x9C9CC7, 0x9C9CC7, 0x000000, 0xFFD700, 0xFFD700, 0x000000, 0x9C9CC7, 0x000000, 0xFFD700, 0xFFD700,
          0x000000, 0x9C9CC7, 0x9C9CC7, 0x9C9CC7, 0x9C9CC7 },
      { 0x9F9FC1, 0x9F9FC1, 0x9F9FC1, 0x9F9FC1, 0x000000, 0x000000, 0x9F9FC1, 0x9F9FC1, 0x9F9FC1, 0x000000, 0x000000,
          0x9F9FC1, 0x9F9FC1, 0x9F9FC1, 0x9F9FC1, 0x9F9FC1 },
      { 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB,
          0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB, 0xA3A3BB },
      { 0xA6A6B5, 0xA6A6B5, 0xA6A6B5, 0xA6A6B5, 0xA6A6B5, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000,
          0xA6A6B5, 0xA6A6B5, 0xA6A6B5, 0xA6A6B5, 0xA6A6B5 },
      { 0xAAAAAF, 0xAAAAAF, 0xAAAAAF, 0xAAAAAF, 0x000000, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700,
          0x000000, 0xAAAAAF, 0xAAAAAF, 0xAAAAAF, 0xAAAAAF },
      { 0xADADA9, 0xADADA9, 0xADADA9, 0x000000, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700,
          0xFFD700, 0x000000, 0xADADA9, 0xADADA9, 0xADADA9 },
      { 0xB1B1A3, 0xB1B1A3, 0xB1B1A3, 0x000000, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700, 0xFFD700,
          0xFFD700, 0x000000, 0xB1B1A3, 0xB1B1A3, 0xB1B1A3 },
      { 0xB4B49D, 0xB4B49D, 0xB4B49D, 0xB4B49D, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000,
          0x000000, 0xB4B49D, 0xB4B49D, 0xB4B49D, 0xB4B49D },
      { 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897,
          0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897, 0xB8B897 },
      { 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91,
          0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91, 0xBBBB91 }
  };

  private BlockUtils blockUtils;
  private BlockData[][] blockImage;

  public Emotes() {
    super("Emotes");
    this.blockUtils = BlockUtils.getInstance();
    PocketPlugins.getInstance().getCommand("emote").setExecutor(this);
    PocketPlugins.getInstance().getCommand("palette").setExecutor(this);
  }

  @Override
  public void init(YamlConfiguration config) {
    blockImage = new BlockData[16][16];

    for (int x = 0; x < 16; x++) {
      for (int y = 0; y < 16; y++) {
        blockImage[x][y] = blockUtils.getClosestBlockColor(Color.fromRGB(image[x][y]));
      }
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("This command can only be used by players");
      return true;
    }
    Player player = (Player) sender;
    Bukkit.getLogger().info("COMMAND RAN");

    if (command.getName().equals("emote")) {
      Bukkit.getLogger().info("EMOTE");
      Block targetBlock = player.getTargetBlockExact(5);

      if (targetBlock == null) {
        player.sendMessage("You must be looking at a block!");
        return true;
      }

      // Get the starting location (16 blocks above target)
      Location startLoc = targetBlock.getLocation().add(0, 16, 0);

      // Place the blocks
      for (int x = 0; x < 16; x++) {
        for (int y = 0; y < 16; y++) {
          Location blockLoc = startLoc.clone().add(x, -y, 0);
          Bukkit.getLogger().info("(" + x + "," + y + ") Block: " + blockImage[x][y].toString());
          blockLoc.getBlock().setBlockData(blockImage[x][y]);
        }
      }

      player.sendMessage("Emote created!");
    } else if (command.getName().equals("palette")) {
      MapView map = Bukkit.createMap(player.getWorld());
      map.getRenderers().clear(); // Clear default renderers

      // Create custom renderer
      map.addRenderer(new MapRenderer() {
        @Override
        public void render(MapView view, MapCanvas canvas, Player player) {
          // Draw random colors in a 128x128 grid (map size)
          for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
              canvas.setPixelColor(x, y, java.awt.Color.getHSBColor((float) (x * 2.5), (float) (y / 1.4), 5.5f));
            }
          }
        }
      });

      // Create map item and add to frame
      ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
      MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
      mapMeta.setMapView(map);
      mapItem.setItemMeta(mapMeta);

      ItemFrame itemFrame = (ItemFrame) player.getWorld().spawnEntity(player.getLocation(), EntityType.ITEM_FRAME);
      itemFrame.setItem(mapItem);
      itemFrame.setInvulnerable(true);
      itemFrame.setFixed(true);
    }

    return true;
  }
}
