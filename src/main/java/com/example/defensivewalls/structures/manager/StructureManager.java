package com.example.defensivewalls.structures.manager;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.ChunkKey;
import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.RoofHalf;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureObserver;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.structures.types.StructureShape;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Registro de estructuras y operaciones sobre ellas (crear, mejorar, destruir, reparar bloques, vincular).
 * Solo debe usarse desde el hilo principal.
 */
public final class StructureManager {

    private final DefensiveWallsPlugin plugin;
    private final Map<UUID, Structure> structures = new LinkedHashMap<>();
    private final Map<BlockKey, UUID> index = new HashMap<>();
    private final Map<ChunkKey, Set<UUID>> byChunk = new HashMap<>();
    private final Map<UUID, Set<UUID>> byCabinet = new HashMap<>();
    private final List<StructureObserver> observers = new ArrayList<>();
    private boolean dirty;

    public StructureManager(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public void addObserver(StructureObserver observer) {
        observers.add(observer);
    }

    public Collection<Structure> all() {
        return Collections.unmodifiableCollection(structures.values());
    }

    public int count() {
        return structures.size();
    }

    public Structure get(UUID id) {
        return id == null ? null : structures.get(id);
    }

    public Structure at(BlockKey key) {
        UUID id = index.get(key);
        return id == null ? null : structures.get(id);
    }

    public Structure at(Block block) {
        return block == null ? null : at(BlockKey.of(block));
    }

    public boolean isStructureBlock(Block block) {
        return block != null && index.containsKey(BlockKey.of(block));
    }

    /** Estructuras vinculadas a un gabinete (copia). */
    public List<Structure> linked(UUID cabinetId) {
        List<Structure> out = new ArrayList<>();
        Set<UUID> ids = byCabinet.get(cabinetId);
        if (ids != null) {
            for (UUID id : ids) {
                Structure s = structures.get(id);
                if (s != null) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    public int linkedCount(UUID cabinetId) {
        Set<UUID> ids = byCabinet.get(cabinetId);
        return ids == null ? 0 : ids.size();
    }

    /** Estructuras cuyo origen esta a menos de {@code radius} bloques (usa el indice por chunk). */
    public List<Structure> nearby(UUID world, double x, double y, double z, double radius) {
        List<Structure> out = new ArrayList<>();
        int chunkRadius = (int) Math.ceil(radius / 16.0) + 1;
        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        double r2 = radius * radius;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                Set<UUID> ids = byChunk.get(new ChunkKey(world, cx + dx, cz + dz));
                if (ids == null) {
                    continue;
                }
                for (UUID id : ids) {
                    Structure s = structures.get(id);
                    if (s == null) {
                        continue;
                    }
                    double ddx = s.origin().x() + 0.5 - x;
                    double ddy = s.origin().y() + 0.5 - y;
                    double ddz = s.origin().z() + 0.5 - z;
                    if (ddx * ddx + ddy * ddy + ddz * ddz <= r2) {
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }

    public List<PlannedBlock> planFor(StructureType type, Tier tier, BlockKey origin, BlockFace facing, RoofHalf half) {
        StructureShape shape = plugin.shapes().get(type);
        return shape.plan(new PlanContext(origin, facing, tier, half));
    }

    public List<PlannedBlock> planOf(Structure s) {
        return planFor(s.type(), s.tier(), s.origin(), s.facing(), s.half());
    }

    public Location center(Structure s) {
        World world = Bukkit.getWorld(s.origin().world());
        if (world == null) {
            return null;
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockKey k : s.blocks()) {
            minX = Math.min(minX, k.x());
            maxX = Math.max(maxX, k.x());
            minY = Math.min(minY, k.y());
            maxY = Math.max(maxY, k.y());
            minZ = Math.min(minZ, k.z());
            maxZ = Math.max(maxZ, k.z());
        }
        return new Location(world, (minX + maxX + 1) / 2.0, (minY + maxY + 1) / 2.0, (minZ + maxZ + 1) / 2.0);
    }

    // ------------------------------------------------------------------
    // Registro interno
    // ------------------------------------------------------------------

    private void register(Structure s) {
        structures.put(s.id(), s);
        for (BlockKey k : s.blocks()) {
            index.put(k, s.id());
        }
        byChunk.computeIfAbsent(s.origin().chunk(), c -> new HashSet<>()).add(s.id());
        if (s.cabinetId() != null) {
            byCabinet.computeIfAbsent(s.cabinetId(), c -> new HashSet<>()).add(s.id());
        }
    }

    private void unregister(Structure s) {
        structures.remove(s.id());
        for (BlockKey k : s.blocks()) {
            index.remove(k, s.id());
        }
        Set<UUID> chunkSet = byChunk.get(s.origin().chunk());
        if (chunkSet != null) {
            chunkSet.remove(s.id());
            if (chunkSet.isEmpty()) {
                byChunk.remove(s.origin().chunk());
            }
        }
        if (s.cabinetId() != null) {
            Set<UUID> set = byCabinet.get(s.cabinetId());
            if (set != null) {
                set.remove(s.id());
            }
        }
        byCabinet.remove(s.id());
    }

    /** Usado por el almacenamiento al cargar. Devuelve false si se solapa con otra estructura. */
    public boolean registerLoaded(Structure s) {
        for (BlockKey k : s.blocks()) {
            if (index.containsKey(k)) {
                return false;
            }
        }
        register(s);
        return true;
    }

    /** Reconstruye el indice por gabinete y elimina vinculos a gabinetes que ya no existen. */
    public void relinkAfterLoad() {
        byCabinet.clear();
        for (Structure s : structures.values()) {
            if (s.cabinetId() != null) {
                Structure cab = structures.get(s.cabinetId());
                if (cab == null || cab.type() != StructureType.CABINET) {
                    s.setCabinetId(null);
                } else {
                    byCabinet.computeIfAbsent(cab.id(), c -> new HashSet<>()).add(s.id());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Vinculos con gabinetes
    // ------------------------------------------------------------------

    public void link(Structure s, Structure cabinet) {
        if (cabinet.type() != StructureType.CABINET || !s.type().needsMaintenance()) {
            return;
        }
        unlinkQuiet(s);
        s.setCabinetId(cabinet.id());
        byCabinet.computeIfAbsent(cabinet.id(), c -> new HashSet<>()).add(s.id());
        changed(s);
        markDirty();
    }

    public void unlink(Structure s) {
        unlinkQuiet(s);
        changed(s);
        markDirty();
    }

    private void unlinkQuiet(Structure s) {
        if (s.cabinetId() != null) {
            Set<UUID> set = byCabinet.get(s.cabinetId());
            if (set != null) {
                set.remove(s.id());
            }
            s.setCabinetId(null);
        }
    }

    // ------------------------------------------------------------------
    // Crear / mejorar / destruir / reparar
    // ------------------------------------------------------------------

    /**
     * Crea y pinta una estructura de forma atomica: si algo falla se restauran los bloques anteriores.
     *
     * @throws RuntimeException si la colocacion falla (los bloques ya se han restaurado)
     */
    public Structure create(StructureType type, Tier tier, BlockKey origin, BlockFace facing, RoofHalf half,
                            List<PlannedBlock> plan) {
        List<BlockKey> keys = new ArrayList<>(plan.size());
        List<BlockState> previous = new ArrayList<>(plan.size());
        for (PlannedBlock pb : plan) {
            keys.add(pb.key());
            Block b = pb.key().toBlock();
            if (b == null) {
                throw new IllegalStateException("Mundo no disponible");
            }
            previous.add(b.getState());
        }
        Structure s = new Structure(UUID.randomUUID(), type, tier, origin, facing, half, keys,
                plugin.settings().maxDurability(tier));
        register(s);
        try {
            for (PlannedBlock pb : plan) {
                pb.key().toBlock().setBlockData(pb.data(), pb.physics());
            }
        } catch (RuntimeException ex) {
            unregister(s);
            for (BlockState state : previous) {
                state.update(true, false);
            }
            throw ex;
        }
        markDirty();
        saveNow();
        return s;
    }

    /** Mejora al siguiente nivel conservando identidad, vinculo y almacenamiento. */
    public boolean upgrade(Structure s) {
        Tier next = s.tier().next();
        StructureShape shape = plugin.shapes().get(s.type());
        if (next == null || shape == null || !shape.supports(next)) {
            return false;
        }
        s.setTier(next);
        s.setMaxDurability(plugin.settings().maxDurability(next));
        if (plugin.settings().upgradeRestoresDurability()) {
            s.setDurability(s.maxDurability());
        }
        s.setDecayAccumulator(0);
        s.setDeteriorating(false);
        for (PlannedBlock pb : planOf(s)) {
            Block b = pb.key().toBlock();
            if (b != null) {
                b.setBlockData(pb.data(), pb.physics());
            }
        }
        effects(s, Sound.BLOCK_STONE_PLACE, false);
        changed(s);
        saveNow();
        return true;
    }

    /**
     * Elimina la estructura completa. Solo se borran bloques que siguen siendo los esperados.
     * El contenido de cofres y los materiales de gabinetes se sueltan al suelo (no se pierde nada).
     */
    public void destroy(Structure s, boolean dropItem, boolean effects) {
        if (structures.get(s.id()) != s) {
            return; // ya destruida
        }
        World world = Bukkit.getWorld(s.origin().world());
        Location center = center(s);
        List<PlannedBlock> plan = planOf(s);
        unregister(s);

        List<ItemStack> drops = new ArrayList<>();
        if (world != null) {
            if (s.type() == StructureType.DOUBLE_CHEST) {
                for (PlannedBlock pb : plan) {
                    Block b = pb.key().toBlock();
                    if (b != null && b.getState() instanceof org.bukkit.block.Chest chest) {
                        for (ItemStack item : chest.getBlockInventory().getContents()) {
                            if (item != null && !item.getType().isAir()) {
                                drops.add(item.clone());
                            }
                        }
                        chest.getBlockInventory().clear();
                    }
                }
            }
            for (PlannedBlock pb : plan) {
                Block b = pb.key().toBlock();
                if (b != null && b.getType() == pb.data().getMaterial()) {
                    b.setType(Material.AIR, pb.physics());
                }
            }
            if (effects) {
                effectsAt(center, Sound.BLOCK_WOOD_BREAK, s.tier());
            }
        }

        if (s.type() == StructureType.CABINET) {
            for (var entry : s.storage().entrySet()) {
                int left = entry.getValue();
                while (left > 0) {
                    int n = Math.min(left, entry.getKey().getMaxStackSize());
                    drops.add(new ItemStack(entry.getKey(), n));
                    left -= n;
                }
            }
            s.storage().clear();
            // Las estructuras asociadas quedan "sin mantenimiento" pero NO desaparecen.
            for (Structure orphan : linked(s.id())) {
                orphan.setCabinetId(null);
                changed(orphan);
            }
            byCabinet.remove(s.id());
        }

        if (dropItem) {
            drops.add(plugin.items().create(s.option(), 1));
        }
        if (center != null) {
            for (ItemStack drop : drops) {
                center.getWorld().dropItemNaturally(center, drop);
            }
        }
        for (StructureObserver o : new ArrayList<>(observers)) {
            o.removed(s);
        }
        saveNow();
    }

    /** Reconstruye bloques faltantes o cambiados. Devuelve cuantos reparo. */
    public int repairBlocks(Structure s) {
        World world = Bukkit.getWorld(s.origin().world());
        if (world == null) {
            return 0;
        }
        int repaired = 0;
        for (PlannedBlock pb : planOf(s)) {
            BlockKey k = pb.key();
            if (!world.isChunkLoaded(k.x() >> 4, k.z() >> 4)) {
                continue;
            }
            Block b = world.getBlockAt(k.x(), k.y(), k.z());
            if (b.getType() != pb.data().getMaterial()) {
                b.setBlockData(pb.data(), pb.physics());
                repaired++;
            }
        }
        return repaired;
    }

    public int verifyAll() {
        int total = 0;
        for (Structure s : new ArrayList<>(structures.values())) {
            total += repairBlocks(s);
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Efectos, notificaciones y guardado
    // ------------------------------------------------------------------

    private void effects(Structure s, Sound sound, boolean unused) {
        effectsAt(center(s), sound, s.tier());
    }

    private void effectsAt(Location loc, Sound sound, Tier tier) {
        if (loc == null || !plugin.settings().effects()) {
            return;
        }
        Material m = tier == Tier.WOOD ? Material.DARK_OAK_PLANKS : (tier == Tier.IRON ? Material.IRON_BLOCK : Material.STONE_BRICKS);
        loc.getWorld().playSound(loc, sound, 1.0f, 0.8f);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 50, 0.7, 0.9, 0.7, m.createBlockData());
    }

    /** Avisa a hologramas/interfaces de que la estructura cambio. */
    public void changed(Structure s) {
        for (StructureObserver o : new ArrayList<>(observers)) {
            o.changed(s);
        }
    }

    public void markDirty() {
        dirty = true;
    }

    /** Guardado diferido (lo llama una tarea periodica). */
    public void flushIfDirty() {
        if (dirty) {
            saveNow();
        }
    }

    public void saveNow() {
        dirty = false;
        plugin.storage().saveAsync(this);
    }

    public void saveSync() {
        dirty = false;
        plugin.storage().saveSync(this);
    }
}
