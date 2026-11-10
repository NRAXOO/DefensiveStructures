package com.example.defensivewalls.particles;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.structures.PlannedBlock;
import com.example.defensivewalls.structures.builder.BuildRequest;
import com.example.defensivewalls.structures.builder.PlacementResult;
import com.example.defensivewalls.tools.PlayerData;
import com.example.defensivewalls.tools.ToolMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holograma de particulas de la estructura activa (verde = valido, rojo = no valido).
 * Solo se calcula para jugadores con el palo en modo construccion; la validacion se cachea
 * y el numero de particulas por actualizacion esta limitado.
 */
public final class PreviewManager {

    private static final class Cache {
        String key;
        long time;
        PlacementResult result;
    }

    private final DefensiveWallsPlugin plugin;
    private final Map<UUID, Cache> caches = new HashMap<>();
    private BukkitTask task;

    public PreviewManager(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.settings().particlesEnabled()) {
            return;
        }
        long period = plugin.settings().particleUpdateTicks();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        caches.clear();
    }

    public void clear(UUID player) {
        caches.remove(player);
    }

//    private void tick() {
//        for (Player p : Bukkit.getOnlinePlayers()) {
//            if (p.getInventory().getItemInMainHand().getType() != plugin.settings().toolMaterial()
//                    || !p.hasPermission(plugin.settings().permUse())) {
//                continue;
//            }
//            PlayerData d = plugin.players().get(p);
//            if (d.mode() != ToolMode.BUILD || d.selection() == null) {
//                continue;
//            }
//            // El resultado de validacion se calcula SIN comprobar protecciones; se completa con cache.
//            BuildRequest req = plugin.targeting().fromRay(p, d, false, plugin.settings().particleCheckProtection());
//
//            if (req == null) {
//                continue;
//            }
//
//            String key = req.origin() + "|" + req.facing() + "|" + req.option() + "|" + req.half();
//            long now = System.currentTimeMillis();
//            Cache cache = caches.computeIfAbsent(p.getUniqueId(), u -> new Cache());
//            if (cache.result == null || !key.equals(cache.key) || now - cache.time > plugin.settings().particleCacheMs()) {
//                cache.key = key;
//                cache.time = now;
//                cache.result = plugin.validator().validate(req);
//            }
//            PlacementResult result = cache.result;
//
//            List<PlannedBlock> plan = result.ok()
//                    ? result.plan()
//                    : plugin.structures().planFor(
//                    req.option().type(),
//                    req.option().tier(),
//                    req.origin(),
//                    req.facing(),
//                    req.half()
//            );
//
//            draw(p, req, result);
//            if (!result.ok() && plugin.settings().particleShowReason()) {
//                plugin.messages().actionBar(p, result.error(), result.args());
//            }
//        }
//    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {

            if (p.getInventory().getItemInMainHand().getType()
                    != plugin.settings().toolMaterial()
                    || !p.hasPermission(plugin.settings().permUse())) {
                continue;
            }

            PlayerData d = plugin.players().get(p);

            if (d.mode() != ToolMode.BUILD || d.selection() == null) {
                continue;
            }

            BuildRequest req = plugin.targeting().fromRay(
                    p, d, false, plugin.settings().particleCheckProtection()
            );

            if (req == null) {
                continue;
            }

            String key = req.origin() + "|" + req.facing()
                    + "|" + req.option() + "|" + req.half();

            long now = System.currentTimeMillis();

            Cache cache = caches.computeIfAbsent(
                    p.getUniqueId(), u -> new Cache()
            );

            if (cache.result == null
                    || !key.equals(cache.key)
                    || now - cache.time > plugin.settings().particleCacheMs()) {

                cache.key = key;
                cache.time = now;
                cache.result = plugin.validator().validate(req);
            }

            PlacementResult result = cache.result;

            // Si no se puede construir, no mostrar partículas.
            if (!result.ok()) {
                continue;
            }

            // Dibujar únicamente cuando la construcción sea válida.
            draw(p, req, result);
        }
    }



    private void draw(Player p, BuildRequest req, PlacementResult result) {
        List<PlannedBlock> plan = result.ok() ? result.plan()
                : plugin.structures().planFor(req.option().type(), req.option().tier(), req.origin(), req.facing(), req.half());
        if (plan.isEmpty()) {
            return;
        }
        World world = p.getWorld();
        Particle.DustOptions options = new Particle.DustOptions(plugin.settings().particleColor(result.ok()), 1.0f);
        int[] budget = {plugin.settings().particleMaxPoints()};
        double step = plugin.settings().particleSpacing();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (PlannedBlock pb : plan) {
            var k = pb.key();
            minX = Math.min(minX, k.x());
            maxX = Math.max(maxX, k.x());
            minY = Math.min(minY, k.y());
            maxY = Math.max(maxY, k.y());
            minZ = Math.min(minZ, k.z());
            maxZ = Math.max(maxZ, k.z());
        }
        double x1 = minX, y1 = minY, z1 = minZ, x2 = maxX + 1, y2 = maxY + 1, z2 = maxZ + 1;

        // Un punto en el centro de cada bloque real de la estructura (muestra la forma exacta y la orientacion).
//        for (PlannedBlock pb : plan) {
//            if (budget[0] <= 0) {
//                break;
//            }
//            point(p, world, pb.key().x() + 0.5, pb.key().y() + 0.5, pb.key().z() + 0.5, options, budget);
//        }
        // Caja exterior (tamano y posicion).
        for (double y : new double[]{y1, y2}) {
            for (double z : new double[]{z1, z2}) {
                line(p, world, x1, y, z, x2, y, z, options, step, budget);
            }
            for (double x : new double[]{x1, x2}) {
                line(p, world, x, y, z1, x, y, z2, options, step, budget);
            }
        }
        for (double x : new double[]{x1, x2}) {
            for (double z : new double[]{z1, z2}) {
                line(p, world, x, y1, z, x, y2, z, options, step, budget);
            }
        }
    }

    private void line(Player p, World w, double x1, double y1, double z1, double x2, double y2, double z2,
                      Particle.DustOptions o, double step, int[] budget) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int n = Math.max(1, (int) Math.ceil(len / step));
        for (int i = 0; i <= n && budget[0] > 0; i++) {
            point(p, w, x1 + dx * i / n, y1 + dy * i / n, z1 + dz * i / n, o, budget);
        }
    }

//    private void point(Player p, World w, double x, double y, double z, Particle.DustOptions o, int[] budget) {
//        budget[0]--;
//        p.spawnParticle(Particle.DUST_COLOR_TRANSITION, new Location(w, x, y, z), 1, 0, 0, 0, 0, o);
//    }

    private void point(Player p, World w, double x, double y, double z,
                       Particle.DustOptions o, int[] budget) {

        budget[0]--;

        p.spawnParticle(
                Particle.EGG_CRACK,
                new Location(w, x, y, z),
                1, 0, 0, 0, 0
        );
    }
}
