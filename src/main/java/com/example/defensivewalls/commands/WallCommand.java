package com.example.defensivewalls.commands;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /wall give|reload|remove|info|maintenance|cabinet|repair (solo administradores). */
public final class WallCommand implements CommandExecutor, TabCompleter {

    private final DefensiveWallsPlugin plugin;

    public WallCommand(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(plugin.settings().permAdmin())) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "reload" -> {
                int repaired = plugin.reloadAll();
                plugin.messages().send(sender, "reloaded", "%structures%", String.valueOf(plugin.structures().count()),
                        "%repaired%", String.valueOf(repaired));
            }
            case "remove" -> withTarget(sender, (p, s) -> {
                String name = plugin.settings().structureName(s.type(), s.tier());
                Location c = plugin.structures().center(s);
                plugin.structures().destroy(s, false, true);
                if (plugin.getConfig().getBoolean("tools.drop-item-on-admin-remove", false) && c != null) {
                    c.getWorld().dropItemNaturally(c, plugin.items().create(s.option(), 1));
                }
                plugin.messages().send(p, "removed", "%structure%", name);
            });
            case "info" -> withTarget(sender, (p, s) -> {
                plugin.report().structureLines(s).forEach(l -> p.sendMessage(Text.plain(l)));
                plugin.holograms().force(p, s);
            });
            case "maintenance" -> maintenance(sender);
            case "cabinet" -> cabinet(sender, args);
            case "repair" -> withTarget(sender, (p, s) -> {
                boolean all = args.length > 1 && args[1].equalsIgnoreCase("all") && s.type() == StructureType.CABINET;
                List<Structure> list = all ? plugin.structures().linked(s.id()) : List.of(s);
                for (Structure t : list) {
                    plugin.durability().repair(t, t.maxDurability());
                    t.setDecayAccumulator(0);
                }
                plugin.messages().send(p, "repaired",
                        "%structure%", all ? list.size() + " estructuras" : plugin.settings().structureName(s.type(), s.tier()),
                        "%durability%", String.valueOf(s.durability()), "%max%", String.valueOf(s.maxDurability()));
            });
            default -> plugin.messages().send(sender, "usage");
        }
        return true;
    }

    private interface TargetAction {
        void run(Player player, Structure structure);
    }

    private Structure target(Player p) {
        Block b = p.getTargetBlockExact(10);
        return plugin.structures().at(b);
    }

    private void withTarget(CommandSender sender, TargetAction action) {
        if (!(sender instanceof Player p)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        Structure s = target(p);
        if (s == null) {
            plugin.messages().send(p, "no-target");
            return;
        }
        action.run(p, s);
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "usage");
            return;
        }
        StructureOption option = plugin.shapes().parse(args[1]);
        if (option == null) {
            plugin.messages().send(sender, "unknown-type");
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                amount = -1;
            }
            if (amount < 1 || amount > 64) {
                plugin.messages().send(sender, "invalid-amount");
                return;
            }
        }
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                plugin.messages().send(sender, "player-not-found");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            plugin.messages().send(sender, "player-only");
            return;
        }
        ItemStack stack = plugin.items().create(option, amount);
        target.getInventory().addItem(stack).values()
                .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        plugin.messages().send(sender, "give-success", "%amount%", String.valueOf(amount),
                "%structure%", plugin.settings().structureName(option.type(), option.tier()),
                "%player%", target.getName());
    }

    private void maintenance(CommandSender sender) {
        List<String> lines;
        if (sender instanceof Player p && target(p) != null) {
            Structure s = target(p);
            lines = new ArrayList<>(plugin.report().structureLines(s));
            if (s.type() == StructureType.CABINET) {
                lines.addAll(plugin.report().cabinetLines(s));
            }
        } else {
            lines = plugin.report().globalLines();
        }
        lines.forEach(l -> sender.sendMessage(Text.plain(l)));
    }

    private void cabinet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        Structure s = target(p);
        if (s == null || s.type() != StructureType.CABINET) {
            plugin.messages().send(p, "cabinet-not-found");
            return;
        }
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "open";
        switch (sub) {
            case "link" -> {
                boolean force = args.length > 2 && args[2].equalsIgnoreCase("force");
                int n = plugin.cabinets().adoptNearby(s, force);
                plugin.messages().send(p, n > 0 ? "cabinet-linked" : "cabinet-none-nearby", "%count%", String.valueOf(n));
            }
            case "info" -> plugin.report().cabinetLines(s).forEach(l -> p.sendMessage(Text.plain(l)));
            default -> plugin.guis().openCabinet(p, s);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission(plugin.settings().permAdmin())) {
            return out;
        }
        if (args.length == 1) {
            add(out, args[0], "give", "reload", "remove", "info", "maintenance", "cabinet", "repair");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give" -> plugin.shapes().options().forEach(o -> add(out, args[1], o.id()));
                case "cabinet" -> add(out, args[1], "open", "link", "info");
                case "repair" -> add(out, args[1], "all");
                default -> { }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            add(out, args[2], "1", "16", "64");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("cabinet") && args[1].equalsIgnoreCase("link")) {
            add(out, args[2], "force");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(pl -> add(out, args[3], pl.getName()));
        }
        return out;
    }

    private void add(List<String> out, String typed, String... options) {
        String lower = typed.toLowerCase(Locale.ROOT);
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(o);
            }
        }
    }
}
