package ru.raskol.caravans.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.raskol.caravans.CaravansPlugin;
import ru.raskol.caravans.service.CaravanService;

public final class CaravanGuiListener implements Listener {

    private final CaravanService service;
    public CaravanGuiListener(CaravansPlugin plugin, CaravanService service) { this.service = service; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CaravanGui.Holder holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String id = holder.getId(e.getSlot());
        if (id == null) return;
        service.collect(p, id);
        p.openInventory(CaravanGui.build(p, service));
    }
}
