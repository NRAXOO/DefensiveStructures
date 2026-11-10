package com.example.defensivewalls.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Base de todas las interfaces del plugin. */
public abstract class GuiHolder implements InventoryHolder {
    protected Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void onClick(InventoryClickEvent event);

    public void onDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    public void onClose(InventoryCloseEvent event) {
    }
}
