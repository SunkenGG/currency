package com.wildwoodsmp.currency.bukkit.cmd;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.bukkit.cmd.eco.EcoGiveCommand;
import com.wildwoodsmp.currency.bukkit.cmd.eco.EcoSetCommand;
import com.wildwoodsmp.currency.bukkit.cmd.eco.EcoTakeCommand;
import com.wildwoodsmp.currency.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EcoCommand extends CurrencyCommand {
    public EcoCommand(@NotNull Currency currency) {
        super(currency, "eco");
        addSubCommand(new EcoGiveCommand(currency));
        addSubCommand(new EcoTakeCommand(currency));
        addSubCommand(new EcoSetCommand(currency));
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        sendLang(commandSender, "help", Placeholders.EMPTY);
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return getSubCommands().stream()
                    .filter(command -> command.testPermission(sender))
                    .flatMap(command -> command.getAliases().stream())
                    .toList();
        }

        return getSubCommands().stream()
                .filter(command -> command.testPermission(sender))
                .flatMap(command -> command.getAliases().stream())
                .filter(subalias -> subalias.startsWith(args[0].toLowerCase()))
                .toList();
    }
}
