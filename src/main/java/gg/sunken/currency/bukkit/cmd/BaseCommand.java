package gg.sunken.currency.bukkit.cmd;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BaseCommand extends CurrencyCommand {

    public BaseCommand(@NotNull Currency currency) {
        super(currency, currency.name(), currency.plural());
        addSubCommand(new BalanceCommand(currency));
        addSubCommand(new BalanceTopCommand(currency));
        if (currency.allowsPay()) {
            addSubCommand(new PayCommand(currency));
        }
        addSubCommand(new EcoCommand(currency));
        addSubCommand(new EcoAdminCommand(currency));
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
