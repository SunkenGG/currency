package com.wildwoodsmp.currency.bukkit.cmd;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.bukkit.cmd.eco.EcoGiveCommand;
import com.wildwoodsmp.currency.bukkit.cmd.eco.EcoSetCommand;
import com.wildwoodsmp.currency.bukkit.cmd.eco.EcoTakeCommand;
import com.wildwoodsmp.currency.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

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
}
