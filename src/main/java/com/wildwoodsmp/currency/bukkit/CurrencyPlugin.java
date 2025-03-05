package com.wildwoodsmp.currency.bukkit;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.api.CurrencyApi;
import com.wildwoodsmp.currency.bukkit.cmd.BaseCommand;
import com.wildwoodsmp.currency.impl.WWCurrency;
import com.wildwoodsmp.currency.impl.WWCurrencyService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CurrencyPlugin extends JavaPlugin {

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

        CurrencyApi.setService(new WWCurrencyService(mongoUri, mongoDatabase));

        for (String key : currenciesConfig.getKeys(false)) {
            Currency currency = new WWCurrency(
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
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        CurrencyApi.getService().currencies().forEach((s, currency) -> {
            Bukkit.getCommandMap().getKnownCommands().remove(currency.name());
        });
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
