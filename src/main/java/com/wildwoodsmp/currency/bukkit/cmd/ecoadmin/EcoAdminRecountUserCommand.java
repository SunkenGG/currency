package com.wildwoodsmp.currency.bukkit.cmd.ecoadmin;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.bukkit.cmd.CurrencyCommand;
import com.wildwoodsmp.currency.util.Placeholders;
import com.wildwoodsmp.currency.util.Predicates;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class EcoAdminRecountUserCommand extends CurrencyCommand {
    public EcoAdminRecountUserCommand(@NotNull Currency currency) {
        super(currency, "recountuser");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sendLang(commandSender, "invalid-usage");
            return;
        }

        String playerName = args[0];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null) {
            sendLang(commandSender, "player-not-found", new Placeholders().add("player", playerName));
            return;
        }

        currency.transaction(() -> currency.recalculateBalance(offlinePlayer.getUniqueId(), currency)).thenAccept((result) -> {;
            if (!result) {
                sendLang(commandSender, "transaction-failed", Placeholders.EMPTY);
            } else {
                sendLang(commandSender, "recountuser-success", new Placeholders().add("player", playerName));
            }
        });
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList());
        }

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().filter(Predicates.CAN_SEE_PLAYER).map(OfflinePlayer::getName).collect(Collectors.toList());
        }

        return List.of();
    }
}
