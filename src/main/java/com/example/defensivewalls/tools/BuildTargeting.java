package com.example.defensivewalls.tools;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.RoofHalf;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.builder.BuildRequest;
import com.example.defensivewalls.utils.Directions;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/** Convierte "donde apunta el jugador" en una peticion de construccion (compartido por clic y holograma). */
public final class BuildTargeting {

    private final DefensiveWallsPlugin plugin;

    public BuildTargeting(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public static RoofHalf resolveHalf(RoofHalf preference, BlockFace face) {
        if (preference == RoofHalf.AUTO) {
            return face == BlockFace.DOWN ? RoofHalf.TOP : RoofHalf.BOTTOM;
        }
        return preference;
    }

    public BuildRequest fromClick(Player p, PlayerData d, Block clicked, BlockFace face, boolean charge, boolean probe) {
        StructureOption option = d.selection();
        if (option == null || clicked == null || face == null) {
            return null;
        }
        Block origin = clicked.getRelative(face);
        return new BuildRequest(p, option, BlockKey.of(origin), Directions.cardinal(p),
                resolveHalf(d.roofHalf(), face), charge, probe, false,
                p.getInventory().getItemInMainHand().clone(), null);
    }

    public BuildRequest fromRay(Player p, PlayerData d, boolean charge, boolean probe) {
        RayTraceResult hit = p.rayTraceBlocks(plugin.settings().toolRange());
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            return null;
        }
        return fromClick(p, d, hit.getHitBlock(), hit.getHitBlockFace(), charge, probe);
    }
}
