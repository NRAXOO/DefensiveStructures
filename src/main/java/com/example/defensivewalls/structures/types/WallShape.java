package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.utils.Directions;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;

import java.util.ArrayList;
import java.util.List;

/**
 * Muro 3x3 (origen = bloque inferior central). Vista frontal (fila 2 = arriba):
 * MADERA: [tronco][valla][tronco] / [tablas][tronco desc.][tablas] / [tronco][tablas][tronco]
 * PIEDRA: [ladrillo][muro][ladrillo] / [musgoso][cincelado][musgoso] / [cantería][ladrillo][cantería]
 */
public final class WallShape implements StructureShape {

    @Override
    public StructureType type() {
        return StructureType.WALL;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.WOOD || tier == Tier.STONE;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        List<PlannedBlock> out = new ArrayList<>(9);
        Axis axis = Directions.axisOf(c.right());
        for (int row = 0; row < 3; row++) {
            for (int col = -1; col <= 1; col++) {
                boolean side = col != 0;
                BlockData data = c.tier() == Tier.WOOD ? wood(side, row, axis) : stone(side, row);
                out.add(new PlannedBlock(c.at(col, row, 0), data, true));
            }
        }
        return out;
    }

    private static BlockData wood(boolean side, int row, Axis wallAxis) {
        return switch (row) {
            case 0 -> side ? oriented(Material.DARK_OAK_LOG, Axis.Y) : Bukkit.createBlockData(Material.DARK_OAK_PLANKS);
            case 1 -> side ? Bukkit.createBlockData(Material.DARK_OAK_PLANKS) : oriented(Material.STRIPPED_DARK_OAK_LOG, wallAxis);
            default -> side ? oriented(Material.DARK_OAK_LOG, Axis.Y) : Bukkit.createBlockData(Material.DARK_OAK_FENCE);
        };
    }

    private static BlockData stone(boolean side, int row) {
        Material m = switch (row) {
            case 0 -> side ? Material.MOSSY_COBBLESTONE : Material.STONE_BRICKS;
            case 1 -> side ? Material.MOSSY_STONE_BRICKS : Material.CHISELED_STONE_BRICKS;
            default -> side ? Material.STONE_BRICKS : Material.STONE_BRICK_WALL;
        };
        return Bukkit.createBlockData(m);
    }

    static BlockData oriented(Material material, Axis axis) {
        BlockData data = Bukkit.createBlockData(material);
        if (data instanceof Orientable orientable) {
            orientable.setAxis(axis);
        }
        return data;
    }
}
