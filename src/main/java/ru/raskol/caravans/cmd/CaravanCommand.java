package ru.raskol.caravans.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.raskol.caravans.CaravansPlugin;
import ru.raskol.caravans.gui.CaravanGui;
import ru.raskol.caravans.model.Caravan;
import ru.raskol.caravans.service.CaravanService;

import java.util.List;

public final class CaravanCommand implements CommandExecutor, TabCompleter {

    private final CaravanService service;
    public CaravanCommand(CaravansPlugin plugin, CaravanService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("open")) {
            p.openInventory(CaravanGui.build(p, service));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "invest" -> {
                if (args.length < 3) { service.msg(p, "&c/caravan invest <1|2|3> <сумма> [guard]"); return true; }
                boolean guard = args.length >= 4 && args[3].equalsIgnoreCase("guard");
                service.invest(p, parseInt(args[1]), parseDouble(args[2]), guard);
            }
            case "collect" -> {
                if (args.length < 2) { service.msg(p, "&c/caravan collect <id>"); return true; }
                service.collect(p, args[1]);
            }
            case "list" -> {
                List<Caravan> list = service.byOwner(p.getUniqueId());
                if (list.isEmpty()) { service.msg(p, "&cУ вас нет караванов."); return true; }
                for (Caravan c : list) {
                    service.msg(p, "&e" + c.getId() + " &7| " + c.getRoute() + " &7| &e" + c.getAmount() + "⚜ &7| стража " + c.getGuards()
                            + (c.isReady(System.currentTimeMillis()) ? " &a[ГОТОВ]" : " &7[в пути]"));
                }
            }
            default -> service.msg(p, "&c/caravan [gui|invest|collect|list]");
        }
        return true;
    }

    private int parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return -1; } }
    private double parseDouble(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("gui", "invest", "collect", "list");
        if (args.length == 2 && args[0].equalsIgnoreCase("invest")) return List.of("1", "2", "3");
        return List.of();
    }
}
