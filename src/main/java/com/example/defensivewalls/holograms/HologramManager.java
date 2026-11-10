package com.example.defensivewalls.holograms;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.durability.DurabilityState;
import com.example.defensivewalls.structures.BlockKey;
import com.example.defensivewalls.structures.Structure;
import com.example.defensivewalls.structures.StructureObserver;
import com.example.defensivewalls.utils.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hologramas de durabilidad con TextDisplay. Cada entidad es temporal (no persistente),
 * invisible por defecto y solo se muestra a los jugadores cercanos o que interactuaron.
 * Se elimina en cuanto nadie la ve.
 */
public final class HologramManager implements StructureObserver {

    private static final class Holo {
        TextDisplay display;
        final Set<UUID> viewers = new HashSet<>();
        final Set<UUID> seen = new HashSet<>();
        final Map<UUID, Long> forced = new HashMap<>();
    }

    private final DefensiveWallsPlugin plugin;
    private final Map<UUID, Holo> holos = new HashMap<>();
    private BukkitTask task;

    public HologramManager(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.settings().hologramsEnabled()) {
            return;
        }
        long period = plugin.settings().hologramCheckTicks();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeAll();
    }

    public void removeAll() {
        for (Holo h : holos.values()) {
            if (h.display != null && h.display.isValid()) {
                h.display.remove();
            }
        }
        holos.clear();
    }

    public void forget(UUID player) {
        for (Holo h : holos.values()) {
            h.viewers.remove(player);
            h.forced.remove(player);
        }
    }

    /** Muestra el holograma de una estructura a un jugador durante unos segundos. */
    public void force(Player player, Structure s) {
        if (!plugin.settings().hologramsEnabled()) {
            return;
        }
        Holo h = ensure(s);
        if (h == null) {
            return;
        }
        h.forced.put(player.getUniqueId(), System.currentTimeMillis() + plugin.settings().hologramInteractSeconds() * 1000L);
        show(h, player);
    }

    private void show(Holo h, Player p) {
        if (h.viewers.add(p.getUniqueId())) {
            p.showEntity(plugin, h.display);
        }
    }

    private Holo ensure(Structure s) {
        Holo h = holos.get(s.id());
        if (h != null && h.display != null && h.display.isValid()) {
            return h;
        }
        World world = Bukkit.getWorld(s.origin().world());
        if (world == null || !world.isChunkLoaded(s.origin().x() >> 4, s.origin().z() >> 4)) {
            return null;
        }
        Location loc = location(s, world);
        Component text = buildText(s);
        Holo created = new Holo();
        created.display = world.spawn(loc, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setGravity(false);
            d.setInvulnerable(true);
            d.setBillboard(Display.Billboard.CENTER);
            d.text(text);
        });
        holos.put(s.id(), created);
        return created;
    }

    private Location location(Structure s, World world) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockKey k : s.blocks()) {
            minX = Math.min(minX, k.x());
            maxX = Math.max(maxX, k.x());
            minZ = Math.min(minZ, k.z());
            maxZ = Math.max(maxZ, k.z());
            maxY = Math.max(maxY, k.y());
        }
        return new Location(world, (minX + maxX + 1) / 2.0, maxY + 1.4, (minZ + maxZ + 1) / 2.0);
    }

    private Component buildText(Structure s) {
        DurabilityState st = plugin.durability().stateOf(s);
        String extra = "";
        if (s.type().needsMaintenance()) {
            if (s.cabinetId() == null) {
                extra = "&c⚠ Sin gabinete asignado";
            } else if (s.deteriorating()) {
                extra = "&c⚠ Necesita mantenimiento";
            } else {
                extra = "&aMantenimiento al día";
            }
        }
        List<Component> lines = new ArrayList<>();
        for (String line : plugin.settings().hologramLines()) {
            String text = Text.replace(line,
                    "{icon}", plugin.settings().hologramIcon(),
                    "{name}", plugin.settings().structureName(s.type(), s.tier()),
                    "{durability}", String.valueOf(s.durability()),
                    "{max}", String.valueOf(s.maxDurability()),
                    "{percent}", String.valueOf(plugin.durability().percent(s)),
                    "{state}", st.label(),
                    "{state_color}", st.color(),
                    "{extra}", extra);
            if (!text.isBlank()) {
                lines.add(Text.plain(text));
            }
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        double view = plugin.settings().hologramViewDistance();
        for (Holo h : holos.values()) {
            h.seen.clear();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location l = p.getLocation();
            for (Structure s : plugin.structures().nearby(l.getWorld().getUID(), l.getX(), l.getY(), l.getZ(), view + 3)) {
                Location c = plugin.structures().center(s);
                if (c == null || c.distanceSquared(l) > view * view) {
                    continue;
                }
                Holo h = ensure(s);
                if (h != null) {
                    h.seen.add(p.getUniqueId());
                    show(h, p);
                }
            }
        }
        Iterator<Holo> it = holos.values().iterator();
        while (it.hasNext()) {
            Holo h = it.next();
            for (UUID viewer : new ArrayList<>(h.viewers)) {
                Player p = Bukkit.getPlayer(viewer);
                boolean keep = p != null && (h.seen.contains(viewer) || h.forced.getOrDefault(viewer, 0L) > now);
                if (!keep) {
                    if (p != null && h.display != null && h.display.isValid()) {
                        p.hideEntity(plugin, h.display);
                    }
                    h.viewers.remove(viewer);
                    h.forced.remove(viewer);
                }
            }
            if (h.viewers.isEmpty()) {
                if (h.display != null && h.display.isValid()) {
                    h.display.remove();
                }
                it.remove();
            }
        }
    }

    @Override
    public void changed(Structure s) {
        Holo h = holos.get(s.id());
        if (h != null && h.display != null && h.display.isValid()) {
            h.display.text(buildText(s));
        }
    }

    @Override
    public void removed(Structure s) {
        Holo h = holos.remove(s.id());
        if (h != null && h.display != null && h.display.isValid()) {
            h.display.remove();
        }
    }
}
