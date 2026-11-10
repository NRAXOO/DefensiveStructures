package com.example.defensivewalls.structures.storage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Escritura de ficheros segura (archivo temporal + movimiento atomico) y ordenada.
 * Los datos llegan ya serializados como String: el hilo asincrono nunca toca la API de Bukkit.
 */
public final class AtomicFileWriter {

    private final Plugin plugin;
    private final Path target;
    private final Object lock = new Object();
    private final AtomicLong counter = new AtomicLong();
    private long lastWritten = 0; // protegido por lock

    public AtomicFileWriter(Plugin plugin, Path target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void writeAsync(String data) {
        long sequence = counter.incrementAndGet();
        if (!plugin.isEnabled()) {
            write(data, sequence);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(data, sequence));
    }

    public void writeSync(String data) {
        write(data, counter.incrementAndGet());
    }

    private void write(String data, long sequence) {
        synchronized (lock) {
            if (sequence < lastWritten) {
                return; // ya se escribio una version mas reciente
            }
            lastWritten = sequence;
            try {
                Files.createDirectories(target.getParent());
                Path temp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.writeString(temp, data, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + target.getFileName(), ex);
            }
        }
    }
}
