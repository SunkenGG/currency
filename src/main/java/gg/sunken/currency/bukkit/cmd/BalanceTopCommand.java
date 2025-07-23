package gg.sunken.currency.bukkit.cmd;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.api.CurrencyUser;
import gg.sunken.currency.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BalanceTopCommand extends CurrencyCommand {

    public BalanceTopCommand(@NotNull Currency currency) {
        super(currency, "baltop");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sendLang(commandSender, "invalid-page", new Placeholders().add("page", args[0]));
                return;
            }
        }

        if (page < 1) {
            sendLang(commandSender, "invalid-page", new Placeholders().add("page", args[0]));
            return;
        }

        int finalPage = page;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<CurrencyUser> balances = currency.getTopBalances(15, (finalPage-1) * 15);

            if (balances.isEmpty()) {
                sendLang(commandSender, "baltop-empty", Placeholders.EMPTY);
                return;
            }

            long userCount = currency.currencyUserCount();
            sendLang(commandSender, "baltop-header", new Placeholders()
                    .add("page", String.valueOf(finalPage))
                    .add("total", String.valueOf(userCount))
            );

            for (int i = 0; i < balances.size(); i++) {
                CurrencyUser user = balances.get(i);
                sendLang(commandSender, "baltop-line", new Placeholders()
                        .add("rank", String.valueOf((finalPage - 1) * 15 + i + 1))
                        .add("player", Bukkit.getOfflinePlayer(user.userId()).getName())
                        .add("balance", currency.format(user.balance(currency)))
                );
            }

            sendLang(commandSender, "baltop-footer", new Placeholders()
                    .add("page", String.valueOf(finalPage))
                    .add("total", String.valueOf(userCount))
                    .add("total-pages", String.valueOf((userCount + 14) / 15))
            );
        });
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return List.of();
    }
}
