package com.example.defensivewalls.utils;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Resuelve "tokens" de material (WOOD, STONE, IRON o nombre de Material). */
public final class ItemMatcher {
    private static final Set<Material> STONES = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS, Material.MOSSY_COBBLESTONE,
            Material.MOSSY_STONE_BRICKS, Material.COBBLED_DEEPSLATE, Material.BLACKSTONE,
            Material.DEEPSLATE_BRICKS, Material.SMOOTH_STONE);

    private ItemMatcher() {
    }

    public static boolean matches(String token, Material material) {
        if (token == null || material == null || material.isAir() || !material.isItem()) {
            return false;
        }
        switch (token.toUpperCase(Locale.ROOT)) {
            case "WOOD":
                return Tag.PLANKS.isTagged(material) || Tag.LOGS.isTagged(material);
            case "STONE":
                return STONES.contains(material);
            case "IRON":
                return material == Material.IRON_INGOT;
            default:
                return material == Material.matchMaterial(token);
        }
    }

    public static boolean matchesAny(Collection<String> tokens, Material material) {
        for (String token : tokens) {
            if (matches(token, material)) {
                return true;
            }
        }
        return false;
    }

    /** Material representativo (para reembolsos). */
    public static Material representative(String token) {
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "WOOD" -> Material.OAK_PLANKS;
            case "STONE" -> Material.COBBLESTONE;
            case "IRON" -> Material.IRON_INGOT;
            default -> {
                Material m = Material.matchMaterial(token);
                yield m == null ? Material.COBBLESTONE : m;
            }
        };
    }

    public static String describe(String token) {
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "WOOD" -> "madera";
            case "STONE" -> "piedra";
            case "IRON" -> "hierro";
            default -> token.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }
}
