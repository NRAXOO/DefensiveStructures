package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.configuration.Settings;
import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;

import java.util.ArrayList;
import java.util.List;

/**
 * Puerta blindada 3x3. Distribucion (fila superior primero):
 * 0 0 0
 * 0 1 0
 * 0 1 0
 * 0 = bloque solido de soporte, 1 = puerta de hierro (2 bloques de alto). Origen = mitad inferior de la puerta.
 */
public final class IronDoorShape implements StructureShape {

    private final Settings settings;

    public IronDoorShape(Settings settings) {
        this.settings = settings;
    }

    @Override
    public StructureType type() {
        return StructureType.IRON_DOOR;
    }

    @Override
    public boolean supports(Tier tier) {
        return tier == Tier.IRON;
    }

    @Override
    public boolean requiresSolidGround() {
        return true;
    }

    @Override
    public List<PlannedBlock> plan(PlanContext c) {
        List<PlannedBlock> out = new ArrayList<>(9);
        Material support = settings.doorSupport();
        for (int row = 0; row < 3; row++) {
            for (int col = -1; col <= 1; col++) {
                boolean isDoorCell = col == 0 && row < 2;
                if (!isDoorCell) {
                    out.add(new PlannedBlock(c.at(col, row, 0), Bukkit.createBlockData(support), true));
                }
            }
        }
        // La puerta se coloca al final y sin fisica (una mitad sola se destruiria).
        out.add(new PlannedBlock(c.at(0, 0, 0), door(c, Bisected.Half.BOTTOM), false));
        out.add(new PlannedBlock(c.at(0, 1, 0), door(c, Bisected.Half.TOP), false));
        return out;
    }

    private Door door(PlanContext c, Bisected.Half half) {
        Door door = (Door) Bukkit.createBlockData(Material.IRON_DOOR);
        door.setFacing(c.back());
        door.setHalf(half);
        door.setHinge(Door.Hinge.LEFT);
        door.setOpen(false);
        door.setPowered(false);
        return door;
    }
}
