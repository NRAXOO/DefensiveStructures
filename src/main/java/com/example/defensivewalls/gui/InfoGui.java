package com.example.defensivewalls.gui;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.utils.ItemBuilder;
import com.example.defensivewalls.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Informacion de durabilidad y mantenimiento de una estructura. */
public final class InfoGui extends GuiHolder {

    private final DefensiveWallsPlugin plugin;
    private final Player player;
    private final Structure structure;

    public InfoGui(DefensiveWallsPlugin plugin, Player player, Structure s) {
        this.plugin = plugin;
        this.player = player;
        this.structure = s;
        this.inventory = Bukkit.createInventory(this, 27, Text.plain("&8" + plugin.settings().structureName(s.type(), s.tier())));
        List<String> lines = plugin.report().structureLines(s);
        inventory.setItem(13, ItemBuilder.of(plugin.items().iconMaterial(s.option()))
                .name(lines.get(0)).lore(lines.subList(1, lines.size())).build());
        Structure cab = plugin.structures().get(s.cabinetId());
        if (cab != null) {
            inventory.setItem(22, ItemBuilder.of(Material.CHISELED_BOOKSHELF).name("&6Abrir gabinete asociado")
                    .lore(List.of("&eClic para abrir")).build());
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getRawSlot() == 22 && e.getClickedInventory() == inventory) {
            Structure cab = plugin.structures().get(structure.cabinetId());
            if (cab != null) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.guis().openCabinet(player, cab));
            }
        }
    }
}
