package com.example.defensivewalls.utils;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Creacion e identificacion de los objetos de estructura (marcados con PersistentDataContainer). */
public final class ItemFactory {

    private final DefensiveWallsPlugin plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey legacyKey; // objetos "Muro" de la v1

    public ItemFactory(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "structure_type");
        this.tierKey = new NamespacedKey(plugin, "structure_tier");
        this.legacyKey = new NamespacedKey(plugin, "wall_level");
    }

    /** Material base del objeto que se entrega (se puede cambiar en config items.materials.TIPO_NIVEL). */
    public Material baseMaterial(StructureOption o) {
        Material override = plugin.settings().itemMaterialOverride(o.type().name() + "_" + o.tier().name());
        if (override != null && override.isBlock() && override.isItem()) {
            return override;
        }
        boolean stone = o.tier() == Tier.STONE;
        return switch (o.type()) {
            case WALL, ROOF, FLOOR, RAMP, VERTICAL_STAIRS -> stone ? Material.STONE_BRICKS : Material.DARK_OAK_PLANKS;
            case DOUBLE_CHEST -> Material.CHEST;
            case CABINET -> Material.CHISELED_BOOKSHELF;
            case IRON_DOOR -> Material.IRON_BLOCK;
        };
    }

    /** Icono representativo para las interfaces. */
    public Material iconMaterial(StructureOption o) {
        boolean stone = o.tier() == Tier.STONE;
        return switch (o.type()) {
            case WALL -> stone ? Material.STONE_BRICK_WALL : Material.DARK_OAK_FENCE;
            case ROOF -> stone ? Material.STONE_BRICK_SLAB : Material.DARK_OAK_SLAB;
            case FLOOR -> stone ? Material.STONE_BRICKS : Material.DARK_OAK_PLANKS;
            case RAMP -> stone ? Material.STONE_BRICK_STAIRS : Material.DARK_OAK_STAIRS;
            case VERTICAL_STAIRS -> Material.LADDER;
            case DOUBLE_CHEST -> Material.CHEST;
            case CABINET -> Material.CHISELED_BOOKSHELF;
            case IRON_DOOR -> Material.IRON_DOOR;
        };
    }

    public ItemStack create(StructureOption option, int amount) {
        ItemStack item = new ItemStack(baseMaterial(option), Math.max(1, Math.min(amount, 64)));
        ItemMeta meta = item.getItemMeta();
        String name = plugin.settings().structureName(option.type(), option.tier());
        meta.displayName(Text.item(Text.replace(plugin.settings().itemNameFormat(), "%name%", name)));

        List<Component> lore = new ArrayList<>();
        for (String line : plugin.settings().itemLore()) {
            lore.add(Text.item(Text.replace(line,
                    "%name%", name,
                    "%max%", String.valueOf(plugin.settings().maxDurability(option.tier())))));
        }
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, option.type().name());
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, option.tier().name());
        item.setItemMeta(meta);
        return item;
    }

    /** Opcion que representa el objeto, o null si no es un objeto de estructura. */
    public StructureOption parse(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        try {
            String type = pdc.get(typeKey, PersistentDataType.STRING);
            if (type != null) {
                String tier = pdc.get(tierKey, PersistentDataType.STRING);
                StructureType st = StructureType.valueOf(type);
                Tier t = tier == null ? st.defaultTier() : Tier.valueOf(tier);
                return new StructureOption(st, t);
            }
            String legacy = pdc.get(legacyKey, PersistentDataType.STRING);
            if (legacy != null) {
                return new StructureOption(StructureType.WALL, Tier.valueOf(legacy));
            }
        } catch (IllegalArgumentException ignored) {
            // dato corrupto: se trata como objeto normal
        }
        return null;
    }

    public boolean isStructureItem(ItemStack item) {
        return parse(item) != null;
    }
}
