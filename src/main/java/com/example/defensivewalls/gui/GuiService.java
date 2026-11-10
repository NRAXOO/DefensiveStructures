package com.example.defensivewalls.gui;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureObserver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

/** Punto de entrada para abrir interfaces. Cierra las de un gabinete si este se destruye. */
public final class GuiService implements StructureObserver {

    private final DefensiveWallsPlugin plugin;

    public GuiService(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openSelection(Player p) {
        new SelectionGui(plugin, p).open();
    }

    public void openCabinet(Player p, Structure cabinet) {
        new CabinetGui(plugin, p, cabinet).open();
    }

    public void openList(Player p, Structure cabinet, int page) {
        new StructureListGui(plugin, p, cabinet, page).open();
    }

    public void openInfo(Player p, Structure s) {
        new InfoGui(plugin, p, s).open();
    }

    @Override
    public void removed(Structure s) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof CabinetGui g && g.cabinetId().equals(s.id())) {
                    p.closeInventory(); // onClose devuelve los items depositados
                }
            }
        });
    }
}
