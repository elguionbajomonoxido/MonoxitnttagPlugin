package com.monoxitnttag.placeholders;

import com.monoxitnttag.MonoxitnttagPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class TntTagPlaceholder extends PlaceholderExpansion {

    private final MonoxitnttagPlugin plugin;

    public TntTagPlaceholder(MonoxitnttagPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "monoxitnttag";
    }

    @Override
    public String getAuthor() {
        return "Monoxido"; // Replace with your name or alias
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return false;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier) {
            case "tnt":
                return plugin.hasTnt(player) ? "true" : "false";
            case "tnt_name":
                // Devuelve los nombres de los jugadores que tienen la TNT
                java.util.List<String> conTnt = new java.util.ArrayList<>();
                for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.hasTnt(p)) {
                        conTnt.add(p.getName());
                    }
                }
                return String.join(", ", conTnt);
            case "glow":
                // Devuelve el código de color rojo para el glow si tiene TNT, blanco si no
                return plugin.hasTnt(player) ? "&c" : "&f";
            default:
                return null;
        }
    }
}
