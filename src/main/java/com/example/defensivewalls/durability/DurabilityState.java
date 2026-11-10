package com.example.defensivewalls.durability;

/** Estados de durabilidad: 100-75 Excelente, 74-50 Bueno, 49-25 Dañado, 24-1 Crítico, 0 Destruido. */
public enum DurabilityState {
    EXCELLENT("Excelente", "&a"),
    GOOD("Bueno", "&e"),
    DAMAGED("Dañado", "&6"),
    CRITICAL("Crítico", "&c"),
    DESTROYED("Destruido", "&4");

    private final String label;
    private final String color;

    DurabilityState(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    public static DurabilityState of(int durability, int max) {
        if (durability <= 0) {
            return DESTROYED;
        }
        double pct = 100.0 * durability / Math.max(1, max);
        if (pct >= 75.0) {
            return EXCELLENT;
        }
        if (pct >= 50.0) {
            return GOOD;
        }
        if (pct >= 25.0) {
            return DAMAGED;
        }
        return CRITICAL;
    }
}
