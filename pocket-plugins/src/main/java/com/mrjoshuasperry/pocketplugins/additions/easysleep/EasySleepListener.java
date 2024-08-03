package com.mrjoshuasperry.pocketplugins.additions.easysleep;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import com.mrjoshuasperry.pocketplugins.MiniAdditions;

public class EasySleepListener extends Module {
    private final List<UUID> sleeping;
    private double threshold;
    private BukkitTask wakeTask = null;

    public EasySleepListener() {
        super("EasySleep");
        this.sleeping = new ArrayList<>();
    }

    @Override
    public void init(YamlConfiguration config) {
        super.init(config);
        this.threshold = config.getDouble("threshold", 0.25);
    }

    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        if (!event.getBedEnterResult().equals(PlayerBedEnterEvent.BedEnterResult.OK)) {
            return;
        }

        if (!this.sleeping.contains(event.getPlayer().getUniqueId())) {
            this.sleeping.add(event.getPlayer().getUniqueId());
            Bukkit.broadcastMessage(
                    ChatColor.YELLOW + event.getPlayer().getName() + " is now sleeping. " + getPlayersInBed());
        }

        if ((double) this.sleeping.size() / (double) Bukkit.getOnlinePlayers().size() >= this.threshold) {

            wakeTask = Bukkit.getScheduler().runTaskLater(MiniAdditions.getInstance(), () -> {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Wakey wakey, eggs and bakey.");
                World world = event.getPlayer().getWorld();
                world.setTime(0);
                world.setStorm(false);

                for (UUID id : this.sleeping) {
                    final Player player = Bukkit.getPlayer(id);
                    if (player == null) {
                        continue;
                    }

                    player.setStatistic(Statistic.TIME_SINCE_REST, 0);
                }
                this.sleeping.clear();
            }, 100);
        }
    }

    @EventHandler
    public void onPlayerWake(PlayerBedLeaveEvent event) {
        if (this.sleeping.contains(event.getPlayer().getUniqueId())) {
            this.sleeping.remove(event.getPlayer().getUniqueId());
            Bukkit.broadcastMessage(
                    ChatColor.YELLOW + event.getPlayer().getName() + " is no longer sleeping. " + getPlayersInBed());
            if (wakeTask != null) {
                wakeTask.cancel();
                wakeTask = null;
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (this.sleeping.contains(event.getPlayer().getUniqueId())) {
            this.sleeping.remove(event.getPlayer().getUniqueId());
            Bukkit.broadcastMessage(ChatColor.YELLOW + event.getPlayer().getName()
                    + " mysteriously vanished. They are no longer sleeping. " + getPlayersInBed());
            if (wakeTask != null) {
                wakeTask.cancel();
                wakeTask = null;
            }
        }
    }

    private String getPlayersInBed() {
        return "(" + ChatColor.GREEN + sleeping.size() + ChatColor.YELLOW + "/" + ChatColor.GREEN
                + Bukkit.getOnlinePlayers().size() + ChatColor.YELLOW + ")";
    }
}
