package com.example.defensivewalls.maintenance;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.durability.DurabilityState;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.utils.ItemMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Genera las lineas informativas (chat, interfaces) sobre estructuras y gabinetes. */
public final class MaintenanceReport {

    private final DefensiveWallsPlugin plugin;

    public MaintenanceReport(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public String bar(Structure s) {
        int filled = (int) Math.round(10.0 * s.durability() / Math.max(1, s.maxDurability()));
        DurabilityState st = plugin.durability().stateOf(s);
        return st.color() + "|".repeat(Math.max(0, filled)) + "&8" + "|".repeat(Math.max(0, 10 - filled));
    }

    public List<String> structureLines(Structure s) {
        List<String> lines = new ArrayList<>();
        DurabilityState st = plugin.durability().stateOf(s);
        lines.add("&6" + plugin.settings().structureName(s.type(), s.tier()));
        lines.add("&7Durabilidad: &f" + s.durability() + "/" + s.maxDurability() + " " + bar(s));
        lines.add("&7Estado: " + st.color() + st.label());
        lines.add("&7Posición: &f" + s.origin().x() + ", " + s.origin().y() + ", " + s.origin().z());
        if (s.type().needsMaintenance()) {
            String token = plugin.settings().tierToken(s.tier());
            lines.add("&7Consumo: &f" + String.format(Locale.ROOT, "%.2f", plugin.settings().maintenancePerMinute(s.tier()))
                    + " " + ItemMatcher.describe(token) + "/min &7(acumulado: &f"
                    + String.format(Locale.ROOT, "%.2f", s.maintenanceAccumulator()) + "&7)");
            Structure cab = plugin.structures().get(s.cabinetId());
            if (cab == null) {
                lines.add("&7Gabinete: &cninguno &7(sin mantenimiento)");
            } else {
                lines.add("&7Gabinete: &f" + cab.origin().x() + ", " + cab.origin().y() + ", " + cab.origin().z()
                        + (s.deteriorating() ? " &c(sin materiales)" : " &a(mantenida)"));
            }
        }
        return lines;
    }

    public List<String> cabinetLines(Structure cab) {
        List<String> lines = new ArrayList<>();
        List<Structure> linked = plugin.structures().linked(cab.id());
        lines.add("&6" + plugin.settings().structureName(cab.type(), cab.tier()));
        lines.add("&7Estructuras mantenidas: &f" + linked.size() + "/" + plugin.settings().maxStructuresPerCabinet());
        int deteriorating = 0;
        for (Structure s : linked) {
            if (s.deteriorating()) {
                deteriorating++;
            }
        }
        lines.add("&7Deteriorándose: " + (deteriorating > 0 ? "&c" : "&a") + deteriorating);
        Map<String, Double> consumption = plugin.cabinets().consumptionPerMinute(cab);
        if (consumption.isEmpty()) {
            lines.add("&7Consumo: &fninguno");
        }
        for (Map.Entry<String, Double> e : consumption.entrySet()) {
            int stock = plugin.cabinets().stockUnits(cab, e.getKey());
            double minutes = e.getValue() > 0 ? stock / e.getValue() : Double.POSITIVE_INFINITY;
            lines.add("&7" + capitalize(ItemMatcher.describe(e.getKey())) + ": &f"
                    + String.format(Locale.ROOT, "%.2f", e.getValue()) + "/min &7| stock &f" + stock
                    + " &7| dura &f" + (Double.isInfinite(minutes) ? "∞" : String.format(Locale.ROOT, "%.0f min", minutes)));
        }
        return lines;
    }

    public List<String> globalLines() {
        int total = 0, cabinets = 0, deteriorating = 0, orphans = 0;
        for (Structure s : plugin.structures().all()) {
            total++;
            if (s.type() == StructureType.CABINET) {
                cabinets++;
            }
            if (s.type().needsMaintenance()) {
                if (s.cabinetId() == null) {
                    orphans++;
                }
                if (s.deteriorating()) {
                    deteriorating++;
                }
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("&6Resumen de mantenimiento");
        lines.add("&7Estructuras: &f" + total + " &7| Gabinetes: &f" + cabinets);
        lines.add("&7Deteriorándose: &c" + deteriorating + " &7| Sin gabinete: &c" + orphans);
        return lines;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
