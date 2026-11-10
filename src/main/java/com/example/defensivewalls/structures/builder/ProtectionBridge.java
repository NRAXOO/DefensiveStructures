package com.example.defensivewalls.structures.builder;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Comprueba protecciones de otros plugins (WorldGuard, GriefPrevention...) lanzando un
 * BlockPlaceEvent de prueba por bloque. Este plugin ignora esos eventos (isProbing()).
 */
public final class ProtectionBridge {

    private boolean probing;

    public boolean isProbing() {
        return probing;
    }

    public boolean canBuild(Player player, Block target, Block against, ItemStack item, EquipmentSlot hand) {
        BlockPlaceEvent probe = new BlockPlaceEvent(target, target.getState(), against, item, player, true,
                hand == null ? EquipmentSlot.HAND : hand);
        probing = true;
        try {
            Bukkit.getPluginManager().callEvent(probe);
        } finally {
            probing = false;
        }
        return !probe.isCancelled() && probe.canBuild();
    }
}
