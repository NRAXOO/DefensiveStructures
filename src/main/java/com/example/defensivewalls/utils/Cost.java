package com.example.defensivewalls.utils;

/**
 * Coste en materiales. {@code token} puede ser una categoria (WOOD, STONE, IRON)
 * o el nombre exacto de un Material de Bukkit.
 */
public record Cost(String token, int amount) {
}
