package com.wildwoodsmp.currency.bukkit.cmd;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BaseCommand extends CurrencyCommand {

    public BaseCommand(@NotNull Currency currency) {
        super(currency, currency.name(), currency.plural());
        addSubCommand(new BalanceCommand(currency));
        if (currency.allowsPay()) {
            addSubCommand(new PayCommand(currency));
        }
        addSubCommand(new EcoCommand(currency));
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        sendLang(commandSender, "help", Placeholders.EMPTY);
    }
}
