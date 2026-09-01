package ru.raskol.caravans;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.caravans.cmd.CaravanCommand;
import ru.raskol.caravans.data.CaravanRepository;
import ru.raskol.caravans.gui.CaravanGuiListener;
import ru.raskol.caravans.hook.EconomyHook;
import ru.raskol.caravans.service.CaravanService;

public final class CaravansPlugin extends JavaPlugin {

    private CaravanRepository repository;
    private CaravanService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!EconomyHook.setup(this)) {
            getLogger().severe("Vault не найден — плагин отключён.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        repository = new CaravanRepository(this);
        repository.load();
        service = new CaravanService(this, repository);

        PluginCommand cmd = getCommand("caravan");
        CaravanCommand executor = new CaravanCommand(this, service);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new CaravanGuiListener(this, service), this);
        getServer().getScheduler().runTaskTimer(this, service::notifyReady, 1200L, 1200L);

        getLogger().info("RaskolCaravans включён. Активных караванов: " + repository.getAll().size());
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (repository != null) repository.save();
    }
}
