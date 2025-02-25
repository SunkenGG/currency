package com.wildwoodsmp.currency.bukkit.cmd;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.bukkit.CurrencyPlugin;
import com.wildwoodsmp.currency.util.Placeholders;
import com.wildwoodsmp.currency.util.StringUtilsPaper;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class CurrencyCommand extends BukkitCommand {

    protected final static CurrencyPlugin plugin = CurrencyPlugin.getPlugin(CurrencyPlugin.class);
    protected final Currency currency;
    private final List<CurrencyCommand> subCommands = new ArrayList<>();

    public CurrencyCommand(@NotNull Currency currency, @NotNull String... aliases) {
        super(aliases[0]);
        this.currency = currency;
        this.description = currency.name() + " currency command";
        this.usageMessage = "/" + aliases[0];
        for (@NotNull String alias : aliases) {
            this.getAliases().add(alias.toLowerCase());
        }
        this.setPermission("currency." + currency.name().toLowerCase() + ".command." + aliases[0]);
    }

    public void addSubCommand(@NotNull CurrencyCommand command) {
        subCommands.add(command);
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        if (!testPermission(commandSender)) {
            return true;
        }

        if (args.length == 0) {
            executeCommand(commandSender, label, args);
            return true;
        }

        if (subCommands.isEmpty()) {
            executeCommand(commandSender, label, args);
            return true;
        }

        for (CurrencyCommand subCommand : subCommands) {
            if (subCommand.getAliases().contains(args[0].toLowerCase())) {
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);

                subCommand.execute(commandSender, label, newArgs);
                return true;
            }
        }

        executeCommand(commandSender, label, args);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return super.tabComplete(sender, alias, args);
        }

        if (subCommands.isEmpty()) {
            return super.tabComplete(sender, alias, args);
        }

        for (CurrencyCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(args[0])) {
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);

                return subCommand.tabComplete(sender, alias, newArgs);
            }
        }

        return super.tabComplete(sender, alias, args);
    }

    public void sendLang(@NotNull CommandSender sender, String key, Placeholders placeholders) {
        ConfigurationSection section = plugin.getLangConfig().getConfigurationSection(currency.name().toLowerCase());
        if (section == null) {
            return;
        }

        StringUtilsPaper.sendMessage(section.getConfigurationSection(key), sender, placeholders);
    }

    public abstract void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args);
}
