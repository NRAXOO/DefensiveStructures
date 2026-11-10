package com.example.defensivewalls.structures;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Datos persistentes de una estructura construida. */
public final class Structure {
    private final UUID id;
    private final StructureType type;
    private Tier tier;
    private final BlockKey origin;
    private final BlockFace facing;
    private final RoofHalf half;
    private final List<BlockKey> blocks;

    private int durability;
    private int maxDurability;
    private UUID cabinetId;
    private double maintenanceAccumulator;
    private double decayAccumulator;
    private boolean deteriorating;
    private final Map<Material, Integer> storage = new EnumMap<>(Material.class);

    private long lastWarning;

    public Structure(UUID id, StructureType type, Tier tier, BlockKey origin, BlockFace facing,
                     RoofHalf half, List<BlockKey> blocks, int maxDurability) {
        this.id = id;
        this.type = type;
        this.tier = tier;
        this.origin = origin;
        this.facing = facing;
        this.half = half;
        this.blocks = Collections.unmodifiableList(blocks);
        this.maxDurability = Math.max(1, maxDurability);
        this.durability = this.maxDurability;
    }

    public UUID id() { return id; }
    public StructureType type() { return type; }
    public Tier tier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
    public BlockKey origin() { return origin; }
    public BlockFace facing() { return facing; }
    public RoofHalf half() { return half; }
    public List<BlockKey> blocks() { return blocks; }

    public int durability() { return durability; }
    public void setDurability(int durability) { this.durability = Math.max(0, Math.min(durability, maxDurability)); }
    public int maxDurability() { return maxDurability; }
    public void setMaxDurability(int max) { this.maxDurability = Math.max(1, max); this.durability = Math.min(durability, this.maxDurability); }

    public UUID cabinetId() { return cabinetId; }
    public void setCabinetId(UUID cabinetId) { this.cabinetId = cabinetId; }

    public double maintenanceAccumulator() { return maintenanceAccumulator; }
    public void setMaintenanceAccumulator(double v) { this.maintenanceAccumulator = Math.max(0, v); }
    public double decayAccumulator() { return decayAccumulator; }
    public void setDecayAccumulator(double v) { this.decayAccumulator = Math.max(0, v); }
    public boolean deteriorating() { return deteriorating; }
    public void setDeteriorating(boolean deteriorating) { this.deteriorating = deteriorating; }

    public long lastWarning() { return lastWarning; }
    public void setLastWarning(long lastWarning) { this.lastWarning = lastWarning; }

    // ---- almacenamiento (solo gabinetes) ----
    public Map<Material, Integer> storage() { return storage; }

    public int stored(Material material) {
        return storage.getOrDefault(material, 0);
    }

    public void addStored(Material material, int amount) {
        if (amount > 0) {
            storage.merge(material, amount, Integer::sum);
        }
    }

    public StructureOption option() {
        return new StructureOption(type, tier);
    }
}
