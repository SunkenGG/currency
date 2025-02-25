package com.wildwoodsmp.currency.util;

import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.UUID;

public class GeyserUtils {

    private GeyserUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Check if Floodgate is enabled
     *
     * @return true if Floodgate is enabled
     */
    public static boolean isFloodgateEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate");
    }

    /**
     * Get the name of a player by their UUID
     *
     * @param uuid UUID of the player
     * @return Name of the player
     */
    public static String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    /**
     * Get the UUID of a player by their name
     *
     * @param name Name of the player
     * @return UUID of the player
     */
    public static Optional<UUID> getUUID(String name) {

        UUID uniqueId = Bukkit.getOfflinePlayer(name).getUniqueId();
        return Optional.of(uniqueId);
    }

    /**
     * Check if a player is a Bedrock player (Floodgate)
     *
     * @param uuid UUID of the player
     * @return true if the player is a Bedrock player
     */
    public static boolean isUserBedrock(UUID uuid) {
        return uuid.getMostSignificantBits() == 0; // fallback
    }

}
