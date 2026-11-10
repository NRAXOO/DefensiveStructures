package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/** Suelo 3x3 de bloques solidos. Origen = centro. */
public final class FloorShape implements StructureShape {

    @Override
    public StructureType type() {
        return StructureType.FLOOR;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.WOOD || tier == Tier.STONE;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        List<PlannedBlock> out = new ArrayList<>(9);
        for (int r = -1; r <= 1; r++) {
            for (int f = -1; f <= 1; f++) {
                Material m;
                if (c.tier() == Tier.WOOD) {
                    m = (r == 0 && f == 0) ? Material.SPRUCE_PLANKS : Material.DARK_OAK_PLANKS;
                } else {
                    m = (r == 0 && f == 0) ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS;
                }
                out.add(new PlannedBlock(c.at(r, 0, f), Bukkit.createBlockData(m), true));
            }
        }
        return out;
    }
}
