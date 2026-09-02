package ru.raskol.caravans.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.raskol.caravans.model.Caravan;
import ru.raskol.caravans.service.CaravanService;

import java.util.List;

public final class CaravanGui {

    private CaravanGui() {}

    public static Inventory build(Player p, CaravanService svc) {
        List<Caravan> list = svc.byOwner(p.getUniqueId());
        int size = Math.min(54, Math.max(9, ((list.size() + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(new Holder(list), size,
                cc("&6⛺ Ваши караваны"));
        long now = System.currentTimeMillis();

        for (int i = 0; i < list.size() && i < size; i++) {
            Caravan c = list.get(i);
            boolean ready = c.isReady(now);
            ItemStack stack = new ItemStack(ready ? Material.EMERALD_BLOCK : Material.CHEST);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                CaravanService.TierSpec t = svc.getTiers().get(c.getTier());
                meta.setDisplayName(cc((ready ? "&a" : "&e") + c.getRoute()));
                long hoursLeft = Math.max(0, (c.getArriveMillis() - now) / 3_600_000L);
                meta.setLore(List.of(
                        cc("&7Уровень: " + (t != null ? t.name : String.valueOf(c.getTier()))),
                        cc("&7Вложено: &e" + (long) c.getAmount() + "⚜"),
                        cc("&7Стража: &e" + c.getGuards()),
                        cc(ready ? "&aВЕРНУЛСЯ — кликните, чтобы забрать"
                                 : "&7В пути, осталось ~" + hoursLeft + " ч")
                ));
                stack.setItemMeta(meta);
            }
            inv.setItem(i, stack);
        }
        return inv;
    }

    /** Трансляция &-кодов в цвета Minecraft */
    private static String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static final class Holder implements InventoryHolder {
        private final List<Caravan> list;
        public Holder(List<Caravan> list) { this.list = list; }
        public String getId(int slot) { return slot >= 0 && slot < list.size() ? list.get(slot).getId() : null; }
        @Override public Inventory getInventory() { return null; }
    }
}
