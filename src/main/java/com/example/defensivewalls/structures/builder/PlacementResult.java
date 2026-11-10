package com.example.defensivewalls.structures.builder;

import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.Structure;

import java.util.List;

/** Resultado de validar/construir. {@code error} es una clave de mensaje de config. */
public record PlacementResult(boolean ok, String error, String[] args, List<PlannedBlock> plan, Structure structure) {

    public static PlacementResult ok(List<PlannedBlock> plan, Structure structure) {
        return new PlacementResult(true, null, new String[0], plan, structure);
    }

    public static PlacementResult fail(String error, String... args) {
        return new PlacementResult(false, error, args, List.of(), null);
    }
}
