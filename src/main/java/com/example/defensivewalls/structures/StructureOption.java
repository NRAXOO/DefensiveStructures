package com.example.defensivewalls.structures;

/** Combinacion tipo + material construible/entregable. */
public record StructureOption(StructureType type, Tier tier) {

    /** Identificador usado en /wall give (ej: techo_madera, cofre, puerta). */
    public String id() {
        return type.tierless() ? type.id() : type.id() + "_" + tier.id();
    }
}
