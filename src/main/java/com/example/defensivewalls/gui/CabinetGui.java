package com.example.defensivewalls.gui;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.utils.ItemBuilder;
import com.example.defensivewalls.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gabinete de mantenimiento. Slots 0-26: zona de deposito (se absorbe al cerrar o con el boton).
 * Slots 27-35: materiales almacenados. Slots 36+: botones informativos.
 */
public final class CabinetGui extends GuiHolder {

    private static final int DEPOSIT_SLOTS = 27;

    private final DefensiveWallsPlugin plugin;
    private final Player player;
    private final UUID cabinetId;

    public CabinetGui(DefensiveWallsPlugin plugin, Player player, Structure cabinet) {
        this.plugin = plugin;
        this.player = player;
        this.cabinetId = cabinet.id();
        this.inventory = Bukkit.createInventory(this, 54, Text.plain("&8Gabinete de mantenimiento"));
        render(cabinet);
    }

    public UUID cabinetId() {
        return cabinetId;
    }

    public void open() {
        player.openInventory(inventory);
    }

    private ItemStack filler() {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name("&r").build();
    }

    private void render(Structure cab) {
        for (int i = DEPOSIT_SLOTS; i < 54; i++) {
            inventory.setItem(i, filler());
        }
        int slot = 27;
        for (Map.Entry<Material, Integer> e : cab.storage().entrySet()) {
            if (slot > 35) {
                break;
            }
            inventory.setItem(slot++, ItemBuilder.of(e.getKey()).amount(e.getValue())
                    .name("&6" + e.getKey().name().toLowerCase().replace('_', ' '))
                    .lore(List.of("&7Almacenado: &f" + e.getValue())).build());
        }
        List<String> report = plugin.report().cabinetLines(cab);
        inventory.setItem(36, ItemBuilder.of(Material.BOOK).name("&6Resumen")
                .lore(report.subList(1, report.size())).build());
        inventory.setItem(38, ItemBuilder.of(Material.CHEST).name("&6Estructuras conectadas")
                .lore(List.of("&7Mantiene: &f" + plugin.structures().linkedCount(cab.id()),
                        "&eClic para ver la lista")).build());
        inventory.setItem(40, ItemBuilder.of(Material.CLOCK).name("&6Consumo por minuto")
                .lore(consumptionLore(cab)).build());
        inventory.setItem(42, ItemBuilder.of(Material.LEAD).name("&6Vincular estructuras cercanas")
                .lore(List.of("&7Asigna a este gabinete las estructuras", "&7sin mantenimiento dentro del radio.",
                        "&eClic para vincular")).build());
        inventory.setItem(44, ItemBuilder.of(Material.HOPPER).name("&aDepositar ahora")
                .lore(List.of("&7Guarda los materiales de la zona", "&7de deposito (superior).")).build());
    }

    private List<String> consumptionLore(Structure cab) {
        List<String> lines = new ArrayList<>(plugin.report().cabinetLines(cab));
        return lines.size() > 3 ? lines.subList(3, lines.size()) : List.of("&7Sin consumo");
    }

    private void refresh() {
        Structure cab = plugin.structures().get(cabinetId);
        if (cab != null) {
            render(cab);
        }
    }

    private void absorb() {
        Structure cab = plugin.structures().get(cabinetId);
        List<ItemStack> back = new ArrayList<>();
        if (cab == null) {
            for (int i = 0; i < DEPOSIT_SLOTS; i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    back.add(item);
                    inventory.setItem(i, null);
                }
            }
        } else {
            back = plugin.cabinets().deposit(cab, inventory, DEPOSIT_SLOTS);
        }
        plugin.cabinets().giveBack(player, back);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54 || slot < DEPOSIT_SLOTS) {
            return; // zona de deposito o inventario del jugador: comportamiento normal
        }
        e.setCancelled(true);
        Structure cab = plugin.structures().get(cabinetId);
        if (cab == null) {
            return;
        }
        switch (slot) {
            case 38 -> Bukkit.getScheduler().runTask(plugin, () -> plugin.guis().openList(player, cab, 0));
            case 42 -> {
                int n = plugin.cabinets().adoptNearby(cab, false);
                plugin.messages().send(player, n > 0 ? "cabinet-linked" : "cabinet-none-nearby", "%count%", String.valueOf(n));
                refresh();
            }
            case 44 -> {
                absorb();
                refresh();
            }
            default -> { }
        }
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        for (int raw : e.getRawSlots()) {
            if (raw >= DEPOSIT_SLOTS && raw < 54) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        absorb();
    }
}
