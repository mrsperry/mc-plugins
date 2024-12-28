package com.mrjoshuasperry.pocketplugins.utils;

import java.util.Random;

import org.bukkit.map.MapCursor;

public class CursorUtils {
  private static Random random = new Random();

  // Define banner marker types
  private static MapCursor.Type[] bannerTypes = {
      MapCursor.Type.BANNER_WHITE,
      MapCursor.Type.BANNER_ORANGE,
      MapCursor.Type.BANNER_MAGENTA,
      MapCursor.Type.BANNER_LIGHT_BLUE,
      MapCursor.Type.BANNER_YELLOW,
      MapCursor.Type.BANNER_LIME,
      MapCursor.Type.BANNER_PINK,
      MapCursor.Type.BANNER_GRAY,
      MapCursor.Type.BANNER_LIGHT_GRAY,
      MapCursor.Type.BANNER_CYAN,
      MapCursor.Type.BANNER_PURPLE,
      MapCursor.Type.BANNER_BLUE,
      MapCursor.Type.BANNER_BROWN,
      MapCursor.Type.BANNER_GREEN,
      MapCursor.Type.BANNER_RED,
      MapCursor.Type.BANNER_BLACK
  };

  public static MapCursor.Type getRandomCursor() {
    return bannerTypes[random.nextInt(bannerTypes.length)];
  }

}
