package com.example.defensivewalls.maintenance;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.Structure;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;

/**
 * Deterioro y consumo automatico. Cada tick (config deterioration.tick-seconds) acumula consumo
 * fraccionario por estructura; cuando el acumulador llega a 1 descuenta UNA unidad del gabinete.
 * Sin materiales (o sin gabinete) la estructura pierde durabilidad hasta destruirse.
 */
public final class MaintenanceManager {

    private static final double EPS = 1e-6;
    private static final long WARN_INTERVAL_MS = 60_000L;

    private final DefensiveWallsPlugin plugin;
    private BukkitTask task;

    public MaintenanceManager(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        long period = Math.max(20L, plugin.settings().tickSeconds() * 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!plugin.settings().deteriorationEnabled()) {
            return;
        }
        double steps = plugin.settings().tickSeconds() / 60.0;
        boolean any = false;
        for (Structure s : new ArrayList<>(plugin.structures().all())) {
            if (!s.type().needsMaintenance()) {
                continue;
            }
            World world = Bukkit.getWorld(s.origin().world());
            if (world == null) {
                continue;
            }
            if (plugin.settings().onlyLoadedChunks() && !world.isChunkLoaded(s.origin().x() >> 4, s.origin().z() >> 4)) {
                continue;
            }
            process(s, steps);
            any = true;
        }
        if (any) {
            plugin.structures().markDirty();
        }
    }

    private void process(Structure s, double steps) {
        String token = plugin.settings().tierToken(s.tier());
        Structure cabinet = plugin.structures().get(s.cabinetId());
        if (s.cabinetId() != null && cabinet == null) {
            s.setCabinetId(null); // referencia a un gabinete inexistente
        }

        double acc = s.maintenanceAccumulator() + plugin.settings().maintenancePerMinute(s.tier()) * steps;
        while (acc >= 1.0 - EPS && cabinet != null && plugin.cabinets().take(cabinet, token)) {
            acc -= 1.0; // una unidad consumida exactamente una vez
        }
        acc = Math.max(0.0, acc);
        boolean unpaid = acc >= 1.0 - EPS;
        if (unpaid) {
            acc = Math.min(acc, 1.0); // la deuda pendiente se limita a una unidad
        }
        s.setMaintenanceAccumulator(acc);

        boolean decaying = cabinet == null || unpaid;
        if (s.deteriorating() != decaying) {
            s.setDeteriorating(decaying);
            plugin.structures().changed(s);
        }
        if (!decaying) {
            return;
        }

        warn(s, cabinet == null);
        double decay = s.decayAccumulator() + plugin.settings().decayPerMinute() * plugin.settings().speedMultiplier() * steps;
        int points = (int) Math.floor(decay + EPS);
        s.setDecayAccumulator(Math.max(0.0, decay - points));
        if (points > 0) {
            plugin.durability().damage(s, points, null); // puede destruir la estructura
        }
    }

    private void warn(Structure s, boolean noCabinet) {
        Location center = plugin.structures().center(s);
        if (center == null) {
            return;
        }
        center.getWorld().spawnParticle(Particle.SMOKE, center, 12, 0.6, 0.8, 0.6, 0.01);
        long now = System.currentTimeMillis();
        if (now - s.lastWarning() < WARN_INTERVAL_MS) {
            return;
        }
        s.setLastWarning(now);
        String name = plugin.settings().structureName(s.type(), s.tier());
        String key = noCabinet ? "needs-maintenance-no-cabinet" : "needs-maintenance";
        String where = s.origin().x() + ", " + s.origin().y() + ", " + s.origin().z();
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= 32 * 32) {
                plugin.messages().send(p, key, "%structure%", name, "%pos%", where);
            }
        }
    }
}
