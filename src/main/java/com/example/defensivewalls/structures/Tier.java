package com.example.defensivewalls.structures;

import java.util.Locale;

/** Niveles/materiales de una estructura. Para agregar niveles nuevos: anadir aqui y en config.yml. */
public enum Tier {
    WOOD("madera", 100),
    STONE("piedra", 200),
    IRON("hierro", 400);

    private final String id;
    private final int defaultDurability;

    Tier(String id, int defaultDurability) {
        this.id = id;
        this.defaultDurability = defaultDurability;
    }

    public String id() {
        return id;
    }

    public int defaultDurability() {
        return defaultDurability;
    }

    /** Siguiente nivel de mejora o null. (IRON solo existe para puertas, no se alcanza por mejora.) */
    public Tier next() {
        return this == WOOD ? STONE : null;
    }

    public static Tier parse(String s) {
        if (s == null) {
            return null;
        }
        return switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "madera", "wood" -> WOOD;
            case "piedra", "stone" -> STONE;
            case "hierro", "iron" -> IRON;
            default -> null;
        };
    }
}
