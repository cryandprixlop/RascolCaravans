package ru.raskol.caravans.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.raskol.caravans.CaravansPlugin;
import ru.raskol.caravans.data.CaravanRepository;
import ru.raskol.caravans.hook.EconomyHook;
import ru.raskol.caravans.model.Caravan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class CaravanService {

    public static final class TierSpec {
        public final int tier; public final String name; public final double min; public final double max;
        public final int hours; public final double success; public final double profitMin; public final double profitMax;
        public final boolean guardsAllowed; public final List<String> routes;
        TierSpec(int t, String n, double mn, double mx, int h, double s, double pmin, double pmax, boolean g, List<String> r) {
            tier=t; name=n; min=mn; max=mx; hours=h; success=s; profitMin=pmin; profitMax=pmax; guardsAllowed=g; routes=r;
        }
    }

    private final CaravansPlugin plugin;
    private final CaravanRepository repository;
    private final Economy economy;
    private final Map<Integer, TierSpec> tiers = new HashMap<>();
    private final Random random = new Random();
    private final Set<String> notified = new HashSet<>();

    public CaravanService(CaravansPlugin plugin, CaravanRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.economy = EconomyHook.get();
        loadTiers();
    }

    private void loadTiers() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("tiers");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            tiers.put(Integer.parseInt(key), new TierSpec(
                    Integer.parseInt(key),
                    s.getString("name", "Караван"),
                    s.getDouble("min", 1), s.getDouble("max", 20000),
                    s.getInt("hours", 12), s.getDouble("success", 90),
                    s.getDouble("profit-min", 5), s.getDouble("profit-max", 12),
                    s.getBoolean("guards-allowed", false),
                    s.getStringList("routes")));
        }
    }

    public Map<Integer, TierSpec> getTiers() { return tiers; }

    public void invest(Player p, int tier, double amount, boolean wantGuard) {
        TierSpec t = tiers.get(tier);
        if (t == null) { msg(p, "&cНет такого уровня."); return; }
        if (amount < t.min || amount > t.max) { msg(p, "&cДиапазон уровня " + tier + ": &e" + t.min + "–" + t.max + "⚜"); return; }
        if (byOwner(p.getUniqueId()).size() >= plugin.getConfig().getInt("caravan.max-active-per-player", 3)) {
            msg(p, "&cДостигнут лимит активных караванов."); return;
        }
        int guards = 0; double guardCost = 0;
        if (wantGuard) {
            if (!t.guardsAllowed) { msg(p, "&cНа этом уровне стража недоступна."); return; }
            guards = plugin.getConfig().getInt("caravan.max-guards", 1);
            double maxProfit = amount * t.profitMax / 100.0;
            guardCost = round2(maxProfit * plugin.getConfig().getInt("caravan.guard-cost-percent-of-max-profit", 10) / 100.0 * guards);
        }
        double total = round2(amount + guardCost);
        if (!economy.withdrawPlayer(p, total).transactionSuccess()) { msg(p, "&cНужно &e" + total + "⚜&c."); return; }

        long now = System.currentTimeMillis();
        String route = t.routes.isEmpty() ? "Тракт" : t.routes.get(random.nextInt(t.routes.size()));
        Caravan c = new Caravan(UUID.randomUUID().toString().substring(0, 8), p.getUniqueId(),
                tier, route, amount, guards, now, now + t.hours * 3_600_000L);
        repository.add(c); repository.saveAsync();
        msg(p, "&aКараван &e" + route + " &a(" + t.name + "&a) ушёл в путь на &e" + t.hours + " ч&a. Вложено &e" + amount + "⚜&a"
                + (guards > 0 ? " + стража &e" + guards + " &a(-" + guardCost + "⚜)" : "") + ".");
    }

    public void collect(Player p, String id) {
        Caravan c = repository.get(id);
        if (c == null || !c.getOwner().equals(p.getUniqueId())) { msg(p, "&cКараван не найден."); return; }
        if (!c.isReady(System.currentTimeMillis())) { msg(p, "&cКараван ещё в пути."); return; }
        TierSpec t = tiers.get(c.getTier());

        double chance = Math.min(plugin.getConfig().getInt("caravan.max-success-chance", 95),
                t.success + c.getGuards() * plugin.getConfig().getInt("caravan.guard-success-bonus", 15));
        boolean success = random.nextDouble() * 100 < chance;
        double payout;
        if (success) {
            double pct = t.profitMin + random.nextDouble() * (t.profitMax - t.profitMin);
            double profit = round2(c.getAmount() * pct / 100.0);
            payout = round2(c.getAmount() + profit);
            msg(p, "&a✅ Караван &e" + c.getRoute() + " &aвернулся с прибылью &e" + profit + "⚜ &a(" + String.format("%.1f", pct) + "%)! Выплачено &e" + payout + "⚜&a.");
        } else {
            payout = round2(c.getAmount() * (100 - plugin.getConfig().getInt("caravan.fail-loss-percent", 50)) / 100.0);
            msg(p, "&c❌ Караван &e" + c.getRoute() + " &cразграблен! Возвращено только &e" + payout + "⚜&c.");
        }
        if (payout > 0) economy.depositPlayer(p, payout);
        repository.remove(id); notified.remove(id); repository.saveAsync();
    }

    public void notifyReady() {
        long now = System.currentTimeMillis();
        for (Caravan c : repository.getAll()) {
            if (c.isReady(now) && !notified.contains(c.getId())) {
                Player p = Bukkit.getPlayer(c.getOwner());
                if (p != null) { msg(p, "&6⛺ Ваш караван &e" + c.getRoute() + " &6вернулся! Заберите выручку: /caravan"); notified.add(c.getId()); }
            }
        }
    }

    public List<Caravan> byOwner(UUID uuid) {
        List<Caravan> list = new ArrayList<>();
        for (Caravan c : repository.getAll()) if (c.getOwner().equals(uuid)) list.add(c);
        return list;
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    public void msg(Player p, String text) { p.sendMessage(ChatColor.translateAlternateColorCodes('&', text)); }
}
