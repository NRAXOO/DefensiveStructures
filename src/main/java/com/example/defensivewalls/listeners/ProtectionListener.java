package com.example.defensivewalls.listeners;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proteccion de bloques de estructuras, destruccion con papel y dano con pico.
 * Los bloques nunca se rompen de forma individual.
 */
public final class ProtectionListener implements Listener {

    private static final class Session {
        final UUID structure;
        long expires;

        Session(UUID structure, long expires) {
            this.structure = structure;
            this.expires = expires;
        }
    }

    private final DefensiveWallsPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Integer> lastHitTick = new HashMap<>();
    private final Map<UUID, Long> messageCooldown = new HashMap<>();

    public ProtectionListener(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isPickaxe(ItemStack item) {
        return item != null && Tag.ITEMS_PICKAXES.isTagged(item.getType()) || item != null && Tag.ITEMS_AXES.isTagged(item.getType());
    }

    private boolean isPaper(ItemStack item) {
        return item != null && item.getType() == plugin.settings().selectorMaterial();
    }

    // ---------------- dano y destruccion ----------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(BlockDamageEvent e) {
        Structure s = plugin.structures().at(e.getBlock());
        if (s == null) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (isPaper(hand) && plugin.settings().paperBreaksStructures()) {
            e.setInstaBreak(true); // se gestiona en onBreak: elimina toda la estructura
            return;
        }
        e.setCancelled(true); // el bloque nunca recibe dano vanilla
        if (isPickaxe(hand)) {
            sessions.put(p.getUniqueId(), new Session(s.id(), System.currentTimeMillis() + 2000));
            hit(p, s, hand);
        } else {
            throttled(p, "need-break-item");
        }
    }

    /** Golpes repetidos mientras se mantiene pulsado el clic (el servidor solo avisa del primero). */
    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent e) {
        Player p = e.getPlayer();
        Session session = sessions.get(p.getUniqueId());
        if (session == null) {
            return;
        }
        long now = System.currentTimeMillis();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (now > session.expires || !isPickaxe(hand)) {
            sessions.remove(p.getUniqueId());
            return;
        }
        Block target = p.getTargetBlockExact(6);
        Structure s = plugin.structures().at(target);
        if (s == null || !s.id().equals(session.structure)) {
            return;
        }
        session.expires = now + 2000;
        hit(p, s, hand);
    }

    private void hit(Player p, Structure s, ItemStack tool) {
        if (!p.hasPermission(plugin.settings().permUse())) {
            return;
        }
        if (p.getGameMode() == GameMode.CREATIVE && !plugin.settings().pickaxeAllowCreative()) {
            throttled(p, "creative-no-damage");
            return;
        }
        if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.ADVENTURE) {
            return;
        }
        int tick = org.bukkit.Bukkit.getCurrentTick();
        Integer last = lastHitTick.get(p.getUniqueId());
        if (last != null && tick - last < plugin.settings().pickaxeCooldownTicks()) {
            return;
        }
        lastHitTick.put(p.getUniqueId(), tick);
        p.setCooldown(tool.getType(),plugin.settings().pickaxeCooldownTicks());

        int damage = plugin.settings().pickaxeDamage(tool.getType());
        if (plugin.durability().damage(s, damage, p)) {
            return; // destruida
        }
        plugin.messages().actionBar(p, "durability-bar",
                "%structure%", plugin.settings().structureName(s.type(), s.tier()),
                "%durability%", String.valueOf(s.durability()),
                "%max%", String.valueOf(s.maxDurability()),
                "%state%", plugin.durability().stateOf(s).label());
        plugin.holograms().force(p, s);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Structure s = plugin.structures().at(e.getBlock());
        if (s == null) {
            return;
        }
        e.setCancelled(true);
        e.setDropItems(false);
        e.setExpToDrop(0);

        Player p = e.getPlayer();
        if (!isPaper(p.getInventory().getItemInMainHand()) || !plugin.settings().paperBreaksStructures()
                || !p.hasPermission(plugin.settings().permUse())) {
            throttled(p, "need-break-item");
            return;
        }
        String name = plugin.settings().structureName(s.type(), s.tier());
        boolean drop = plugin.settings().dropItemOnRemove() && p.getGameMode() != GameMode.CREATIVE;
        plugin.structures().destroy(s, drop, true);
        plugin.messages().send(p, "removed", "%structure%", name);
    }

    // ---------------- mecanismos y entorno ----------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(plugin.structures()::isStructureBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(plugin.structures()::isStructureBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (containsStructure(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (containsStructure(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (plugin.structures().isStructureBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent e) {
        if (plugin.structures().isStructureBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    private boolean containsStructure(List<Block> blocks) {
        for (Block b : blocks) {
            if (plugin.structures().isStructureBlock(b)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        sessions.remove(id);
        lastHitTick.remove(id);
        messageCooldown.remove(id);
    }

    private void throttled(Player p, String key) {
        long now = System.currentTimeMillis();
        Long last = messageCooldown.get(p.getUniqueId());
        if (last != null && now - last < 1000) {
            return;
        }
        messageCooldown.put(p.getUniqueId(), now);
        plugin.messages().send(p, key);
    }
}
