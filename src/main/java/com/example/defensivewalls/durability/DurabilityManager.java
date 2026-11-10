package com.example.defensivewalls.durability;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Perdida, reparacion y destruccion por durabilidad. */
public final class DurabilityManager {

    private final DefensiveWallsPlugin plugin;

    public DurabilityManager(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public DurabilityState stateOf(Structure s) {
        return DurabilityState.of(s.durability(), s.maxDurability());
    }

    public int percent(Structure s) {
        return (int) Math.round(100.0 * s.durability() / Math.max(1, s.maxDurability()));
    }

    /**
     * Resta durabilidad. Si llega a cero destruye la estructura completa.
     *
     * @return true si la estructura fue destruida
     */
    public boolean damage(Structure s, int amount, Player cause) {
        if (amount <= 0) {
            return false;
        }
        s.setDurability(s.durability() - amount);
        if (s.durability() <= 0) {
            String name = plugin.settings().structureName(s.type(), s.tier());
            Location center = plugin.structures().center(s);
            plugin.structures().destroy(s, false, true);
            notifyDestroyed(name, center, cause);
            return true;
        }
        plugin.structures().changed(s);
        plugin.structures().markDirty();
        return false;
    }

    /** Recupera hasta {@code amount} puntos; devuelve los recuperados. */
    public int repair(Structure s, int amount) {
        int before = s.durability();
        s.setDurability(before + Math.max(0, amount));
        int restored = s.durability() - before;
        if (restored > 0) {
            plugin.structures().changed(s);
            plugin.structures().markDirty();
        }
        return restored;
    }

    private void notifyDestroyed(String name, Location center, Player cause) {
        if (cause != null) {
            plugin.messages().send(cause, "structure-destroyed", "%structure%", name);
        }
        if (center == null) {
            return;
        }
        for (Player p : center.getWorld().getPlayers()) {
            if (p != cause && p.getLocation().distanceSquared(center) <= 32 * 32) {
                plugin.messages().send(p, "structure-destroyed", "%structure%", name);
            }
        }
    }
}
