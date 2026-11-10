package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Ladder;

import java.util.ArrayList;
import java.util.List;

public final class VerticalStairsShape implements StructureShape {

    @Override
    public StructureType type() {
        return StructureType.VERTICAL_STAIRS;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.WOOD || tier == Tier.STONE;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        List<PlannedBlock> out = new ArrayList<>(3);

        for (int h = 0; h < 3; h++) {
            BlockData data = Bukkit.createBlockData(Material.LADDER);
            Ladder ladder = (Ladder) data;

            ladder.setFacing(c.back());

            out.add(new PlannedBlock(c.at(0, h, 0), ladder, false));
        }

        return out;
    }
}