package gg.sunken.currency.bukkit.cmd;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BalanceCommand extends CurrencyCommand {

    public BalanceCommand(@NotNull Currency currency) {
        super(currency, "balance", "bal");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        OfflinePlayer target;

        if (args.length == 0) {
            if (!(commandSender instanceof Player player)) {
                sendLang(commandSender, "balance-usage", Placeholders.EMPTY);
                return;
            }
            target = player;
        } else {
            if (args.length > 1) {
                sendLang(commandSender, "balance-usage", Placeholders.EMPTY);
                return;
            }
            target = Bukkit.getServer().getOfflinePlayerIfCached(args[0]);
        }

        if (target == null || !target.hasPlayedBefore()) {
            sendLang(commandSender, "player-not-found", new Placeholders().add("player", target == null ? args[0] : target.getName()));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = currency.balance(target.getUniqueId());
            if (commandSender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
                sendLang(commandSender, "balance", new Placeholders().add("balance", currency.format(balance)));
                return;
            }
            sendLang(commandSender, "balance-other", new Placeholders().add("player", target.getName()).add("balance", currency.format(balance)));
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
        return List.of();
    }
}
