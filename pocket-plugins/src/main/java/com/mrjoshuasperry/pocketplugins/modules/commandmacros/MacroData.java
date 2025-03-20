package com.mrjoshuasperry.pocketplugins.modules.commandmacros;

import java.util.List;

public class MacroData {
  private final boolean opOnly;
  private final List<String> commands;

  public MacroData(boolean opOnly, List<String> commands) {
    this.opOnly = opOnly;
    this.commands = commands;
  }

  public boolean isOpOnly() {
    return opOnly;
  }

  public List<String> getCommands() {
    return commands;
  }
}