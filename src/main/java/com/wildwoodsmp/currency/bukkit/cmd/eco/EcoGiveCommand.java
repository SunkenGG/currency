package com.wildwoodsmp.currency.bukkit.cmd.eco;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.bukkit.cmd.CurrencyCommand;
import com.wildwoodsmp.currency.util.Placeholders;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Log
public class EcoGiveCommand extends CurrencyCommand {

    public EcoGiveCommand(@NotNull Currency currency) {
        super(currency, "give");
    }

    @Override
    // /coins eco give <player> <amount> <reason>
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 3) {
            sendLang(commandSender, "eco-give-usage", Placeholders.EMPTY);
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);

        if (target == null) {
            sendLang(commandSender, "player-not-found", new Placeholders().add("player", targetName));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendLang(commandSender, "invalid-amount", new Placeholders().add("amount", args[2]));
            return;
        }

        if (amount < 0 && !this.currency.allowsNegatives()) {
            sendLang(commandSender, "negative-balance-not-allowed", new Placeholders().add("amount", args[2]));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            this.currency.transaction(() -> {
                this.currency.deposit(target.getUniqueId(), amount, reason);
            }).thenAccept((result) -> {
                if (!result) {
                    sendLang(commandSender, "transaction-failed", Placeholders.EMPTY);
                } else {
                    sendLang(commandSender, "eco-give-success", new Placeholders()
                            .add("player", target.getName())
                            .add("amount", String.valueOf(amount))
                            .add("reason", reason));
                }
            });
        });
    }
}
