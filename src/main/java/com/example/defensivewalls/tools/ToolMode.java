package com.example.defensivewalls.tools;

/** Modos del palo de construccion. */
public enum ToolMode {
    BUILD("Construcción"),
    REMOVE("Eliminación"),
    UPGRADE("Mejora"),
    REPAIR("Reparación"),
    INFO("Información");

    private final String defaultName;

    ToolMode(String defaultName) {
        this.defaultName = defaultName;
    }

    public String defaultName() {
        return defaultName;
    }

    public ToolMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
