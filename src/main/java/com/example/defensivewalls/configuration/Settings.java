package com.example.defensivewalls.configuration;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.tools.ToolMode;
import com.example.defensivewalls.utils.Cost;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Acceso tipado a config.yml (siempre lee el valor actual, por lo que /wall reload aplica al instante). */
public final class Settings {

    private final DefensiveWallsPlugin plugin;

    public Settings(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration c() {
        return plugin.getConfig();
    }

    private Material material(String path, Material fallback) {
        String name = c().getString(path);
        if (name == null) {
            return fallback;
        }
        Material m = Material.matchMaterial(name);
        return m != null ? m : fallback;
    }

    // ---------------- permisos y herramientas ----------------
    public String permUse() { return c().getString("permissions.use", "defensivewalls.use"); }
    public String permAdmin() { return c().getString("permissions.admin", "defensivewalls.admin"); }

    public Material selectorMaterial() { return material("tools.selector-item", Material.PAPER); }
    public Material toolMaterial() { return material("tools.builder-item", Material.STICK); }
    public double toolRange() { return Math.max(2.0, c().getDouble("tools.range", 8.0)); }
    public boolean creativeFreeBuild() { return c().getBoolean("tools.creative-free-build", true); }
    public boolean dropItemOnRemove() { return c().getBoolean("tools.drop-item-on-remove", true); }
    public boolean paperBreaksStructures() { return c().getBoolean("tools.paper-breaks-structures", true); }
    public boolean selfRepairBlocks() { return c().getBoolean("tools.self-repair-blocks", true); }
    public boolean effects() { return c().getBoolean("tools.effects", true); }

    public String modeName(ToolMode mode) {
        return c().getString("modes." + mode.name(), mode.defaultName());
    }

    // ---------------- niveles ----------------
    public int maxDurability(Tier t) {
        return Math.max(1, c().getInt("tiers." + t.name() + ".max-durability", t.defaultDurability()));
    }

    public double maintenancePerMinute(Tier t) {
        return Math.max(0.0, c().getDouble("tiers." + t.name() + ".maintenance-per-minute", 0.25));
    }

    public String tierToken(Tier t) {
        String def = switch (t) {
            case WOOD -> "WOOD";
            case STONE -> "STONE";
            case IRON -> "IRON";
        };
        return c().getString("tiers." + t.name() + ".material", def);
    }

    public String tierName(Tier t) {
        return c().getString("tiers." + t.name() + ".display-name", t.id().substring(0, 1).toUpperCase(Locale.ROOT) + t.id().substring(1));
    }

    public String structureName(StructureType type, Tier tier) {
        if (type == StructureType.IRON_DOOR) {
            return type.displayName();
        }
        return type.displayName() + " de " + tierName(tier).toLowerCase(Locale.ROOT);
    }

    /** Todos los tokens de material usados por los niveles (materiales aceptados por el gabinete). */
    public List<String> acceptedTokens() {
        List<String> tokens = new ArrayList<>();
        for (Tier t : Tier.values()) {
            tokens.add(tierToken(t));
        }
        return tokens;
    }

    // ---------------- costes ----------------
    public Cost buildCost(StructureType type, Tier tier) {
        int def = switch (type) {
            case WALL -> 16;
            case ROOF, FLOOR, RAMP -> 9;
            case VERTICAL_STAIRS -> 6;
            case DOUBLE_CHEST -> 16;
            case CABINET -> 24;
            case IRON_DOOR -> 12;
        };
        return new Cost(tierToken(tier), Math.max(0, c().getInt("costs.build." + type.name() + "." + tier.name(), def)));
    }

    /** Coste de pasar AL nivel indicado. Por defecto 128 de madera (compatible con la v1). */
    public Cost upgradeCost(Tier target) {
        String p = "costs.upgrade." + target.name();
        return new Cost(c().getString(p + ".material", "WOOD"), Math.max(0, c().getInt(p + ".amount", 128)));
    }

    public boolean upgradeRestoresDurability() { return c().getBoolean("costs.upgrade.restore-full-durability", true); }

    public String repairToken(Tier t) { return c().getString("costs.repair." + t.name() + ".material", tierToken(t)); }
    public int repairUnits(Tier t) { return Math.max(1, c().getInt("costs.repair." + t.name() + ".units", 1)); }
    public int repairRestore(Tier t) { return Math.max(1, c().getInt("costs.repair." + t.name() + ".restore", 10)); }

    // ---------------- picos ----------------
    public int pickaxeDamage(Material tool) {
        String p = "pickaxe.damage-per-tool." + tool.name();
        if (c().contains(p)) {
            return Math.max(0, c().getInt(p));
        }
        return Math.max(0, c().getInt("pickaxe.damage", 1));
    }

    public int axeDamage(Material tool) {
        String p = "axe.damage-per-tool." + tool.name();
        if (c().contains(p)) {
            return Math.max(0, c().getInt(p));
        }
        return Math.max(0, c().getInt("axe.damage", 1));
    }

    public int pickaxeCooldownTicks() { return Math.max(1, c().getInt("pickaxe.hit-cooldown-ticks", 10)); }
    public boolean pickaxeAllowCreative() { return c().getBoolean("pickaxe.allow-creative", false); }

    // ---------------- deterioro y gabinetes ----------------
    public boolean deteriorationEnabled() { return c().getBoolean("deterioration.enabled", true); }
    public int tickSeconds() { return Math.max(1, c().getInt("deterioration.tick-seconds", 20)); }
    public double decayPerMinute() { return Math.max(0.0, c().getDouble("deterioration.damage-per-minute", 1.0)); }
    public double speedMultiplier() { return Math.max(0.0, c().getDouble("deterioration.speed-multiplier", 1.0)); }
    public boolean onlyLoadedChunks() { return c().getBoolean("deterioration.only-loaded-chunks", true); }

    public double associationRadius() { return Math.max(1.0, c().getDouble("cabinets.association-radius", 16.0)); }
    public int maxStructuresPerCabinet() { return Math.max(1, c().getInt("cabinets.max-structures", 64)); }
    public boolean requireCabinet() { return c().getBoolean("cabinets.require-cabinet-to-build", true); }

    public Material doorSupport() { return material("structures.iron-door.support-material", Material.STONE_BRICKS); }

    // ---------------- hologramas ----------------
    public boolean hologramsEnabled() { return c().getBoolean("holograms.enabled", true); }
    public double hologramViewDistance() { return Math.max(1.0, c().getDouble("holograms.view-distance", 5.0)); }
    public int hologramCheckTicks() { return Math.max(5, c().getInt("holograms.check-ticks", 20)); }
    public int hologramInteractSeconds() { return Math.max(1, c().getInt("holograms.interact-seconds", 8)); }
    public String hologramIcon() { return c().getString("holograms.icon", "\uD83D\uDEE1"); }
    public List<String> hologramLines() { return c().getStringList("holograms.lines"); }

    // ---------------- particulas ----------------
    public boolean particlesEnabled() { return c().getBoolean("particles.enabled", true); }
    public int particleUpdateTicks() { return Math.max(2, c().getInt("particles.update-ticks", 5)); }
    public int particleCacheMs() { return Math.max(0, c().getInt("particles.validation-cache-ms", 1000)); }
    public boolean particleCheckProtection() { return c().getBoolean("particles.check-protection", true); }
    public boolean particleShowReason() { return c().getBoolean("particles.show-reason", true); }
    public int particleMaxPoints() { return Math.max(10, c().getInt("particles.max-points", 160)); }
    public double particleSpacing() { return Math.max(0.2, c().getDouble("particles.spacing", 0.5)); }

    public Color particleColor(boolean valid) {
        String hex = c().getString(valid ? "particles.valid-color" : "particles.invalid-color", valid ? "55FF55" : "FF3333");
        try {
            int rgb = Integer.parseInt(hex.replace("#", ""), 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException ex) {
            return valid ? Color.LIME : Color.RED;
        }
    }

    // ---------------- items ----------------
    public Material itemMaterialOverride(String key) { return material("items.materials." + key, null); }
    public List<String> itemLore() { return c().getStringList("items.lore"); }
    public String itemNameFormat() { return c().getString("items.name-format", "&6%name%"); }
}
