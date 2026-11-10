package com.example.defensivewalls.structures;

import com.example.defensivewalls.utils.Directions;
import org.bukkit.block.BlockFace;

/**
 * Datos necesarios para planificar una estructura.
 * {@code facing} es la direccion cardinal en la que mira el jugador.
 */
public record PlanContext(BlockKey origin, BlockFace facing, Tier tier, RoofHalf half) {

    /** Direccion "derecha" del jugador (sentido horario respecto a facing). */
    public BlockFace right() {
        return Directions.cw(facing);
    }

    /** Direccion hacia el jugador. */
    public BlockFace back() {
        return facing.getOppositeFace();
    }

    /** Posicion relativa: right (derecha+), up (arriba+), forward (hacia adelante+). */
    public BlockKey at(int right, int up, int forward) {
        BlockFace r = right();
        return new BlockKey(origin.world(),
                origin.x() + r.getModX() * right + facing.getModX() * forward,
                origin.y() + up,
                origin.z() + r.getModZ() * right + facing.getModZ() * forward);
    }
}
