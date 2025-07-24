package gg.sunken.currency.bukkit.cmd;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.bukkit.CurrencyPlugin;
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
            sendErrorOnlyPlayers(commandSender);
            return;
        }

        if (!validateArguments(commandSender, args, label)) {
            return;
        }

        Player target = getPlayer(args[0], commandSender);
        if (target == null) return;

        if (isSelfPayment(player, target, commandSender)) {
            return;
        }

        Double amount = parseAmount(args, commandSender);
        if (amount == null || !validatePositiveAmount(amount, args[1], commandSender)) {
            return;
        }

        if (!acquireTransactionLock(player, commandSender)) {
            return;
        }

        processTransactionAsync(player, target, amount, commandSender);
    }

    private void sendErrorOnlyPlayers(CommandSender sender) {
        sender.sendMessage("Only players can use this command");
    }

    private boolean validateArguments(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " <player> <amount>");
            return false;
        }
        return true;
    }

    private Player getPlayer(String targetName, CommandSender sender) {
        Player target = Bukkit.getServer().getPlayer(targetName);
        if (target == null) {
            sendLang(sender, "player-not-found", new Placeholders().add("player", targetName));
        }
        return target;
    }

    private boolean isSelfPayment(Player player, Player target, CommandSender sender) {
        if (target.getUniqueId().equals(player.getUniqueId())) {
            sendLang(sender, "cannot-pay-yourself", Placeholders.EMPTY);
            return true;
        }
        return false;
    }

    private Double parseAmount(String[] args, CommandSender sender) {
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sendLang(sender, "invalid-amount", new Placeholders().add("amount", args[1]));
            return null;
        }
        return amount;
    }

    private boolean validatePositiveAmount(double amount, String rawAmount, CommandSender sender) {
        if (amount <= 0) {
            sendLang(sender, "invalid-amount", new Placeholders().add("amount", rawAmount));
            return false;
        }
        return true;
    }

    private boolean acquireTransactionLock(Player player, CommandSender sender) {
        UUID uuid = UUID.randomUUID();
        if (CurrencyPlugin.TRANSACTION_LOCK.putIfAbsent(player.getUniqueId(), uuid) != null) {
            sendLang(sender, "transaction-failed", new Placeholders().add("error", "Transaction in progress"));
            return false;
        }

        if (CurrencyPlugin.TRANSACTION_LOCK.get(player.getUniqueId()) != uuid) {
            sendLang(sender, "transaction-failed", new Placeholders().add("error", "Transaction in progress"));
            return false;
        }

        return true;
    }

    private void processTransactionAsync(Player player, Player target, double amount, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!currency.has(player.getUniqueId(), amount)) {
                sendLang(sender, "insufficient-funds", new Placeholders().add("amount", currency.format(amount)));
                cleanupTransactionLock(player);
                return;
            }

            processTransaction(player, target, amount, sender);
        });
    }

    private void processTransaction(Player player, Player target, double amount, CommandSender sender) {
        UUID linkerId = UUID.randomUUID();
        currency.transaction(() -> performPaymentTransaction(player, target, amount, linkerId))
                .thenAccept(success -> handleTransactionResult(success, player, target, amount, sender, linkerId))
          .exceptionally(throwable -> handleTransactionException(throwable, player, target, sender));
    }

    private void performPaymentTransaction(Player player, Player target, double amount, UUID linkerId) {
        currency.withdraw(player.getUniqueId(), amount, "Payment to " + target.getName(), linkerId,
                          "Linked to payment to " + target.getName());
        currency.deposit(target.getUniqueId(), amount, "Payment from " + player.getName(), linkerId,
                         "Linked to payment from " + player.getName());
    }

    private void handleTransactionResult(boolean success, Player player, Player target, double amount, 
                                         CommandSender sender, UUID linkerId) {
        if (!success) {
            sendLang(sender, "payment-failed", new Placeholders().add("error", "Transaction failed"));
            log.warning("Failed to process payment for " + player.getName() + " to " + target.getName());
            cleanupTransactionLock(player);
            return;
        }

        updateCache(player, target, amount, linkerId);
        sendLang(sender, "payment-success", new Placeholders().add("amount", amount).add("player", target.getName()));
        cleanupTransactionLock(player);
    }

    private Void handleTransactionException(Throwable throwable, Player player, Player target, CommandSender sender) {
        sendLang(sender, "payment-failed", new Placeholders().add("error", throwable.getMessage()));
        log.warning("Payment processing failed: " + throwable.getMessage());
        cleanupTransactionLock(player);
        return null;
    }

    private void updateCache(Player player, Player target, double amount, UUID linkerId) {
        currency.forCacheUser(target.getUniqueId(), user -> {
            user.deposit(currency, amount, "Payment from " + player.getName(), linkerId,
                         "Linked to payment from " + player.getName());
        });

        currency.forCacheUser(player.getUniqueId(), user -> {
            user.withdraw(currency, amount, "Payment to " + target.getName(), linkerId,
                          "Linked to payment to " + target.getName());
        });
    }

    private void cleanupTransactionLock(Player player) {
        CurrencyPlugin.TRANSACTION_LOCK.remove(player.getUniqueId());
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return Bukkit.getServer().getOnlinePlayers().stream()
                    .filter(player -> !(sender instanceof Player p && player.getUniqueId().equals(p.getUniqueId())))
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