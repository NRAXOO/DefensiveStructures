package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.structures.PlanContext;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;

import java.util.List;

/**
 * Define la forma de un tipo de estructura. La planificacion es determinista:
 * misma entrada = mismos bloques, por lo que sirve para construir, reparar y destruir.
 */
public interface StructureShape {

    StructureType type();

    boolean supports(Tier tier);

    List<PlannedBlock> plan(PlanContext ctx);

    /** true si el bloque bajo el origen debe ser solido (puertas). */
    default boolean requiresSolidGround() {
        return false;
    }
}
