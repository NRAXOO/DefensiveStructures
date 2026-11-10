package com.example.defensivewalls.structures;

/** Observador de cambios en estructuras (hologramas, interfaces...). */
public interface StructureObserver {
    default void changed(Structure structure) {
    }

    default void removed(Structure structure) {
    }
}
