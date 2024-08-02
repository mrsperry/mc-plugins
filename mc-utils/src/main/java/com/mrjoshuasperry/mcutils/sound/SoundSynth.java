package com.mrjoshuasperry.mcutils.sound;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SoundSynth {
    List<SoundByte> soundBytes;

    public SoundSynth() {
        soundBytes = new ArrayList<>();
    }

    public void add(SoundByte soundByte) {
        this.soundBytes.add(soundByte);
    }

    public void remove(int index) {
        this.soundBytes.remove(index);
    }

    public void edit(SoundByte soundByte, int index) {
        this.soundBytes.set(index, soundByte);
    }

    public void list(Player player) {
        int index = 0;
        for (SoundByte soundByte : this.soundBytes) {
            player.sendMessage("[" + index + "] " + ChatColor.GRAY + soundByte.toString());
            index++;
        }
    }

    public void play(Plugin plugin, Location location) {
        List<SoundByte> soundBytesClone = new ArrayList<>(this.soundBytes);
        List<SoundByte> toDelete = new ArrayList<>();

        new BukkitRunnable() {
            int time = 0;

            @Override
            public void run() {
                if (soundBytesClone.size() <= 0) {
                    this.cancel();
                }

                for (SoundByte soundByte : soundBytesClone) {
                    if (soundByte.delay == time) {
                        location.getWorld().playSound(location, soundByte.sound, soundByte.volume, soundByte.pitch);
                        toDelete.add(soundByte);
                    }
                }

                for (SoundByte soundByte : toDelete) {
                    soundBytesClone.remove(soundByte);
                }
                toDelete.clear();

                time++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
