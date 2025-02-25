package com.wildwoodsmp.currency.bukkit.cmd;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PayCommand extends CurrencyCommand {

    public PayCommand(@NotNull Currency currency) {
        super(currency, "pay");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        if (!this.currency.allowsPay()) {
            throw new UnsupportedOperationException("This currency does not support paying other players");
        }

        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("Only players can use this command");
            return;
        }

        if (args.length < 2) {
            commandSender.sendMessage("Usage: /" + label + " <player> <amount>");
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getServer().getPlayer(targetName);

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

        if (amount <= 0) {
            sendLang(commandSender, "invalid-amount", new Placeholders().add("amount", args[1]));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!this.currency.has(player.getUniqueId(), amount)) {
                sendLang(commandSender, "insufficient-funds", new Placeholders().add("amount", amount));
                return;
            }

            UUID linkerId = UUID.randomUUID();
            this.currency.withdraw(player.getUniqueId(), amount, "Payment to " + target.getName(), linkerId, "Linked to payment to " + target.getName());
            this.currency.deposit(target.getUniqueId(), amount, "Payment from " + player.getName(), linkerId, "Linked to payment from " + player.getName());

            sendLang(commandSender, "payment-success", new Placeholders().add("amount", amount).add("player", target.getName()));
        });
    }
}
