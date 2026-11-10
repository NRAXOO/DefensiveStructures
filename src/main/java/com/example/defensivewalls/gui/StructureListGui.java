package com.example.defensivewalls.gui;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.durability.DurabilityState;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.utils.ItemBuilder;
import com.example.defensivewalls.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lista paginada de estructuras conectadas a un gabinete. */
public final class StructureListGui extends GuiHolder {

    private static final int PER_PAGE = 45;

    private final DefensiveWallsPlugin plugin;
    private final Player player;
    private final Structure cabinet;
    private final int page;
    private final Map<Integer, UUID> slotMap = new HashMap<>();

    public StructureListGui(DefensiveWallsPlugin plugin, Player player, Structure cabinet, int page) {
        this.plugin = plugin;
        this.player = player;
        this.cabinet = cabinet;
        List<Structure> list = plugin.structures().linked(cabinet.id());
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));
        this.inventory = Bukkit.createInventory(this, 54, Text.plain("&8Estructuras conectadas (" + (this.page + 1) + "/" + pages + ")"));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = this.page * PER_PAGE + i;
            if (idx >= list.size()) {
                break;
            }
            Structure s = list.get(idx);
            DurabilityState st = plugin.durability().stateOf(s);
            List<String> lore = new ArrayList<>(plugin.report().structureLines(s));
            lore.remove(0);
            lore.add("");
            lore.add("&eClic para mostrar su holograma");
            inventory.setItem(i, ItemBuilder.of(plugin.items().iconMaterial(s.option()))
                    .name(st.color() + plugin.settings().structureName(s.type(), s.tier())).lore(lore).build());
            slotMap.put(i, s.id());
        }
        inventory.setItem(45, ItemBuilder.of(Material.ARROW).name("&ePágina anterior").build());
        inventory.setItem(49, ItemBuilder.of(Material.BARRIER).name("&cVolver al gabinete").build());
        inventory.setItem(53, ItemBuilder.of(Material.ARROW).name("&ePágina siguiente").build());
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() != inventory) {
            return;
        }
        int slot = e.getRawSlot();
        if (slot == 45) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.guis().openList(player, cabinet, page - 1));
        } else if (slot == 53) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.guis().openList(player, cabinet, page + 1));
        } else if (slot == 49) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.guis().openCabinet(player, cabinet));
        } else if (slotMap.containsKey(slot)) {
            Structure s = plugin.structures().get(slotMap.get(slot));
            if (s != null) {
                plugin.holograms().force(player, s);
                Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            }
        }
    }
}
