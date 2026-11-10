package com.example.defensivewalls.tools;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureType;
import com.example.defensivewalls.structures.Tier;
import com.example.defensivewalls.structures.builder.BuildRequest;
import com.example.defensivewalls.structures.builder.PlacementResult;
import com.example.defensivewalls.structures.types.StructureShape;
import com.example.defensivewalls.utils.Cost;
import com.example.defensivewalls.utils.ItemMatcher;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/** Acciones del palo segun el modo activo. */
public final class ToolActions {

    private final DefensiveWallsPlugin plugin;

    public ToolActions(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public void perform(Player p, PlayerData d, Block clicked, BlockFace face, boolean denied) {
        if (d.mode() == ToolMode.BUILD) {
            build(p, d, clicked, face);
            return;
        }
        Structure s = plugin.structures().at(clicked);
        if (s == null) {
            plugin.messages().send(p, "no-target");
            return;
        }
        if (d.mode() != ToolMode.INFO && denied) {
            plugin.messages().send(p, "protected-area");
            return;
        }
        switch (d.mode()) {
            case REMOVE -> remove(p, s);
            case UPGRADE -> upgrade(p, s);
            case REPAIR -> repair(p, s);
            case INFO -> info(p, s);
            default -> { }
        }
    }

    private void build(Player p, PlayerData d, Block clicked, BlockFace face) {
        if (d.selection() == null) {
            plugin.messages().send(p, "no-selection");
            return;
        }
        BuildRequest req = clicked != null
                ? plugin.targeting().fromClick(p, d, clicked, face, true, true)
                : plugin.targeting().fromRay(p, d, true, true);
        if (req == null) {
            plugin.messages().send(p, "no-target");
            return;
        }
        PlacementResult result = plugin.builder().build(req);
        if (!result.ok()) {
            plugin.messages().send(p, result.error(), result.args());
            return;
        }
        plugin.messages().send(p, "built", "%structure%",
                plugin.settings().structureName(req.option().type(), req.option().tier()));
    }

    private void remove(Player p, Structure s) {
        String name = plugin.settings().structureName(s.type(), s.tier());
        plugin.structures().destroy(s, plugin.settings().dropItemOnRemove() && p.getGameMode() != GameMode.CREATIVE, true);
        plugin.messages().send(p, "removed", "%structure%", name);
    }

    private boolean free(Player p) {
        return p.getGameMode() == GameMode.CREATIVE && plugin.settings().creativeFreeBuild();
    }

    private void upgrade(Player p, Structure s) {
        Tier next = s.tier().next();
        StructureShape shape = plugin.shapes().get(s.type());
        if (next == null || shape == null || !shape.supports(next)) {
            plugin.messages().send(p, "upgrade-max");
            return;
        }
        Cost cost = plugin.settings().upgradeCost(next);
        if (!free(p) && (!plugin.costs().has(p, cost) || !plugin.costs().take(p, cost))) {
            plugin.messages().send(p, "no-materials",
                    "%amount%", String.valueOf(cost.amount()),
                    "%material%", ItemMatcher.describe(cost.token()),
                    "%have%", String.valueOf(plugin.costs().count(p, cost.token())));
            return;
        }
        if (!plugin.structures().upgrade(s)) {
            if (!free(p)) {
                plugin.costs().refund(p, cost);
            }
            plugin.messages().send(p, "upgrade-max");
            return;
        }
        plugin.messages().send(p, "upgraded",
                "%structure%", plugin.settings().structureName(s.type(), s.tier()),
                "%tier%", plugin.settings().tierName(s.tier()).toLowerCase());
    }

    private void repair(Player p, Structure s) {
        if (s.durability() >= s.maxDurability()) {
            plugin.messages().send(p, "repair-full");
            return;
        }
        Tier t = s.tier();
        int restore = plugin.settings().repairRestore(t);
        int points = Math.min(restore, s.maxDurability() - s.durability());
        int units = (int) Math.ceil(plugin.settings().repairUnits(t) * points / (double) restore);
        Cost cost = new Cost(plugin.settings().repairToken(t), Math.max(1, units));
        if (!free(p) && (!plugin.costs().has(p, cost) || !plugin.costs().take(p, cost))) {
            plugin.messages().send(p, "no-materials",
                    "%amount%", String.valueOf(cost.amount()),
                    "%material%", ItemMatcher.describe(cost.token()),
                    "%have%", String.valueOf(plugin.costs().count(p, cost.token())));
            return;
        }
        plugin.durability().repair(s, points);
        plugin.messages().send(p, "repaired",
                "%structure%", plugin.settings().structureName(s.type(), s.tier()),
                "%durability%", String.valueOf(s.durability()),
                "%max%", String.valueOf(s.maxDurability()));
        plugin.holograms().force(p, s);
    }

    private void info(Player p, Structure s) {
        plugin.holograms().force(p, s);
        if (s.type() == StructureType.CABINET) {
            plugin.guis().openCabinet(p, s);
        } else {
            plugin.guis().openInfo(p, s);
        }
    }
}
