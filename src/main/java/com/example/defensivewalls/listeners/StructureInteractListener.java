package com.example.defensivewalls.listeners;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Interaccion normal con estructuras: gabinete (interfaz), puerta blindada, cofres, hachas bloqueadas. */
public final class StructureInteractListener implements Listener {

    private final DefensiveWallsPlugin plugin;

    public StructureInteractListener(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) {
            return;
        }
        Structure s = plugin.structures().at(e.getClickedBlock());
        if (s == null) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        Material m = hand.getType();
        if (m == plugin.settings().toolMaterial() || m == plugin.settings().selectorMaterial()) {
            return; // lo gestiona ToolListener
        }
        if (plugin.settings().selfRepairBlocks()) {
            plugin.structures().repairBlocks(s);
        }
        // Las hachas no pueden pelar troncos (modificaria una parte individual).
        if (Tag.ITEMS_AXES.isTagged(m)) {
            e.setCancelled(true);
            return;
        }
        // SHIFT con un bloque en la mano: permitir construir contra la estructura.
        if (p.isSneaking() && m.isBlock() && !m.isAir()) {
            return;
        }
        plugin.holograms().force(p, s);

        if (s.type() == StructureType.CABINET) {
            e.setCancelled(true);
            if (p.hasPermission(plugin.settings().permUse())) {
                plugin.guis().openCabinet(p, s);
            }
        } else if (s.type() == StructureType.IRON_DOOR && e.getClickedBlock().getBlockData() instanceof Door) {
            e.setCancelled(true);
            if (p.hasPermission(plugin.settings().permUse())) {
                toggleDoor(s);
            }
        }
    }

    /** Abre/cierra las dos mitades a la vez (la puerta de hierro no se abre a mano en vanilla). */
    private void toggleDoor(Structure s) {
        List<Block> doorBlocks = new ArrayList<>();
        for (BlockKey k : s.blocks()) {
            Block b = k.toBlock();
            if (b != null && b.getBlockData() instanceof Door) {
                doorBlocks.add(b);
            }
        }
        if (doorBlocks.isEmpty()) {
            return;
        }
        boolean open = !((Door) doorBlocks.get(0).getBlockData()).isOpen();
        for (Block b : doorBlocks) {
            Door door = (Door) b.getBlockData();
            door.setOpen(open);
            b.setBlockData(door, false);
        }
        Block first = doorBlocks.get(0);
        first.getWorld().playSound(first.getLocation(), open ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
    }
}
