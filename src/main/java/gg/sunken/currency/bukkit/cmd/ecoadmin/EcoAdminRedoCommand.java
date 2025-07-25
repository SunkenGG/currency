package gg.sunken.currency.bukkit.cmd.ecoadmin;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.bukkit.cmd.CurrencyCommand;
import gg.sunken.currency.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class EcoAdminRedoCommand extends CurrencyCommand {

    public EcoAdminRedoCommand(@NotNull Currency currency) {
        super(currency, "redo");
    }

    @Override
    public void executeCommand(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sendLang(commandSender, "invalid-args", Placeholders.EMPTY);
            return;
        }

        UUID id = UUID.fromString(args[0]);
        try {
            currency.transaction(() -> currency.validateTransaction(id));
        } catch (Exception e) {
            sendLang(commandSender, "invalid-args", Placeholders.EMPTY);
        }

        sendLang(commandSender, "redo-success", new Placeholders().add("id", id.toString()));
    }

    @Override
    public @NotNull List<String> executeTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return List.of();
    }
}
