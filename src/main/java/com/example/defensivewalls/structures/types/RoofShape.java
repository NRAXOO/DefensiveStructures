package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.RoofHalf;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;

import java.util.ArrayList;
import java.util.List;

/** Techo 3x3 de losas. Origen = centro. La orientacion (losa inferior/superior) viene en el contexto. */
public final class RoofShape implements StructureShape {

    @Override
    public StructureType type() {
        return StructureType.ROOF;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.WOOD || tier == Tier.STONE;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        List<PlannedBlock> out = new ArrayList<>(9);
        Slab.Type slabType = Slab.Type.TOP;
        for (int r = -2; r <= 0; r++) {
            for (int f = -2; f <= 0; f++) {
                Material m;
                if (c.tier() == Tier.WOOD) {
                    m = Material.DARK_OAK_SLAB;
                }
                else {
                    m = (r == 0 && f == 0) ? Material.MOSSY_STONE_BRICK_SLAB : Material.STONE_BRICK_SLAB;
                }
                BlockData data = Bukkit.createBlockData(m);
                ((Slab) data).setType(slabType);
                out.add(new PlannedBlock(c.at(r, 0, f), data, true));
            }
        }
        return out;
    }
}
