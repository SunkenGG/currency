package com.wildwoodsmp.currency.bukkit.cmd;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BalanceCommand extends CurrencyCommand {

    public BalanceCommand(@NotNull Currency currency) {
        super(currency, "balance", "bal");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        OfflinePlayer target;

        if (args.length == 0) {
            target = (OfflinePlayer) commandSender;
        } else {
            target = Bukkit.getServer().getOfflinePlayerIfCached(args[0]);
        }

        if (target == null || !target.hasPlayedBefore()) {
            sendLang(commandSender, "player-not-found", new Placeholders().add("player", args[0]));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = currency.balance(target.getUniqueId());
            if (commandSender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
                sendLang(commandSender, "balance", new Placeholders().add("balance", balance));
                return;
            }
            sendLang(commandSender, "balance-other", new Placeholders().add("player", target.getName()).add("balance", balance));
        });
    }
}
