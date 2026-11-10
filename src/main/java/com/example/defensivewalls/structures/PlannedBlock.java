package com.example.defensivewalls.structures;

import org.bukkit.block.data.BlockData;

/**
 * Bloque planificado de una estructura.
 *
 * @param physics si se aplica fisica al colocarlo (false para puertas, escaleras de mano, cofres...)
 */
public record PlannedBlock(BlockKey key, BlockData data, boolean physics) {
}
