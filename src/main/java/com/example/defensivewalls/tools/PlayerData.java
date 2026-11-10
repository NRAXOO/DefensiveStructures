package com.example.defensivewalls.tools;

import com.example.defensivewalls.structures.RoofHalf;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;

import java.util.UUID;

/** Seleccion y modo de herramienta de un jugador (se guarda en players.yml). */
public final class PlayerData {
    private final UUID uuid;
    private StructureType selectedType;
    private Tier selectedTier = Tier.WOOD;
    private ToolMode mode = ToolMode.BUILD;
    private Tier materialPreference = Tier.WOOD;
    private RoofHalf roofHalf = RoofHalf.AUTO;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() { return uuid; }
    public ToolMode mode() { return mode; }
    public void setMode(ToolMode mode) { this.mode = mode; }
    public Tier materialPreference() { return materialPreference; }
    public void setMaterialPreference(Tier t) { this.materialPreference = t; }
    public RoofHalf roofHalf() { return roofHalf; }
    public void setRoofHalf(RoofHalf h) { this.roofHalf = h; }

    public StructureType selectedType() { return selectedType; }
    public Tier selectedTier() { return selectedTier; }

    public void select(StructureOption option) {
        this.selectedType = option.type();
        this.selectedTier = option.tier();
    }

    /** Estructura activa o null. */
    public StructureOption selection() {
        return selectedType == null ? null : new StructureOption(selectedType, selectedTier);
    }
}
