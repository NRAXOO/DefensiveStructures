package com.example.defensivewalls.structures.builder;

import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.RoofHalf;
import com.example.defensivewalls.structures.StructureOption;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Peticion de construccion.
 *
 * @param half          ya resuelta (BOTTOM o TOP)
 * @param charge        cobrar el coste en materiales al jugador
 * @param probe         comprobar protecciones de otros plugins
 * @param originPlaced  el bloque origen ya fue colocado por el jugador (colocacion con objeto)
 */
public record BuildRequest(Player player, StructureOption option, BlockKey origin, BlockFace facing, RoofHalf half,
                           boolean charge, boolean probe, boolean originPlaced, ItemStack probeItem,
                           EquipmentSlot hand) {
}
