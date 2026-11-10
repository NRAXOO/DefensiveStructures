package com.example.defensivewalls.structures.types;

import com.example.defensivewalls.configuration.Settings;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Registro de formas. Para anadir un tipo nuevo basta con registrarlo aqui. */
public final class ShapeRegistry {

    private final Map<StructureType, StructureShape> shapes = new EnumMap<>(StructureType.class);

    public ShapeRegistry(Settings settings) {
        register(new WallShape());
        register(new RoofShape());
        register(new FloorShape());
        register(new RampShape());
        register(new VerticalStairsShape());
        register(new DoubleChestShape());
        register(new CabinetShape());
        register(new IronDoorShape(settings));
    }

    private void register(StructureShape shape) {
        shapes.put(shape.type(), shape);
    }

    public StructureShape get(StructureType type) {
        return shapes.get(type);
    }

    /** Todas las combinaciones tipo+material validas. */
    public List<StructureOption> options() {
        List<StructureOption> list = new ArrayList<>();
        for (StructureType type : StructureType.values()) {
            StructureShape shape = shapes.get(type);
            if (shape == null) {
                continue;
            }
            for (Tier tier : Tier.values()) {
                if (shape.supports(tier)) {
                    list.add(new StructureOption(type, tier));
                }
            }
        }
        return list;
    }

    /** Interpreta ids como "techo_madera", "cofre", "madera" (muro de madera, compatibilidad con v1)... */
    public StructureOption parse(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        s = switch (s) {
            case "madera", "wood", "muro_wood" -> "muro_madera";
            case "piedra", "stone", "muro_stone" -> "muro_piedra";
            case "cofre_doble" -> "cofre";
            case "puerta_blindada" -> "puerta";
            case "gabinete_de_mantenimiento" -> "gabinete";
            default -> s;
        };
        for (StructureOption option : options()) {
            if (option.id().equals(s)) {
                return option;
            }
        }
        return null;
    }
}
