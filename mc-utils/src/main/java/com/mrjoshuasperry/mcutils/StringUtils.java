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
      parts.add(part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase());
    }

    return String.join(" ", parts);
  }
}
