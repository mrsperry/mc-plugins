package com.mrjoshuasperry.enhanceddungeons.parties;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class PartyHandler {
    /** All active parties */
    private static Set<Party> activeParties;
    /** The amount of time a player has to accept a party invite */
    private static int inviteTime;

    /**
     * Loads party information from the config
     * @param config The section used for config values
     */
    public static void initialize(final ConfigurationSection config) {
        PartyHandler.activeParties = new HashSet<>();
        PartyHandler.inviteTime = config.getInt("invite-time", 15);
    }

    /**
     * Creates a new party
     * @param owner The owner of the party
     * @return If the party was created
     */
    public static boolean createParty(final Player owner) {
        for (final Party party : PartyHandler.activeParties) {
            // Check if there is an existing party with the owner
            if (party.getOwner() == owner) {
                return false;
            }
        }

        PartyHandler.activeParties.add(new Party(owner, PartyHandler.inviteTime));
        return true;
    }

    /**
     * Removes a registered party
     * @param owner The owner of the party to remove
     */
    public static void removeParty(final Player owner) {
        PartyHandler.activeParties.removeIf(party -> party.getOwner() == owner);
    }

    /**
     * Finds a party that contains a specific member
     * @param member The member to find
     * @return The party the member is in or null if they are not in a party
     */
    public static Party getPartyByMember(final Player member) {
        for (final Party party : PartyHandler.activeParties) {
            if (party.getMembers().contains(member)) {
                return party;
            }
        }

        return null;
    }

    /**
     * Finds a party that contains a specific invitee
     * @param invitee The invitee to find
     * @return The party the player has been invited to or null if they have no active invites
     */
    public static Party getPartyByInvite(final Player invitee) {
        for (final Party party : PartyHandler.activeParties) {
            if (party.getInvites().contains(invitee)) {
                return party;
            }
        }

        return null;
    }

    /**
     * Finds a party that contains a specific player or creates one if they are not in a party
     * @param player The player to check
     * @return A party containing the player as a member or owner
     */
    public static Party getOrCreateParty(final Player player) {
        if (!PartyHandler.isInParty(player)) {
            PartyHandler.createParty(player);
        }

        return PartyHandler.getPartyByMember(player);
    }

    /**
     * Checks if a player is in a party
     * @param player The player to check
     * @return If the player is in a party
     */
    public static boolean isInParty(final Player player) {
        for (final Party party : PartyHandler.activeParties) {
            if (party.getMembers().contains(player)) {
                return true;
            }
        }

        return false;
    }
}
