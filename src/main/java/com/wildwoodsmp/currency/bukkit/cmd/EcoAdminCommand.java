package com.wildwoodsmp.currency.bukkit.cmd;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.bukkit.cmd.ecoadmin.*;
import com.wildwoodsmp.currency.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EcoAdminCommand extends CurrencyCommand {

    public EcoAdminCommand(@NotNull Currency currency) {
        super(currency, "ecoadmin");
        addSubCommand(new EcoAdminReloadCommand(currency));
        addSubCommand(new EcoAdminDeletedTransactionHistoryCommand(currency));
        addSubCommand(new EcoAdminRecountUserCommand(currency));
        addSubCommand(new EcoAdminRedoCommand(currency));
        addSubCommand(new EcoAdminTransactionHistoryCommand(currency));
        addSubCommand(new EcoAdminUndoCommand(currency));
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        sendLang(commandSender, "admin-help", Placeholders.EMPTY);
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return getSubCommands().stream()
                .filter(command -> command.testPermission(sender))
                .flatMap(command -> command.getAliases().stream())
                .toList();
    }
}
