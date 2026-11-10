package com.example.defensivewalls.configuration;

import com.example.defensivewalls.DefensiveWallsPlugin;
import com.example.defensivewalls.utils.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Mensajes configurables en espanol (codigos '&' y marcadores %nombre%). */
public final class Messages {

    private final DefensiveWallsPlugin plugin;

    public Messages(DefensiveWallsPlugin plugin) {
        this.plugin = plugin;
    }

    public String raw(String key) {
        String text = plugin.getConfig().getString("messages." + key);
        return text != null ? text : "&c[Mensaje no definido: " + key + "]";
    }

    public Component get(String key, String... pairs) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return Text.plain(prefix + Text.replace(raw(key), pairs));
    }

    public void send(CommandSender to, String key, String... pairs) {
        to.sendMessage(get(key, pairs));
    }

    public void actionBar(Player to, String key, String... pairs) {
        to.sendActionBar(Text.plain(Text.replace(raw(key), pairs)));
    }
}
