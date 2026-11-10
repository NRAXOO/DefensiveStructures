package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

import java.util.ArrayList;
import java.util.List;

/** Gabinete de mantenimiento 2x1: dos estanterias cinceladas apiladas. */
public final class CabinetShape implements StructureShape {

    @Override
    public StructureType type() {
        return StructureType.CABINET;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.WOOD || tier == Tier.STONE;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        List<PlannedBlock> out = new ArrayList<>(2);
        for (int h = 0; h < 2; h++) {
            BlockData data = Bukkit.createBlockData(Material.CHISELED_BOOKSHELF);
            if (data instanceof Directional directional) {
                directional.setFacing(c.back());
            }
            out.add(new PlannedBlock(c.at(0, h, 0), data, false));
        }
        return out;
    }
}
