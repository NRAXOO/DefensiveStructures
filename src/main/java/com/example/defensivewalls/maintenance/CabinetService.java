package com.example.defensivewalls.maintenance;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.utils.ItemMatcher;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Logica de gabinetes: asociacion, deposito y consumo de materiales. */
public final class CabinetService {

    private final DefensiveWallsPlugin plugin;

    public CabinetService(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------- asociacion ----------------

    public Structure findNearest(BlockKey origin) {
        Structure best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Structure s : plugin.structures().nearby(origin.world(), origin.x() + 0.5, origin.y() + 0.5,
                origin.z() + 0.5, plugin.settings().associationRadius())) {
            if (s.type() != StructureType.CABINET || !hasCapacity(s)) {
                continue;
            }
            double dx = s.origin().x() - origin.x();
            double dy = s.origin().y() - origin.y();
            double dz = s.origin().z() - origin.z();
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestDistance) {
                bestDistance = d;
                best = s;
            }
        }
        return best;
    }

    public boolean hasCapacity(Structure cabinet) {
        return plugin.structures().linkedCount(cabinet.id()) < plugin.settings().maxStructuresPerCabinet();
    }

    /**
     * Vincula al gabinete las estructuras cercanas sin mantenimiento.
     *
     * @param force si es true tambien "roba" estructuras asignadas a otro gabinete (reasignacion)
     * @return cuantas estructuras se vincularon
     */
    public int adoptNearby(Structure cabinet, boolean force) {
        int count = 0;
        for (Structure s : plugin.structures().nearby(cabinet.origin().world(), cabinet.origin().x() + 0.5,
                cabinet.origin().y() + 0.5, cabinet.origin().z() + 0.5, plugin.settings().associationRadius())) {
            if (s == cabinet || !s.type().needsMaintenance() || cabinet.id().equals(s.cabinetId())) {
                continue;
            }
            if (s.cabinetId() != null && !force) {
                continue;
            }
            if (!hasCapacity(cabinet)) {
                break;
            }
            plugin.structures().link(s, cabinet);
            count++;
        }
        return count;
    }

    // ---------------- almacenamiento ----------------

    public boolean accepts(Material material) {
        return ItemMatcher.matchesAny(plugin.settings().acceptedTokens(), material);
    }

    /**
     * Mueve los items validos de un inventario (slots 0..limit-1) al almacen del gabinete.
     * Devuelve los items que no se aceptan (el llamador debe devolverlos al jugador).
     */
    public List<ItemStack> deposit(Structure cabinet, Inventory inventory, int limit) {
        List<ItemStack> rejected = new ArrayList<>();
        int deposited = 0;
        for (int slot = 0; slot < limit; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            inventory.setItem(slot, null);
            if (accepts(item.getType()) && !plugin.items().isStructureItem(item)) {
                cabinet.addStored(item.getType(), item.getAmount());
                deposited += item.getAmount();
            } else {
                rejected.add(item);
            }
        }
        if (deposited > 0) {
            plugin.structures().markDirty();
            plugin.structures().changed(cabinet);
        }
        return rejected;
    }

    /** Consume 1 unidad del token del almacen. false si no hay. */
    public boolean take(Structure cabinet, String token) {
        Iterator<Map.Entry<Material, Integer>> it = cabinet.storage().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Material, Integer> e = it.next();
            if (e.getValue() > 0 && ItemMatcher.matches(token, e.getKey())) {
                int left = e.getValue() - 1;
                if (left <= 0) {
                    it.remove();
                } else {
                    e.setValue(left);
                }
                return true;
            }
        }
        return false;
    }

    public int stockUnits(Structure cabinet, String token) {
        int total = 0;
        for (Map.Entry<Material, Integer> e : cabinet.storage().entrySet()) {
            if (ItemMatcher.matches(token, e.getKey())) {
                total += e.getValue();
            }
        }
        return total;
    }

    /** Consumo por minuto agrupado por token de material para las estructuras vinculadas. */
    public Map<String, Double> consumptionPerMinute(Structure cabinet) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Structure s : plugin.structures().linked(cabinet.id())) {
            if (!s.type().needsMaintenance()) {
                continue;
            }
            map.merge(plugin.settings().tierToken(s.tier()), plugin.settings().maintenancePerMinute(s.tier()), Double::sum);
        }
        return map;
    }

    /** Devuelve items al jugador; lo que no cabe se suelta a sus pies. */
    public void giveBack(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            player.getInventory().addItem(item).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }
}
