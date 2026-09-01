package ru.raskol.caravans.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyHook {

    private static Economy economy;

    private EconomyHook() {
    }

    public static boolean setup(JavaPlugin plugin) {
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public static Economy get() {
        return economy;
    }
}
