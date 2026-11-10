package com.example.defensivewalls.structures;

/** Orientacion vertical de las losas de un techo. AUTO se resuelve segun la cara apuntada. */
public enum RoofHalf {
    AUTO, BOTTOM, TOP;

    public RoofHalf next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String label() {
        return switch (this) {
            case AUTO -> "Automatica";
            case BOTTOM -> "Inferior";
            case TOP -> "Superior";
        };
    }
}
