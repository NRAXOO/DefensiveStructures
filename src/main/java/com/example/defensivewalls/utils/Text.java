package com.example.defensivewalls.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Conversion de texto con codigos '&' a Component. */
public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    /** Texto normal (para chat, titulos de inventario). */
    public static Component plain(String legacy) {
        return LEGACY.deserialize(legacy == null ? "" : legacy);
    }

    /** Texto para items (sin cursiva por defecto). */
    public static Component item(String legacy) {
        return plain(legacy).decoration(TextDecoration.ITALIC, false);
    }

    public static String replace(String text, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }
}
