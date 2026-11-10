package com.example.defensivewalls.structures;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.UUID;

/** Posicion inmutable de un bloque (mundo por UUID + coordenadas). */
public record BlockKey(UUID world, int x, int y, int z) {

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    /** Devuelve el bloque o null si el mundo no esta cargado. */
    public Block toBlock() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : w.getBlockAt(x, y, z);
    }

    public ChunkKey chunk() {
        return new ChunkKey(world, x >> 4, z >> 4);
    }

    public BlockKey shift(BlockFace face, int amount) {
        return new BlockKey(world, x + face.getModX() * amount, y + face.getModY() * amount, z + face.getModZ() * amount);
    }

    public String serialize() {
        return x + "," + y + "," + z;
    }
}
