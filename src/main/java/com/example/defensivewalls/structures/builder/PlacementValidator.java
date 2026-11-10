package com.example.defensivewalls.structures.builder;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.types.StructureShape;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;

/** Comprueba espacio, colisiones, entidades, soporte, gabinete y protecciones antes de construir. */
public final class PlacementValidator {

    private final DefensiveWallsPlugin plugin;

    public PlacementValidator(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public PlacementResult validate(BuildRequest r) {
        StructureOption option = r.option();
        StructureShape shape = plugin.shapes().get(option.type());
        if (shape == null || !shape.supports(option.tier())) {
            return PlacementResult.fail("invalid-option");
        }
        World world = Bukkit.getWorld(r.origin().world());
        if (world == null) {
            return PlacementResult.fail("no-space");
        }
        List<PlannedBlock> plan = shape.plan(new PlanContext(r.origin(), r.facing(), option.tier(), r.half()));

        for (PlannedBlock pb : plan) {
            var k = pb.key();
            if (k.y() < world.getMinHeight() || k.y() >= world.getMaxHeight()) {
                return PlacementResult.fail("no-room-height");
            }
            if (!world.isChunkLoaded(k.x() >> 4, k.z() >> 4)) {
                return PlacementResult.fail("no-space");
            }
            Block block = world.getBlockAt(k.x(), k.y(), k.z());
            boolean isOrigin = r.originPlaced() && k.equals(r.origin());
            if (!isOrigin) {
                if (plugin.structures().isStructureBlock(block) || !block.isReplaceable()) {
                    return PlacementResult.fail("no-space");
                }
                if (hasEntityInside(block)) {
                    return PlacementResult.fail("entity-in-way");
                }
            }
        }

        if (shape.requiresSolidGround()) {
            Block below = r.origin().shift(BlockFace.DOWN, 1).toBlock();
            if (below == null || !below.isSolid()) {
                return PlacementResult.fail("need-ground");
            }
        }

        if (option.type().needsMaintenance() && plugin.settings().requireCabinet()
                && plugin.cabinets().findNearest(r.origin()) == null) {
            return PlacementResult.fail("need-cabinet");
        }

        if (r.probe()) {
            Block against = r.origin().toBlock();
            for (PlannedBlock pb : plan) {
                if (r.originPlaced() && pb.key().equals(r.origin())) {
                    continue;
                }
                Block target = pb.key().toBlock();
                if (!plugin.protection().canBuild(r.player(), target, against, r.probeItem(), r.hand())) {
                    return PlacementResult.fail("protected-area");
                }
            }
        }
        return PlacementResult.ok(plan, null);
    }

    private boolean hasEntityInside(Block block) {
        BoundingBox box = BoundingBox.of(block);
        return !block.getWorld().getNearbyEntities(box,
                e -> e instanceof LivingEntity le
                        && !(le instanceof Player p && p.getGameMode() == GameMode.SPECTATOR)).isEmpty();
    }
}
