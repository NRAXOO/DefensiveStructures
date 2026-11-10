package com.example.defensivewalls.tools;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.RoofHalf;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.structures.storage.AtomicFileWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Datos por jugador (seleccion, modo, preferencias) con persistencia en players.yml. */
public final class PlayerDataManager {

    private final DefensiveWallsPlugin plugin;
    private final Map<UUID, PlayerData> data = new HashMap<>();
    private final File file;
    private final AtomicFileWriter writer;

    public PlayerDataManager(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        this.writer = new AtomicFileWriter(plugin, file.toPath());
    }

    public void load() {
        data.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                PlayerData pd = new PlayerData(id);
                String type = s.getString("type");
                if (type != null) {
                    StructureType st = StructureType.valueOf(type);
                    Tier tier = Tier.valueOf(s.getString("tier", "WOOD"));
                    pd.select(new com.example.defensivewalls.structures.StructureOption(st, tier));
                }
                pd.setMode(ToolMode.valueOf(s.getString("mode", "BUILD")));
                pd.setMaterialPreference(Tier.valueOf(s.getString("material", "WOOD")));
                pd.setRoofHalf(RoofHalf.valueOf(s.getString("roof-half", "AUTO")));
                data.put(id, pd);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Datos de jugador invalidos '" + key + "': " + ex.getMessage());
            }
        }
    }

    public PlayerData get(Player player) {
        return data.computeIfAbsent(player.getUniqueId(), PlayerData::new);
    }

    public PlayerData peek(UUID id) {
        return data.get(id);
    }

    private String serialize() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerData pd : data.values()) {
            String p = "players." + pd.uuid();
            if (pd.selectedType() != null) {
                yaml.set(p + ".type", pd.selectedType().name());
                yaml.set(p + ".tier", pd.selectedTier().name());
            }
            yaml.set(p + ".mode", pd.mode().name());
            yaml.set(p + ".material", pd.materialPreference().name());
            yaml.set(p + ".roof-half", pd.roofHalf().name());
        }
        return yaml.saveToString();
    }

    public void save() {
        writer.writeAsync(serialize());
    }

    public void saveSync() {
        writer.writeSync(serialize());
    }
}
