package ru.raskol.caravans.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.caravans.model.Caravan;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CaravanRepository {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Caravan> caravans = new ConcurrentHashMap<>();

    public CaravanRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "caravans.yml");
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("caravans");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) continue;
                caravans.put(id, new Caravan(id,
                        UUID.fromString(s.getString("owner")),
                        s.getInt("tier", 1),
                        s.getString("route", ""),
                        s.getDouble("amount", 0),
                        s.getInt("guards", 0),
                        s.getLong("depart", System.currentTimeMillis()),
                        s.getLong("arrive", System.currentTimeMillis())));
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось загрузить караван " + id);
            }
        }
        plugin.getLogger().info("Загружено караванов: " + caravans.size());
    }

    public synchronized void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Caravan c : caravans.values()) {
            String p = "caravans." + c.getId();
            cfg.set(p + ".owner", c.getOwner().toString());
            cfg.set(p + ".tier", c.getTier());
            cfg.set(p + ".route", c.getRoute());
            cfg.set(p + ".amount", c.getAmount());
            cfg.set(p + ".guards", c.getGuards());
            cfg.set(p + ".depart", c.getDepartMillis());
            cfg.set(p + ".arrive", c.getArriveMillis());
        }
        try { cfg.save(file); } catch (IOException e) { plugin.getLogger().severe("Не удалось сохранить caravans.yml"); }
    }

    public void saveAsync() { Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save); }
    public Caravan get(String id) { return caravans.get(id); }
    public Collection<Caravan> getAll() { return caravans.values(); }
    public void add(Caravan c) { caravans.put(c.getId(), c); }
    public void remove(String id) { caravans.remove(id); }
}
