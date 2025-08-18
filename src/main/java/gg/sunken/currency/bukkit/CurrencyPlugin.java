package gg.sunken.currency.bukkit;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.api.CurrencyApi;
import gg.sunken.currency.bukkit.cmd.BaseCommand;
import gg.sunken.currency.bukkit.vault.VaultEconomy;
import gg.sunken.currency.impl.MongoCurrency;
import gg.sunken.currency.impl.MongoCurrencyService;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CurrencyPlugin extends JavaPlugin {

    public static final Map<UUID, UUID> TRANSACTION_LOCK = new ConcurrentHashMap<>();
    private File currenciesFile;
    @Getter private YamlConfiguration currenciesConfig;

    private File langFile;
    @Getter private YamlConfiguration langConfig;


    @Override
    public void onLoad() {
        saveDefaultConfig();
        saveConfig();
        reloadConfig();

        String mongoUri = getConfig().getString("mongo-uri");
        String mongoDatabase = getConfig().getString("mongo-database");

        CurrencyApi.setService(new MongoCurrencyService(mongoUri, mongoDatabase));

        for (String key : currenciesConfig.getKeys(false)) {
            Currency currency = new MongoCurrency(
                    currenciesConfig.getString(key + ".name"),
                    currenciesConfig.getString(key + ".plural"),
                    currenciesConfig.getString(key + ".symbol"),
                    currenciesConfig.getBoolean(key + ".allows-negatives"),
                    currenciesConfig.getBoolean(key + ".allows-pay"),
                    currenciesConfig.getString(key + ".format"),
                    currenciesConfig.getDouble(key + ".default"),
                    mongoUri,
                    mongoDatabase
            );

            CurrencyApi.getService().addCurrency(currency);
            Bukkit.getCommandMap().register("currency", new BaseCommand(currency));

            if (currency.name().equals("vault")) {
                VaultEconomy vaultEconomy = new VaultEconomy(currency);
                if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                    Bukkit.getServicesManager().register(
                            Economy.class,
                            vaultEconomy,
                            this,
                            ServicePriority.Normal
                    );
                } else {
                    getLogger().warning("Vault is not installed, VaultEconomy will not be registered.");
                }

            }
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        CurrencyApi.get().currencies()
                .forEach((s, currency) ->
                        Bukkit.getCommandMap().getKnownCommands().remove(currency.name())
                );
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        currenciesFile = new File(getDataFolder(), "currencies.yml");
        if (!currenciesFile.exists()) {
            saveResource("currencies.yml", false);
        }

        currenciesConfig = YamlConfiguration.loadConfiguration(currenciesFile);

        langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
}
