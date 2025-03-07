package com.wildwoodsmp.currency.bukkit.cmd.ecoadmin;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.api.CurrencyTransaction;
import com.wildwoodsmp.currency.bukkit.cmd.CurrencyCommand;
import com.wildwoodsmp.currency.util.Placeholders;
import com.wildwoodsmp.currency.util.Predicates;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class EcoAdminTransactionHistoryCommand extends CurrencyCommand {

    public EcoAdminTransactionHistoryCommand(@NotNull Currency currency) {
        super(currency, "transactionhistory", "history", "hist");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {

        OfflinePlayer target;
        if (args.length == 0) {
            if (!(commandSender instanceof OfflinePlayer)) {
                sendLang(commandSender, "invalid-usage", Placeholders.EMPTY);
                return;
            }
            target = (OfflinePlayer) commandSender;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
        }

        int page;

        if (args.length <= 1) {
            page = 1;
        } else {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendLang(commandSender, "invalid-page", new Placeholders().add("page", args[0]));
                return;
            }
        }

        if (page < 1) {
            sendLang(commandSender, "invalid-page", new Placeholders().add("page", args[0]));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<CurrencyTransaction> transactions = currency.getTransactions(target.getUniqueId(), 3, (page - 1) * 3);

            if (transactions.isEmpty()) {
                sendLang(commandSender, "transactionhistory-empty", new Placeholders().add("player", target.getName()));
                return;
            }

            long transactionsCount = currency.transactionsCount(target.getUniqueId());
            sendLang(commandSender, "transactionhistory-header", new Placeholders()
                    .add("player", target.getName())
                    .add("page", String.valueOf(page))
                    .add("total", String.valueOf(transactionsCount))
            );

            for (CurrencyTransaction transaction : transactions) {
                sendLang(commandSender, "transactionhistory-line", new Placeholders()
                        .add("id", String.valueOf(transaction.id()))
                        .add("amount", currency.format(transaction.amount()))
                        .add("reason", transaction.reason())
                        .add("timestamp", transaction.timestamp().toString())
                        .add("type", transaction.type().name())
                        .add("linkerId", transaction.linkerId().map(UUID::toString).orElse("N/A"))
                        .add("linkerReason", transaction.linkerReason().orElse("N/A"))
                        .add("user", Bukkit.getOfflinePlayer(transaction.user()).getName())
                );
            }

            sendLang(commandSender, "transactionhistory-footer", new Placeholders()
                    .add("player", target.getName())
                    .add("page", String.valueOf(page))
                    .add("total", String.valueOf(transactionsCount))
                    .add("total-pages", String.valueOf((transactionsCount + 2) / 3))
            );
        });
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return Bukkit.getServer().getOnlinePlayers().stream()
                    .filter(Predicates.CAN_SEE_PLAYER)
                    .map(OfflinePlayer::getName)
                    .toList();
        }

        if (args.length == 1) {
            return Bukkit.getServer().getOnlinePlayers().stream()
                    .filter(Predicates.CAN_SEE_PLAYER)
                    .map(OfflinePlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
