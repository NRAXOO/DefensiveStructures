package com.example.defensivewalls.listeners;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.tools.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Papel (menu de seleccion) y palo (herramienta con modos).
 * SHIFT + clic con el palo cambia de modo; clic derecho normal ejecuta la accion del modo.
 */
public final class ToolListener implements Listener {

    private final DefensiveWallsPlugin plugin;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public ToolListener(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean debounce(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastClick.get(p.getUniqueId());
        lastClick.put(p.getUniqueId(), now);
        return last != null && now - last < 150;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null) {
            return;
        }
        Player p = e.getPlayer();
        Material m = item.getType();
        Action a = e.getAction();
        boolean right = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;

        if (m == plugin.settings().selectorMaterial()) {
            if (!right || !p.hasPermission(plugin.settings().permUse())) {
                return;
            }
            e.setCancelled(true);
            if (!debounce(p)) {
                plugin.guis().openSelection(p);
            }
            return;
        }
        if (m != plugin.settings().toolMaterial() || !p.hasPermission(plugin.settings().permUse())) {
            return;
        }

        // Debe leerse ANTES de cancelar: indica si un plugin de proteccion ya denego la interaccion.
        boolean denied = a == Action.RIGHT_CLICK_BLOCK && e.useInteractedBlock() == Event.Result.DENY;
        PlayerData d = plugin.players().get(p);

        if (p.isSneaking()) {
            e.setCancelled(true);
            if (debounce(p)) {
                return;
            }
            d.setMode(d.mode().next());
            plugin.players().save();
            plugin.messages().send(p, "mode-changed", "%mode%", plugin.settings().modeName(d.mode()));
            return;
        }
        if (!right) {
            return;
        }
        e.setCancelled(true);
        if (debounce(p)) {
            return;
        }
        plugin.tools().perform(p, d, e.getClickedBlock(), e.getBlockFace(), denied);
    }
}
