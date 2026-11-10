package com.example.defensivewalls.structures.storage;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.RoofHalf;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.structures.manager.StructureManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistencia de estructuras en structures.yml (con migracion automatica de walls.yml de la v1). */
public final class StructureStorage {

    private final DefensiveWallsPlugin plugin;
    private final File file;
    private final File legacyFile;
    private final AtomicFileWriter writer;

    public StructureStorage(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "structures.yml");
        this.legacyFile = new File(plugin.getDataFolder(), "walls.yml");
        this.writer = new AtomicFileWriter(plugin, file.toPath());
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    public void load(StructureManager manager) {
        if (file.exists()) {
            loadCurrent(manager);
        } else if (legacyFile.exists()) {
            migrateLegacy(manager);
        }
    }

    private void loadCurrent(StructureManager manager) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("structures");
        if (root == null) {
            return;
        }
        int loaded = 0;
        for (String idString : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(idString);
                if (s == null) {
                    continue;
                }
                UUID id = UUID.fromString(idString);
                StructureType type = StructureType.valueOf(s.getString("type", ""));
                Tier tier = Tier.valueOf(s.getString("tier", "WOOD"));
                UUID world = UUID.fromString(s.getString("world-uid", ""));
                BlockKey origin = new BlockKey(world, s.getInt("origin.x"), s.getInt("origin.y"), s.getInt("origin.z"));
                BlockFace facing = BlockFace.valueOf(s.getString("facing", "NORTH"));
                RoofHalf half = RoofHalf.valueOf(s.getString("half", "BOTTOM"));

                List<PlannedBlock> plan = manager.planFor(type, tier, origin, facing, half);
                List<BlockKey> keys = new ArrayList<>(plan.size());
                for (PlannedBlock pb : plan) {
                    keys.add(pb.key());
                }
                Structure st = new Structure(id, type, tier, origin, facing, half, keys,
                        s.getInt("max-durability", plugin.settings().maxDurability(tier)));
                st.setDurability(s.getInt("durability", st.maxDurability()));
                String cab = s.getString("cabinet");
                if (cab != null && !cab.isEmpty()) {
                    st.setCabinetId(UUID.fromString(cab));
                }
                st.setMaintenanceAccumulator(s.getDouble("maintenance-accumulator", 0.0));
                st.setDecayAccumulator(s.getDouble("decay-accumulator", 0.0));
                st.setDeteriorating(s.getBoolean("deteriorating", false));

                ConfigurationSection stored = s.getConfigurationSection("storage");
                if (stored != null) {
                    for (String matName : stored.getKeys(false)) {
                        Material m = Material.matchMaterial(matName);
                        if (m != null) {
                            st.addStored(m, stored.getInt(matName));
                        }
                    }
                }
                if (manager.registerLoaded(st)) {
                    loaded++;
                } else {
                    plugin.getLogger().warning("Estructura " + id + " se solapa con otra; se ignora.");
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Entrada de estructura invalida '" + idString + "': " + ex.getMessage());
            }
        }
        manager.relinkAfterLoad();
        plugin.getLogger().info("Estructuras cargadas: " + loaded);
    }

    /** Importa los muros de la v1 (walls.yml). Los muros importados quedan sin gabinete. */
    private void migrateLegacy(StructureManager manager) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(legacyFile);
        ConfigurationSection root = yaml.getConfigurationSection("walls");
        int migrated = 0;
        if (root != null) {
            for (String idString : root.getKeys(false)) {
                try {
                    ConfigurationSection s = root.getConfigurationSection(idString);
                    if (s == null) {
                        continue;
                    }
                    UUID id = UUID.fromString(idString);
                    UUID world = UUID.fromString(s.getString("world-uid", ""));
                    BlockKey origin = new BlockKey(world, s.getInt("origin.x"), s.getInt("origin.y"), s.getInt("origin.z"));
                    BlockFace facing = "Z".equalsIgnoreCase(s.getString("axis", "X")) ? BlockFace.EAST : BlockFace.NORTH;
                    Tier tier = "STONE".equalsIgnoreCase(s.getString("level", "WOOD")) ? Tier.STONE : Tier.WOOD;

                    List<PlannedBlock> plan = manager.planFor(StructureType.WALL, tier, origin, facing, RoofHalf.BOTTOM);
                    List<BlockKey> keys = new ArrayList<>();
                    for (PlannedBlock pb : plan) {
                        keys.add(pb.key());
                    }
                    Structure st = new Structure(id, StructureType.WALL, tier, origin, facing, RoofHalf.BOTTOM, keys,
                            plugin.settings().maxDurability(tier));
                    if (manager.registerLoaded(st)) {
                        migrated++;
                    }
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Muro v1 invalido '" + idString + "': " + ex.getMessage());
                }
            }
        }
        // Se guarda en el formato nuevo y se conserva una copia del archivo antiguo.
        writer.writeSync(serialize(manager));
        File backup = new File(plugin.getDataFolder(), "walls.yml.migrated");
        if (!legacyFile.renameTo(backup)) {
            plugin.getLogger().warning("No se pudo renombrar walls.yml tras la migracion.");
        }
        plugin.getLogger().info("Migrados " + migrated + " muros de la v1. Vincúlalos con un gabinete (/wall cabinet link).");
    }

    // ------------------------------------------------------------------
    // Guardado
    // ------------------------------------------------------------------

    private String serialize(StructureManager manager) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("data-version", 2);
        for (Structure s : manager.all()) {
            String p = "structures." + s.id();
            yaml.set(p + ".type", s.type().name());
            yaml.set(p + ".tier", s.tier().name());
            yaml.set(p + ".world-uid", s.origin().world().toString());
            World w = Bukkit.getWorld(s.origin().world());
            if (w != null) {
                yaml.set(p + ".world-name", w.getName());
            }
            yaml.set(p + ".origin.x", s.origin().x());
            yaml.set(p + ".origin.y", s.origin().y());
            yaml.set(p + ".origin.z", s.origin().z());
            yaml.set(p + ".facing", s.facing().name());
            yaml.set(p + ".half", s.half().name());
            yaml.set(p + ".durability", s.durability());
            yaml.set(p + ".max-durability", s.maxDurability());
            yaml.set(p + ".cabinet", s.cabinetId() == null ? "" : s.cabinetId().toString());
            yaml.set(p + ".maintenance-accumulator", s.maintenanceAccumulator());
            yaml.set(p + ".decay-accumulator", s.decayAccumulator());
            yaml.set(p + ".deteriorating", s.deteriorating());
            List<String> blocks = new ArrayList<>();
            for (BlockKey key : s.blocks()) {
                blocks.add(key.serialize());
            }
            yaml.set(p + ".blocks", blocks);
            if (s.type() == StructureType.CABINET) {
                for (var entry : s.storage().entrySet()) {
                    if (entry.getValue() > 0) {
                        yaml.set(p + ".storage." + entry.getKey().name(), entry.getValue());
                    }
                }
            }
        }
        return yaml.saveToString();
    }

    /** Serializa en el hilo principal y escribe en segundo plano. */
    public void saveAsync(StructureManager manager) {
        writer.writeAsync(serialize(manager));
    }

    public void saveSync(StructureManager manager) {
        writer.writeSync(serialize(manager));
    }
}
