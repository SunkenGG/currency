package gg.sunken.currency.bukkit.cmd;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.bukkit.CurrencyPlugin;
import gg.sunken.currency.util.Placeholders;
import gg.sunken.currency.util.StringUtilsPaper;
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
    private CurrencyCommand parent;

    public CurrencyCommand(@NotNull Currency currency, @NotNull String... aliases) {
        super(aliases[0]);
        this.parent = null;
        this.currency = currency;
        this.description = currency.name() + " currency command";
        this.usageMessage = "/" + aliases[0];
        this.setAliases(new ArrayList<>());
        for (String alias : aliases) {
            if (alias == null) continue;

            this.getAliases().add(alias.toLowerCase());
        }

        StringBuilder parentPath = new StringBuilder();
        while (parent != null) {
            parentPath.insert(0, parent.getName() + ".");
            parent = parent.getParent();
        }
        this.setPermission("currency." + currency.name().toLowerCase() + ".command." + parentPath + aliases[0]);
    }

    public void addSubCommand(@NotNull CurrencyCommand command) {
        subCommands.add(command);
        command.setParent(this);
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

                if (!subCommand.testPermission(commandSender)) {
                    return true;
                }
                subCommand.execute(commandSender, label, newArgs);
                return true;
            }
        }

        executeCommand(commandSender, label, args);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (!testPermission(sender)) {
            return List.of();
        }

        if (args.length == 0) {
            return this.executeTabComplete(sender, alias, args);
        }

        if (subCommands.isEmpty()) {
            return this.executeTabComplete(sender, alias, args);
        }

        for (CurrencyCommand subCommand : subCommands) {
            if (subCommand.getAliases().contains(args[0].toLowerCase())) {
                if (!subCommand.testPermissionSilent(sender)) {
                    return List.of();
                }

                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);

                return subCommand.tabComplete(sender, alias, newArgs);
            }
        }

        return this.executeTabComplete(sender, alias, args);
    }

    public void sendLang(@NotNull CommandSender sender, String key, Placeholders placeholders) {
        ConfigurationSection section = plugin.getLangConfig().getConfigurationSection(currency.name().toLowerCase());
        if (section == null) {
            return;
        }

        StringUtilsPaper.sendMessage(section.getConfigurationSection(key), sender, placeholders);
    }

    public void sendLang(@NotNull CommandSender sender, String key) {
        sendLang(sender, key, Placeholders.EMPTY);
    }

    public void setParent(CurrencyCommand parent) {
        this.parent = parent;
    }

    public abstract void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args);

    public abstract @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException;
}
