package com.mrjoshuasperry.pocketplugins.modules.improvedmaps.renderers;

import java.awt.Color;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class SepiaAtlasRenderer extends MapRenderer {

  private Color transformToSepia(Color originalColor, int x, int z) {
    // Convert to sepia with lighter values
    int r = originalColor.getRed();
    int g = originalColor.getGreen();
    int b = originalColor.getBlue();

    // Lighter sepia transformation
    int tr = (int) (0.293 * r + 0.669 * g + 0.189 * b);
    int tg = (int) (0.249 * r + 0.586 * g + 0.168 * b);
    int tb = (int) (0.172 * r + 0.434 * g + 0.131 * b);

    // Lighten the overall image
    tr = (int) (tr * 1.2);
    tg = (int) (tg * 1.2);
    tb = (int) (tb * 1.2);

    // Ensure values don't exceed 255
    tr = Math.min(tr, 255);
    tg = Math.min(tg, 255);
    tb = Math.min(tb, 255);

    // Add slight variations to make it look more natural
    int variation = (x * 7 + z * 13) % 3 - 1;
    tr = Math.max(0, Math.min(255, tr + variation));
    tg = Math.max(0, Math.min(255, tg + variation));
    tb = Math.max(0, Math.min(255, tb + variation));

    return new Color(tr, tg, tb);
  }

  @Override
  public void render(MapView map, MapCanvas canvas, Player player) {
    for (int x = 0; x < 128; x++) {
      for (int z = 0; z < 128; z++) {
        Color originalColor = canvas.getBasePixelColor(x, z);

        if (originalColor.getAlpha() == 0)
          continue; // Skip unexplored areas

        canvas.setPixelColor(x, z, transformToSepia(originalColor, x, z));
      }
    }
  }
}