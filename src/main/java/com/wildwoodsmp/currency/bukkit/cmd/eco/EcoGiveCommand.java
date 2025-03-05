package com.wildwoodsmp.currency.bukkit.cmd.eco;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.bukkit.cmd.CurrencyCommand;
import com.wildwoodsmp.currency.util.Placeholders;
import com.wildwoodsmp.currency.util.Predicates;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

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

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);

        if (target == null) {
            sendLang(commandSender, "player-not-found", new Placeholders().add("player", targetName));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sendLang(commandSender, "invalid-amount", new Placeholders().add("amount", args[1]));
            return;
        }

        if (amount < 0 && !this.currency.allowsNegatives()) {
            sendLang(commandSender, "negative-balance-not-allowed", new Placeholders().add("amount", args[1]));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

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

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return Bukkit.getServer().getOnlinePlayers().stream()
                    .filter(player -> {
                        if (sender instanceof Player p) {
                            return !player.getUniqueId().equals(p.getUniqueId());
                        }
                        return true;
                    })
                    .map(Player::getName)
                    .toList();
        }

        if (args.length == 1) {
            return Bukkit.getServer().getOnlinePlayers().stream()
                    .filter(player -> {
                        if (sender instanceof Player p) {
                            return !player.getUniqueId().equals(p.getUniqueId());
                        }
                        return true;
                    })
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            List<String> list = List.of("10", "100", "1000");
            return list.stream()
                    .filter(s -> s.startsWith(args[1]))
                    .toList();
        }

        return List.of("reason...");
    }
}
