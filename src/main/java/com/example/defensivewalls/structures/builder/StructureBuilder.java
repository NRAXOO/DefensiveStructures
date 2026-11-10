package com.example.defensivewalls.structures.builder;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureOption;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.utils.Cost;
import com.example.defensivewalls.utils.ItemMatcher;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/** Valida, cobra, construye de forma atomica y vincula al gabinete. */
public final class StructureBuilder {

    private final DefensiveWallsPlugin plugin;

    public StructureBuilder(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public PlacementResult build(BuildRequest r) {
        PlacementResult validation = plugin.validator().validate(r);
        if (!validation.ok()) {
            return validation;
        }
        StructureOption option = r.option();
        Player player = r.player();

        Cost cost = null;
        boolean free = player.getGameMode() == GameMode.CREATIVE && plugin.settings().creativeFreeBuild();
        if (r.charge() && !free) {
            cost = plugin.settings().buildCost(option.type(), option.tier());
            if (!plugin.costs().has(player, cost) || !plugin.costs().take(player, cost)) {
                return PlacementResult.fail("no-materials",
                        "%amount%", String.valueOf(cost.amount()),
                        "%material%", ItemMatcher.describe(cost.token()),
                        "%have%", String.valueOf(plugin.costs().count(player, cost.token())));
            }
        }

        Structure structure;
        try {
            structure = plugin.structures().create(option.type(), option.tier(), r.origin(), r.facing(), r.half(),
                    validation.plan());
        } catch (RuntimeException ex) {
            if (cost != null) {
                plugin.costs().refund(player, cost);
            }
            plugin.getLogger().log(Level.SEVERE, "Fallo al construir " + option.id(), ex);
            return PlacementResult.fail("internal-error");
        }

        if (option.type().needsMaintenance()) {
            Structure cabinet = plugin.cabinets().findNearest(r.origin());
            if (cabinet != null) {
                plugin.structures().link(structure, cabinet);
            }
        } else if (option.type() == StructureType.CABINET) {
            plugin.cabinets().adoptNearby(structure, false);
        }
        return PlacementResult.ok(validation.plan(), structure);
    }
}
