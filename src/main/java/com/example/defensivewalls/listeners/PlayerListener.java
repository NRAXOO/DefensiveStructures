package com.example.defensivewalls.listeners;

import com.example.defensivewalls.DefensiveWallsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final DefensiveWallsPlugin plugin;

    public PlayerListener(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.previews().clear(e.getPlayer().getUniqueId());
        plugin.holograms().forget(e.getPlayer().getUniqueId());
    }
}
