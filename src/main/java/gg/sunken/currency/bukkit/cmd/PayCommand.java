package gg.sunken.currency.bukkit.cmd;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.util.Placeholders;
import gg.sunken.currency.util.Predicates;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@Log
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

        if (target.getUniqueId() == player.getUniqueId()) {
            sendLang(commandSender, "cannot-pay-yourself", Placeholders.EMPTY);
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
                sendLang(commandSender, "insufficient-funds", new Placeholders().add("amount", currency.format(amount)));
                return;
            }

            UUID linkerId = UUID.randomUUID();
            this.currency.transaction(() -> {
                this.currency.withdraw(player.getUniqueId(), amount, "Payment to " + target.getName(), linkerId, "Linked to payment to " + target.getName());
                this.currency.deposit(target.getUniqueId(), amount, "Payment from " + player.getName(), linkerId, "Linked to payment from " + player.getName());
            }).thenAccept(success -> {
                if (!success) {
                    sendLang(commandSender, "payment-failed", new Placeholders().add("error", "Transaction failed"));
                    log.warning("Failed to process payment: Transaction failed for " + player.getName() + " to " + target.getName() + " of " + amount + " " + this.currency.name());
                    return;
                }

                this.currency.forCacheUser(target.getUniqueId(), user -> {
                    user.deposit(this.currency, amount, "Payment from " + player.getName(), linkerId, "Linked to payment from " + player.getName());
                });
                this.currency.forCacheUser(player.getUniqueId(), user -> {
                    user.withdraw(this.currency, amount, "Payment to " + target.getName(), linkerId, "Linked to payment to " + target.getName());
                });

                sendLang(commandSender, "payment-success", new Placeholders().add("amount", amount).add("player", target.getName()));
            }).exceptionally(throwable -> {
                sendLang(commandSender, "payment-failed", new Placeholders().add("error", throwable.getMessage()));
                log.warning("Failed to process payment: " + throwable.getMessage() + "\n" + throwable.getStackTrace());
                return null;
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
        } else if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().filter(Predicates.CAN_SEE_PLAYER).map(Player::getName).toList();
        } else if (args.length == 2) {
            List<String> list = List.of("100", "1000", "10000", "100000", "1000000", "10000000", "100000000", "1000000000");
            return list.stream().filter(s -> s.startsWith(args[1])).toList();
        }
        return List.of();
    }
}
