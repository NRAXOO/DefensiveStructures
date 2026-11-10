package com.example.defensivewalls.gui;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.tools.PlayerData;
import com.example.defensivewalls.utils.Cost;
import com.example.defensivewalls.utils.ItemBuilder;
import com.example.defensivewalls.utils.ItemMatcher;
import com.example.defensivewalls.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Menu del papel: seleccion de la estructura activa. */
public final class SelectionGui extends GuiHolder {

    private static final int SLOT_ROOF_HALF = 3;
    private static final int SLOT_MATERIAL = 5;

    private final DefensiveWallsPlugin plugin;
    private final Player player;
    private final Map<Integer, StructureOption> slots = new HashMap<>();

    public SelectionGui(DefensiveWallsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, Text.plain("&8Selecciona una estructura"));
        render();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private StructureOption option(StructureType type, Tier fixed) {
        PlayerData d = plugin.players().get(player);
        Tier tier = fixed != null ? fixed : (type.tierless() ? type.defaultTier() : d.materialPreference());
        return new StructureOption(type, tier);
    }

    private void render() {
        inventory.clear();
        slots.clear();
        PlayerData d = plugin.players().get(player);
        StructureOption[] options = {
                option(StructureType.ROOF, null),
                option(StructureType.VERTICAL_STAIRS, null),
                option(StructureType.FLOOR, null),
                option(StructureType.RAMP, null),
                option(StructureType.DOUBLE_CHEST, null),
                option(StructureType.CABINET, null),
                option(StructureType.IRON_DOOR, null),
                option(StructureType.WALL, Tier.WOOD),
                option(StructureType.WALL, Tier.STONE)
        };
        for (int i = 0; i < options.length; i++) {
            StructureOption o = options[i];
            Cost cost = plugin.settings().buildCost(o.type(), o.tier());
            List<String> lore = new ArrayList<>();
            lore.add("&7Coste: &f" + cost.amount() + " " + ItemMatcher.describe(cost.token()));
            lore.add("&7Durabilidad: &f" + plugin.settings().maxDurability(o.tier()));
            lore.add(o.type().needsMaintenance() ? "&7Requiere gabinete de mantenimiento" : "&7Sin mantenimiento");
            lore.add("");
            lore.add("&eClic para seleccionar");
            boolean selected = o.equals(d.selection());
            inventory.setItem(9 + i, ItemBuilder.of(plugin.items().iconMaterial(o))
                    .name((selected ? "&a✔ " : "&6") + plugin.settings().structureName(o.type(), o.tier()))
                    .lore(lore).glow(selected).build());
            slots.put(9 + i, o);
        }
//        inventory.setItem(SLOT_ROOF_HALF, ItemBuilder.of(Material.STONE_SLAB)
//                .name("&6Orientación del techo: &f" + d.roofHalf().label())
//                .lore(List.of("&7Automática: depende de la cara apuntada", "&eClic para cambiar")).build());
        inventory.setItem(SLOT_MATERIAL, ItemBuilder.of(d.materialPreference() == Tier.WOOD ? Material.DARK_OAK_PLANKS : Material.STONE_BRICKS)
                .name("&6Material: &f" + plugin.settings().tierName(d.materialPreference()))
                .lore(List.of("&7Para techo, suelo, escaleras y gabinete", "&eClic para cambiar")).build());
        StructureOption current = d.selection();
        inventory.setItem(22, ItemBuilder.of(Material.PAPER)
                .name("&6Activa: &f" + (current == null ? "ninguna" : plugin.settings().structureName(current.type(), current.tier())))
                .lore(List.of("&7Equipa el palo y apunta donde construir.", "&7SHIFT + clic con el palo cambia de modo.")).build());
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() != inventory) {
            return;
        }
        PlayerData d = plugin.players().get(player);
        int slot = e.getRawSlot();
        if (slot == SLOT_ROOF_HALF) {
            d.setRoofHalf(d.roofHalf().next());
            plugin.players().save();
            render();
        } else if (slot == SLOT_MATERIAL) {
            Tier next = d.materialPreference() == Tier.WOOD ? Tier.STONE : Tier.WOOD;
            d.setMaterialPreference(next);
            StructureOption current = d.selection();
            if (current != null && !current.type().tierless() && current.type() != StructureType.WALL) {
                d.select(new StructureOption(current.type(), next));
            }
            plugin.players().save();
            render();
        } else if (slots.containsKey(slot)) {
            StructureOption o = slots.get(slot);
            d.select(o);
            plugin.players().save();
            plugin.messages().send(player, "selected", "%structure%", plugin.settings().structureName(o.type(), o.tier()));
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        }
    }
}
