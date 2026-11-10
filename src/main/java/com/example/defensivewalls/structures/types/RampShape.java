package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;

import java.util.ArrayList;
import java.util.List;

/**
 * Rampa de escaleras 3 (ancho) x 3 (largo) x 3 (alto). Cada escalon i avanza i bloques
 * y sube i bloques. Bajo los escalones queda aire. Origen = escalon inferior central.
 */
public final class RampShape implements StructureShape {

    @Override
    public StructureType type() {
        return StructureType.RAMP;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.WOOD || tier == Tier.STONE;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        List<PlannedBlock> out = new ArrayList<>(9);
        Material m = c.tier() == Tier.WOOD ? Material.DARK_OAK_STAIRS : Material.STONE_BRICK_STAIRS;
        for (int step = 0; step < 3; step++) {
            for (int col = -1; col <= 1; col++) {
                BlockData data = Bukkit.createBlockData(m);
                Stairs stairs = (Stairs) data;
                stairs.setFacing(c.facing()); // asciende en la direccion en la que mira el jugador
                stairs.setHalf(Bisected.Half.BOTTOM);
                stairs.setShape(Stairs.Shape.STRAIGHT);
                out.add(new PlannedBlock(c.at(col, step, step), stairs, true));
            }
        }
        return out;
    }
}
