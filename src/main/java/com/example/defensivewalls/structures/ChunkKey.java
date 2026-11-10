package com.example.defensivewalls.structures;

import java.util.UUID;

/** Clave de chunk (mundo + coordenadas de chunk). */
public record ChunkKey(UUID world, int cx, int cz) {
}
