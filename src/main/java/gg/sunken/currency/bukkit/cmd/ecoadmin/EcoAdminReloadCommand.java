package gg.sunken.currency.bukkit.cmd.ecoadmin;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.bukkit.cmd.CurrencyCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EcoAdminReloadCommand extends CurrencyCommand {
    public EcoAdminReloadCommand(@NotNull Currency currency) {
        super(currency, "reload");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        long start = System.currentTimeMillis();
        plugin.reloadConfig();
        plugin.onDisable();
        plugin.onLoad();
        plugin.onEnable();
        commandSender.sendMessage(ChatColor.GREEN + "Currency Reloaded Successfully! Took " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return List.of();
    }
}
