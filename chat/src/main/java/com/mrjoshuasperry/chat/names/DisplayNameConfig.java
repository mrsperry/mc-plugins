package com.mrjoshuasperry.chat.names;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.mcutils.TextColors;

import net.kyori.adventure.text.format.TextColor;

public class DisplayNameConfig {
    private List<TextColor> nameColors;
    private String prefix;
    private List<TextColor> prefixColors;
    private String suffix;
    private List<TextColor> suffixColors;

    public DisplayNameConfig() {
        this(new ArrayList<>(), "", new ArrayList<>(), "", new ArrayList<>());
    }

    public DisplayNameConfig(List<TextColor> nameColors, String prefix, List<TextColor> prefixColors, String suffix,
            List<TextColor> suffixColors) {
        this.nameColors = nameColors;
        this.prefix = prefix;
        this.prefixColors = prefixColors;
        this.suffix = suffix;
        this.suffixColors = suffixColors;
    }

    public static DisplayNameConfig loadFromConfig(ConfigurationSection config) {
        List<String> nameColorStrings = config.getStringList("name-colors");
        if (nameColorStrings == null) {
            nameColorStrings = new ArrayList<>();
        }

        List<String> prefixColorStrings = config.getStringList("prefix-colors");
        if (prefixColorStrings == null) {
            prefixColorStrings = new ArrayList<>();
        }

        List<String> suffixColorStrings = config.getStringList("suffix-colors");
        if (suffixColorStrings == null) {
            suffixColorStrings = new ArrayList<>();
        }

        List<TextColor> nameColors = nameColorStrings.stream()
                .map(TextColors::parseTextColor)
                .toList();

        String prefix = config.getString("prefix", "");
        List<TextColor> prefixColors = prefixColorStrings.stream()
                .map(TextColors::parseTextColor)
                .toList();

        String suffix = config.getString("suffix", "");
        List<TextColor> suffixColors = suffixColorStrings.stream()
                .map(TextColors::parseTextColor)
                .toList();

        return new DisplayNameConfig(nameColors, prefix, prefixColors, suffix, suffixColors);
    }

    public void saveToConfig(ConfigurationSection config) {
        config.set("name-colors", this.nameColors);
        config.set("prefix", this.prefix);
        config.set("prefix-colors", this.prefixColors);
        config.set("suffix", this.suffix);
        config.set("suffix-colors", this.suffixColors);
    }

    public List<TextColor> getNameColors() {
        return Lists.newArrayList(nameColors);
    }

    public void setNameColors(List<TextColor> nameColors) {
        this.nameColors = nameColors;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        if (prefix == null) {
            this.prefix = null;
            return;
        }

        this.prefix = prefix.trim();

        if (this.prefix.isEmpty()) {
            this.prefix = null;
        }
    }

    public List<TextColor> getPrefixColors() {
        return Lists.newArrayList(prefixColors);
    }

    public void setPrefixColors(List<TextColor> prefixColors) {
        this.prefixColors = prefixColors;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        if (suffix == null) {
            this.suffix = null;
            return;
        }

        this.suffix = suffix.trim();

        if (this.suffix.isEmpty()) {
            this.suffix = null;
        }
    }

    public List<TextColor> getSuffixColors() {
        return Lists.newArrayList(suffixColors);
    }

    public void setSuffixColors(List<TextColor> suffixColors) {
        this.suffixColors = suffixColors;
    }
}
