package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.utils.Directions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;

import java.util.ArrayList;
import java.util.List;

/**
 * Cofre doble real: dos cofres CHEST con tipos LEFT/RIGHT enlazados.
 * En vanilla el cofre LEFT se conecta con el vecino situado en facing.rotarHorario().
 */
public final class DoubleChestShape implements StructureShape {

    @Override
    public StructureType type() {
        return StructureType.DOUBLE_CHEST;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.WOOD;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        BlockFace front = c.back(); // el frente del cofre mira al jugador
        Chest left = (Chest) Bukkit.createBlockData(Material.CHEST);
        left.setFacing(front);
        left.setType(Chest.Type.LEFT);

        Chest right = (Chest) Bukkit.createBlockData(Material.CHEST);
        right.setFacing(front);
        right.setType(Chest.Type.RIGHT);

        List<PlannedBlock> out = new ArrayList<>(2);
        out.add(new PlannedBlock(c.origin(), left, false));
        out.add(new PlannedBlock(c.origin().shift(Directions.cw(front), 1), right, false));
        return out;
    }
}
