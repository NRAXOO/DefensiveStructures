package com.example.defensivewalls.listeners;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.builder.BuildRequest;
import com.example.defensivewalls.structures.builder.PlacementResult;
import com.example.defensivewalls.tools.BuildTargeting;
import com.example.defensivewalls.utils.Directions;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/** Colocacion de estructuras con objetos (/wall give) + proteccion contra usos indebidos del objeto. */
public final class ItemPlacementListener implements Listener {

    private final DefensiveWallsPlugin plugin;

    public ItemPlacementListener(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    private BuildRequest request(BlockPlaceEvent e, StructureOption option, boolean probe) {
        Player p = e.getPlayer();
        Block placed = e.getBlockPlaced();
        BlockFace face = e.getBlockAgainst().getFace(placed);
        if (face == null) {
            face = BlockFace.UP;
        }
        return new BuildRequest(p, option, BlockKey.of(placed), Directions.cardinal(p),
                BuildTargeting.resolveHalf(plugin.players().get(p).roofHalf(), face),
                false, probe, true, e.getItemInHand().clone(), e.getHand());
    }

    /** Validacion (HIGH). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceValidate(BlockPlaceEvent e) {
        if (plugin.protection().isProbing()) {
            return;
        }
        StructureOption option = plugin.items().parse(e.getItemInHand());
        if (option == null || !e.canBuild()) {
            return;
        }
        PlacementResult result = plugin.validator().validate(request(e, option, true));
        if (!result.ok()) {
            e.setCancelled(true);
            plugin.messages().send(e.getPlayer(), result.error(), result.args());
        }
    }

    /** Construccion (MONITOR): solo si nadie mas cancelo, asi el objeto siempre se consume (sin duplicar). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceBuild(BlockPlaceEvent e) {
        if (plugin.protection().isProbing()) {
            return;
        }
        StructureOption option = plugin.items().parse(e.getItemInHand());
        if (option == null || !e.canBuild()) {
            return;
        }
        PlacementResult result = plugin.builder().build(request(e, option, false));
        if (!result.ok()) {
            e.setCancelled(true);
            plugin.messages().send(e.getPlayer(), result.error(), result.args());
            return;
        }
        plugin.messages().send(e.getPlayer(), "built", "%structure%",
                plugin.settings().structureName(option.type(), option.tier()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        for (ItemStack stack : e.getInventory().getMatrix()) {
            if (stack != null && plugin.items().isStructureItem(stack)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent e) {
        if (plugin.items().isStructureItem(e.getFuel())) {
            e.setCancelled(true);
        }
    }
}
