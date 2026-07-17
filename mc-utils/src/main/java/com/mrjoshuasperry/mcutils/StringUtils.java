package com.mrjoshuasperry.mcutils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {
  public static String capitalize(String string) {
    if (string == null || string.isEmpty()) {
      return string;
    }

    List<String> parts = new ArrayList<>();
    for (String part : string.split(" ")) {
      // split(" ") yields empty tokens for doubled/leading spaces; keep them as-is
      // so spacing is preserved and substring(0, 1) never runs on an empty string
      if (part.isEmpty()) {
        parts.add(part);
        continue;
      }
      parts.add(part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase());
    }

    return String.join(" ", parts);
  }

  public static String toEnumName(String name) {
    if (name == null) {
      return name;
    }

    return name.toUpperCase().replaceAll(" ", "_");
  }
}
