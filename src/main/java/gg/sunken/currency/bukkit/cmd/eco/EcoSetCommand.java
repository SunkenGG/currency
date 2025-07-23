package gg.sunken.currency.bukkit.cmd.eco;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.bukkit.cmd.CurrencyCommand;
import gg.sunken.currency.util.Placeholders;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@Log
public class EcoSetCommand extends CurrencyCommand {

    public EcoSetCommand(@NotNull Currency currency) {
        super(currency, "set");
    }

    @Override
    // /coins eco set <player> <amount> <reason>
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 3) {
            sendLang(commandSender, "eco-set-usage", Placeholders.EMPTY);
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
            sendLang(commandSender, "invalid-amount", new Placeholders().add("amount", args[2]));
            return;
        }

        if (amount < 0 && !this.currency.allowsNegatives()) {
            sendLang(commandSender, "negative-balance-not-allowed", new Placeholders().add("amount", args[2]));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            this.currency.transaction(() -> {
                this.currency.set(target.getUniqueId(), amount, reason);
            }).thenAccept((result) -> {
                if (!result) {
                    sendLang(commandSender, "eco-set-error", new Placeholders()
                            .add("player", targetName)
                            .add("error", "Transaction failed"));
                    return;
                }

                this.currency.forCacheUser(target.getUniqueId(), (user) -> {
                    user.set(this.currency, amount, reason);
                });

                sendLang(commandSender, "eco-set-success", new Placeholders()
                        .add("player", targetName)
                        .add("amount", this.currency.format(amount))
                        .add("reason", reason));

            }).exceptionally((e) -> {
                sendLang(commandSender, "eco-set-error", new Placeholders()
                        .add("player", targetName)
                        .add("error", e.getMessage()));
                log.warning("Failed to process eco set: " + e.getMessage() + " for " + targetName + " to " + amount + " " + this.currency.name());
                log.warning("Stack trace: " + Arrays.toString(e.getStackTrace()));
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
            return List.of("100", "1000", "10000", "100000", "1000000", "10000000", "100000000", "1000000000")
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of("reason...");
    }
}
