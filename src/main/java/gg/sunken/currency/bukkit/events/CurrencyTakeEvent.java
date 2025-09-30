package gg.sunken.currency.bukkit.events;

import gg.sunken.currency.api.Currency;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter @Setter
public class CurrencyTakeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final UUID user;
    private final Currency currency;
    private double amount;
    private boolean cancelled = false;

    public CurrencyTakeEvent(@NotNull UUID who, Currency currency, double amount) {
        this.user = who;
        this.currency = currency;
        this.amount = amount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
