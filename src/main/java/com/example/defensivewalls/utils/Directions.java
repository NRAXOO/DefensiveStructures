package com.example.defensivewalls.utils;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/** Utilidades de direcciones cardinales. */
public final class Directions {
    private Directions() {
    }

    /** Giro de 90 grados en sentido horario (NORTE -> ESTE -> SUR -> OESTE). */
    public static BlockFace cw(BlockFace f) {
        return switch (f) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> f;
        };
    }

    public static BlockFace ccw(BlockFace f) {
        return cw(cw(cw(f)));
    }

    public static BlockFace cardinal(Player player) {
        BlockFace f = player.getFacing();
        return switch (f) {
            case NORTH, EAST, SOUTH, WEST -> f;
            default -> BlockFace.NORTH;
        };
    }

    public static Axis axisOf(BlockFace f) {
        return f.getModX() != 0 ? Axis.X : Axis.Z;
    }
}
