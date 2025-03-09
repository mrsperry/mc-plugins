package com.mrjoshuasperry.enhanceddungeons.parties;

import com.mrjoshuasperry.enhanceddungeons.Main;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class Party {
    /** The owner of this party */
    private final Player owner;
    /** The party members */
    private final Set<Player> members;
    /** Pending invitations to players */
    private final Set<Player> invites;
    /** The amount of time a player has to accept an invite */
    private final int inviteTime;

    /**
     * Creates a new party
     * @param owner The owner of the party
     * @param inviteTime The amount of time a player has to accept an invite
     */
    protected Party(final Player owner, final int inviteTime) {
        this.owner = owner;
        this.members = new HashSet<>();
        this.invites = new HashSet<>();
        this.inviteTime = inviteTime;

        this.members.add(owner);
    }

    /**
     * Adds a member to the party
     * @param player The player to add
     */
    public void addMember(final Player player) {
        this.members.add(player);
    }

    /**
     * Removes a member from the party
     *
     * If the owner is removed, the party will disband
     * @param player The player to remove
     */
    public void removeMember(final Player player) {
        this.members.remove(player);

        for (final Player member : this.members) {
            if (player == this.owner) {
                member.sendMessage(ChatColor.GRAY + "Your party was disbanded by the owner!");
            } else {
                member.sendMessage(ChatColor.GREEN + player.getName() + ChatColor.GRAY + " has left the party");
            }
        }

        if (player == this.owner) {
            PartyHandler.removeParty(player);
        }
    }

    /**
     * Invites a player to join this party
     * @param player The player to invite
     * @return If the player was invited to the party
     */
    public boolean invite(final Player player) {
        // Disallow sending invites to players that are in a party
        if (PartyHandler.isInParty(player)) {
            return false;
        }

        // Prompt the invitee to join the party
        player.sendMessage(ChatColor.GRAY + "Would you like to join " + ChatColor.AQUA + this.owner.getDisplayName() + ChatColor.GRAY + "'s dungeon party?");
        player.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.GREEN + "/dungeon party accept "
                + ChatColor.GRAY + "within " + ChatColor.GREEN + this.inviteTime + ChatColor.GRAY + " seconds to accept");
        this.invites.add(player);

        // Cancel the invite after a number of seconds
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
            // Only try to remove if the invite is still available
            if (this.invites.contains(player)) {
                this.invites.remove(player);
                player.sendMessage(ChatColor.GRAY + "Your party invite from " + ChatColor.AQUA + this.getOwner().getName() + ChatColor.GRAY + " has expired");
            }
        }, this.inviteTime * 20);

        return true;
    }

    /**
     * Accepts an invite to the party
     * @param player The player who accepted the invite
     * @return If the invite was valid
     */
    public boolean acceptInvite(final Player player) {
        if (this.invites.contains(player)) {
            for (final Player member : this.getMembers()) {
                member.sendMessage(ChatColor.GREEN + player.getName() + ChatColor.GRAY + " joined your party!");
            }

            this.addMember(player);
            this.invites.remove(player);

            return true;
        }

        return false;
    }

    /** @return The owner of the party */
    public Player getOwner() {
        return this.owner;
    }

    /** @return A set of all party members, including the owner */
    public Set<Player> getMembers() {
        return this.members;
    }

    /** @return A set of all invited players */
    public Set<Player> getInvites() {
        return this.invites;
    }
}
