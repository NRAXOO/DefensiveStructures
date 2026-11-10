package com.example.defensivewalls.structures;

/** Tipos de estructura. */
public enum StructureType {
    WALL("muro", "Muro", true, false, Tier.WOOD),
    ROOF("techo", "Techo", true, false, Tier.WOOD),
    FLOOR("suelo", "Suelo", true, false, Tier.WOOD),
    RAMP("escalera", "Escalera", true, false, Tier.WOOD),
    VERTICAL_STAIRS("escalera_vertical", "Escalera vertical", true, false, Tier.WOOD),
    DOUBLE_CHEST("cofre", "Cofre doble", false, true, Tier.WOOD),
    CABINET("gabinete", "Gabinete de mantenimiento", false, false, Tier.WOOD),
    IRON_DOOR("puerta", "Puerta blindada", false, true, Tier.IRON);

    private final String id;
    private final String displayName;
    private final boolean needsMaintenance;
    private final boolean tierless;
    private final Tier defaultTier;

    StructureType(String id, String displayName, boolean needsMaintenance, boolean tierless, Tier defaultTier) {
        this.id = id;
        this.displayName = displayName;
        this.needsMaintenance = needsMaintenance;
        this.tierless = tierless;
        this.defaultTier = defaultTier;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Techos, escaleras, paredes y suelos requieren mantenimiento. */
    public boolean needsMaintenance() {
        return needsMaintenance;
    }

    /** true si el tipo tiene un unico material fijo (cofre, puerta). */
    public boolean tierless() {
        return tierless;
    }

    public Tier defaultTier() {
        return defaultTier;
    }
}
