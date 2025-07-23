package gg.sunken.currency.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class Predicates {
    public static Predicate<CommandSender> CAN_SEE_PLAYER = sender -> {
        if (sender instanceof Player player) {
            return player.canSee(player);
        }
        return true;
    };

    public static Predicate<CommandSender> CAN_SEE_PLAYER_NOT_SELF = sender -> {
        if (sender instanceof Player player) {
            return player.canSee(player) && !player.getUniqueId().equals(player.getUniqueId());
        }
        return true;
    };
}
